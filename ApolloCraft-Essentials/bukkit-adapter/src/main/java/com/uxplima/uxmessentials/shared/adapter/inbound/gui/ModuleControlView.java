package com.uxplima.uxmessentials.shared.adapter.inbound.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmessentials.shared.adapter.inbound.gui.menu.EntityListSpec;
import com.uxplima.uxmessentials.shared.adapter.inbound.gui.menu.Menus;
import com.uxplima.uxmessentials.shared.adapter.outbound.BukkitRefs;
import com.uxplima.uxmessentials.shared.adapter.outbound.style.Tiles;
import com.uxplima.uxmessentials.shared.application.message.GuiMessageKey;
import com.uxplima.uxmessentials.shared.application.module.FeatureModule;
import com.uxplima.uxmessentials.shared.application.module.ModuleId;
import com.uxplima.uxmessentials.shared.application.module.ModuleRegistry;
import com.uxplima.uxmessentials.shared.application.port.ConfigStore;
import com.uxplima.uxmessentials.shared.application.port.Logger;
import com.uxplima.uxmessentials.shared.application.port.MessageSink;
import com.uxplima.uxmessentials.shared.application.port.Messages;
import com.uxplima.uxmessentials.shared.application.port.Scheduler;
import com.uxplima.uxmessentials.shared.domain.PlayerRef;
import com.uxplima.uxmlib.item.ItemBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

/**
 * ApolloCraft's always-available module switchboard.
 *
 * <p>No module is removed from the jar. A click only changes the module's {@code enabled} flag on disk. Because
 * feature adapters, listeners and command nodes are wired at plugin enable, the screen deliberately marks a changed
 * module as restart-bound instead of pretending it was hot-swapped. The next server start consumes the chosen state.
 *
 * <p>The window renders through the shared {@link Menus} engine rather than creating a Bukkit inventory directly,
 * and every player-facing label resolves through the message catalog. That keeps the panel inside the same inventory
 * and locale boundaries as the rest of the management UI.
 */
@NullMarked
public final class ModuleControlView {

    private static final int ROWS = 6;
    private static final int REFRESH_SLOT = 45;
    private static final int PREVIOUS_SLOT = 46;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 52;
    private static final int CLOSE_SLOT = 53;
    private static final List<Integer> CONTENT_SLOTS = java.util.stream.IntStream.range(0, 45).boxed().toList();

    private final ModuleRegistry registry;
    private final ConfigStore config;
    private final Scheduler scheduler;
    private final Path dataFolder;
    private final Logger log;
    private final Menus menus;
    private final GuiText guiText;
    private final Messages messages;
    private final MessageSink sink;
    private final Set<ModuleId> enabledAtBoot;
    private final Set<ModuleId> pendingModules = ConcurrentHashMap.newKeySet();

    public ModuleControlView(
            ModuleRegistry registry,
            ConfigStore config,
            Scheduler scheduler,
            Path dataFolder,
            Logger log,
            Menus menus,
            GuiText guiText,
            Messages messages,
            MessageSink sink) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.config = Objects.requireNonNull(config, "config");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.log = Objects.requireNonNull(log, "log");
        this.menus = Objects.requireNonNull(menus, "menus");
        this.guiText = Objects.requireNonNull(guiText, "guiText");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.sink = Objects.requireNonNull(sink, "sink");
        Set<ModuleId> running = new HashSet<>();
        for (FeatureModule module : registry.enabledModules(config)) {
            running.add(module.id());
        }
        this.enabledAtBoot = Set.copyOf(running);
    }

    /** Opens the module switchboard for an administrator. */
    public void open(Player player, PlayerRef viewer) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(viewer, "viewer");
        menus.openList(viewer, spec(viewer));
    }

    /** Opens the module switchboard for an administrator. */
    public void open(Player player) {
        Objects.requireNonNull(player, "player");
        open(player, BukkitRefs.toRef(player));
    }

    /** Builds the engine-owned paginated list, including the original refresh/info/close footer controls. */
    private EntityListSpec spec(PlayerRef viewer) {
        return EntityListSpec.builder()
                .title(guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_TITLE))
                .rows(ROWS)
                .contentSlots(CONTENT_SLOTS)
                .navigation(PREVIOUS_SLOT, NEXT_SLOT, Material.ARROW)
                .navNames(
                        guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_PREVIOUS),
                        guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_NEXT))
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .entities(() -> List.<Object>copyOf(registry.all()))
                .iconRenderer((who, entity) -> moduleItem(who, (FeatureModule) entity))
                .onSelect((live, entity) -> toggle(live, viewer, (FeatureModule) entity))
                .extraButtons(List.of(
                        new EntityListSpec.ExtraButton(REFRESH_SLOT, refreshItem(viewer), live -> open(live, viewer)),
                        new EntityListSpec.ExtraButton(INFO_SLOT, infoItem(viewer), ignored -> {}),
                        new EntityListSpec.ExtraButton(CLOSE_SLOT, closeItem(viewer), Player::closeInventory)))
                .build();
    }

    private void toggle(Player player, PlayerRef viewer, FeatureModule module) {
        Objects.requireNonNull(player, "player");
        boolean desired = module.enabled(config);
        boolean next = !desired;
        if (!pendingModules.add(module.id())) {
            send(viewer, GuiMessageKey.MODULE_CONTROL_BUSY, Map.of());
            return;
        }
        scheduler.async(() -> toggleOffTick(viewer, module, next));
    }

    private void toggleOffTick(PlayerRef viewer, FeatureModule module, boolean next) {
        try {
            writeEnabled(module.id(), next);
            config.reload();
            scheduler.onEntity(viewer, () -> {
                @Nullable Player player = Bukkit.getPlayer(viewer.uuid());
                if (player == null) {
                    return;
                }
                send(
                        viewer,
                        next
                                ? GuiMessageKey.MODULE_CONTROL_SAVED_ENABLED
                                : GuiMessageKey.MODULE_CONTROL_SAVED_DISABLED,
                        Map.of("module", module.id().value()));
                open(player, viewer);
            });
        } catch (RuntimeException failure) {
            log.error("failed to persist ApolloCraft module toggle for " + module.id().value(), failure);
            send(viewer, GuiMessageKey.MODULE_CONTROL_SAVE_FAILED, Map.of());
        } finally {
            pendingModules.remove(module.id());
        }
    }

    private void send(PlayerRef viewer, GuiMessageKey key, Map<String, String> placeholders) {
        sink.deliver(viewer, messages.resolve(viewer, key, placeholders));
    }

    private void writeEnabled(ModuleId id, boolean enabled) {
        Path file = dataFolder.resolve("modules").resolve(id.value()).resolve("config.conf");
        try {
            Files.createDirectories(Objects.requireNonNull(file.getParent(), "module config parent"));
            HoconConfigurationLoader loader = HoconConfigurationLoader.builder().path(file).build();
            CommentedConfigurationNode root = Files.exists(file) ? loader.load() : CommentedConfigurationNode.root();
            root.node("enabled").set(enabled);
            loader.save(root);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("could not write module config " + file, failure);
        }
    }

    private ItemStack moduleItem(PlayerRef viewer, FeatureModule module) {
        boolean running = enabledAtBoot.contains(module.id());
        boolean desired = module.enabled(config);
        boolean pending = running != desired;
        Material material = desired ? Material.LIME_DYE : Material.GRAY_DYE;

        List<Component> lore = new ArrayList<>();
        lore.add(guiText.text(
                viewer, GuiMessageKey.MODULE_CONTROL_MODULE_ID, Map.of("id", module.id().value())));
        lore.add(Component.empty());
        lore.add(guiText.text(
                viewer,
                running ? GuiMessageKey.MODULE_CONTROL_BOOT_ENABLED : GuiMessageKey.MODULE_CONTROL_BOOT_DISABLED));
        lore.add(guiText.text(
                viewer,
                desired
                        ? GuiMessageKey.MODULE_CONTROL_RESTART_ENABLED
                        : GuiMessageKey.MODULE_CONTROL_RESTART_DISABLED));
        lore.add(Component.empty());
        lore.add(guiText.text(
                viewer,
                pending ? GuiMessageKey.MODULE_CONTROL_PENDING : GuiMessageKey.MODULE_CONTROL_NO_PENDING));
        lore.add(guiText.text(
                viewer,
                desired ? GuiMessageKey.MODULE_CONTROL_CLICK_DISABLE : GuiMessageKey.MODULE_CONTROL_CLICK_ENABLE));

        Component title = guiText.text(
                viewer, GuiMessageKey.MODULE_CONTROL_MODULE_NAME, Map.of("module", module.id().value()));
        return tile(material, title, lore);
    }

    private ItemStack refreshItem(PlayerRef viewer) {
        return tile(
                Material.CLOCK,
                guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_REFRESH),
                List.of(guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_REFRESH_LORE)));
    }

    private ItemStack closeItem(PlayerRef viewer) {
        return tile(
                Material.BARRIER,
                guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_CLOSE),
                List.of(guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_CLOSE_LORE)));
    }

    private ItemStack infoItem(PlayerRef viewer) {
        long desired = registry.all().stream().filter(module -> module.enabled(config)).count();
        long pending = registry.all().stream()
                .filter(module -> enabledAtBoot.contains(module.id()) != module.enabled(config))
                .count();
        Map<String, String> counts = Map.of(
                "total", Integer.toString(registry.all().size()),
                "enabled", Long.toString(desired),
                "pending", Long.toString(pending));
        return tile(
                Material.NETHER_STAR,
                guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_INFO),
                List.of(
                        guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_INFO_TOTAL, counts),
                        guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_INFO_ENABLED, counts),
                        guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_INFO_PENDING, counts),
                        Component.empty(),
                        guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_INFO_NOTE),
                        guiText.text(viewer, GuiMessageKey.MODULE_CONTROL_INFO_RESTART_NOTE)));
    }

    private static ItemStack tile(Material material, Component title, List<Component> lore) {
        return ItemBuilder.of(material)
                .name(Tiles.blankName())
                .lore(Tiles.titled(title, lore))
                .build();
    }
}
