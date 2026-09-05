package com.uxplima.uxmessentials.shared.adapter.inbound.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import com.uxplima.uxmessentials.shared.adapter.outbound.BukkitRefs;
import com.uxplima.uxmessentials.shared.application.module.FeatureModule;
import com.uxplima.uxmessentials.shared.application.module.ModuleId;
import com.uxplima.uxmessentials.shared.application.module.ModuleRegistry;
import com.uxplima.uxmessentials.shared.application.port.ConfigStore;
import com.uxplima.uxmessentials.shared.application.port.Logger;
import com.uxplima.uxmessentials.shared.application.port.Scheduler;
import com.uxplima.uxmessentials.shared.domain.PlayerRef;
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
 */
@NullMarked
public final class ModuleControlView implements Listener {

    private static final int SIZE = 54;
    private static final int REFRESH_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    private static final Map<String, String> DISPLAY_NAMES = Map.ofEntries(
            Map.entry("teleport", "Teleportes"),
            Map.entry("worlds", "Mundos"),
            Map.entry("homes", "Homes"),
            Map.entry("economy", "Economia"),
            Map.entry("warps", "Warps"),
            Map.entry("kits", "Kits"),
            Map.entry("playerstate", "Estado do jogador"),
            Map.entry("vanish", "Vanish"),
            Map.entry("messaging", "Mensagens privadas"),
            Map.entry("presence", "AFK e presença"),
            Map.entry("moderation", "Moderação"),
            Map.entry("itemworld", "Itens e mundo"),
            Map.entry("vaults", "Cofres"),
            Map.entry("communication", "Comunicação"),
            Map.entry("holograms", "Hologramas"),
            Map.entry("playerwarps", "Warps de jogadores"),
            Map.entry("scoreboard", "Scoreboard"),
            Map.entry("tablist", "Tablist"),
            Map.entry("vote", "Votação"),
            Map.entry("nametags", "Nametags"),
            Map.entry("staff", "Staff"),
            Map.entry("npc", "NPCs"),
            Map.entry("custommenus", "Menus personalizados"),
            Map.entry("customcommands", "Comandos personalizados"),
            Map.entry("poses", "Poses"),
            Map.entry("survival", "Mecânicas survival"),
            Map.entry("ranks", "Ranks"),
            Map.entry("security", "Segurança"),
            Map.entry("commandcontrol", "Controle de comandos"),
            Map.entry("trade", "Trocas"),
            Map.entry("villagers", "Villagers"),
            Map.entry("invrollback", "Rollback de inventário"),
            Map.entry("regions", "Regiões"),
            Map.entry("servertweaks", "Ajustes do servidor"),
            Map.entry("skin", "Skins"),
            Map.entry("discordlink", "Discord Link"));

    private final ModuleRegistry registry;
    private final ConfigStore config;
    private final Scheduler scheduler;
    private final Path dataFolder;
    private final Logger log;
    private final Set<ModuleId> enabledAtBoot;
    private final Set<ModuleId> pendingModules = ConcurrentHashMap.newKeySet();

    public ModuleControlView(
            ModuleRegistry registry, ConfigStore config, Scheduler scheduler, Path dataFolder, Logger log) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.config = Objects.requireNonNull(config, "config");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        this.log = Objects.requireNonNull(log, "log");
        Set<ModuleId> running = new HashSet<>();
        for (FeatureModule module : registry.enabledModules(config)) {
            running.add(module.id());
        }
        this.enabledAtBoot = Set.copyOf(running);
    }

    /** Opens the module switchboard for an administrator. */
    public void open(Player player, PlayerRef ignoredViewer) {
        open(player);
    }

    /** Opens the module switchboard for an administrator. */
    public void open(Player player) {
        Objects.requireNonNull(player, "player");
        Holder holder = new Holder();
        Inventory inventory = holder.getInventory();

        List<FeatureModule> modules = registry.all();
        for (int index = 0; index < modules.size() && index < 45; index++) {
            FeatureModule module = modules.get(index);
            holder.bind(index, module.id());
            inventory.setItem(index, moduleItem(module));
        }

        inventory.setItem(REFRESH_SLOT, button(
                Material.CLOCK,
                "Atualizar painel",
                NamedTextColor.AQUA,
                List.of("Recarrega os estados mostrados.")));
        inventory.setItem(INFO_SLOT, infoItem());
        inventory.setItem(CLOSE_SLOT, button(
                Material.BARRIER,
                "Fechar",
                NamedTextColor.RED,
                List.of("Fecha o gerenciador de módulos.")));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == REFRESH_SLOT) {
            open(player);
            return;
        }
        ModuleId moduleId = holder.module(slot);
        if (moduleId == null) {
            return;
        }
        var found = registry.byId(moduleId);
        if (found.isEmpty()) {
            return;
        }
        FeatureModule module = found.get();

        boolean desired = module.enabled(config);
        boolean next = !desired;
        PlayerRef viewer = BukkitRefs.toRef(player);
        if (!pendingModules.add(module.id())) {
            player.sendMessage(Component.text("ApolloCraft » Aguarde a alteração deste módulo ser salva.", NamedTextColor.YELLOW));
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
                player.sendMessage(Component.text("ApolloCraft » ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(displayName(module.id()) + " ficará ", NamedTextColor.GRAY))
                        .append(Component.text(next ? "ATIVADO" : "DESATIVADO", next ? NamedTextColor.GREEN : NamedTextColor.RED))
                        .append(Component.text(" após reiniciar o servidor.", NamedTextColor.YELLOW)));
                open(player);
            });
        } catch (RuntimeException failure) {
            log.error("failed to persist ApolloCraft module toggle for " + module.id().value(), failure);
            scheduler.onEntity(viewer, () -> {
                @Nullable Player player = Bukkit.getPlayer(viewer.uuid());
                if (player != null) {
                    player.sendMessage(Component.text("ApolloCraft » Não foi possível salvar essa alteração.", NamedTextColor.RED));
                }
            });
        } finally {
            pendingModules.remove(module.id());
        }
    }

    private void writeEnabled(ModuleId id, boolean enabled) {
        Path file = dataFolder.resolve("modules").resolve(id.value()).resolve("config.conf");
        try {
            Files.createDirectories(Objects.requireNonNull(file.getParent(), "module config parent"));
            HoconConfigurationLoader loader =
                    HoconConfigurationLoader.builder().path(file).build();
            CommentedConfigurationNode root = Files.exists(file) ? loader.load() : CommentedConfigurationNode.root();
            root.node("enabled").set(enabled);
            loader.save(root);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("could not write module config " + file, failure);
        }
    }

    private ItemStack moduleItem(FeatureModule module) {
        boolean running = enabledAtBoot.contains(module.id());
        boolean desired = module.enabled(config);
        boolean pending = running != desired;
        Material material = desired ? Material.LIME_DYE : Material.GRAY_DYE;
        NamedTextColor nameColor = desired ? NamedTextColor.GREEN : NamedTextColor.RED;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ID: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(module.id().value(), NamedTextColor.GRAY)));
        lore.add(Component.empty());
        lore.add(statusLine("Configurado no boot", running));
        lore.add(statusLine("Após reinício", desired));
        lore.add(Component.empty());
        if (pending) {
            lore.add(Component.text("⚠ Reinício necessário", NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("✓ Sem alteração pendente", NamedTextColor.DARK_GREEN));
        }
        lore.add(Component.text("Clique para ", NamedTextColor.GRAY)
                .append(Component.text(desired ? "desativar" : "ativar", NamedTextColor.AQUA)));

        return item(material, displayName(module.id()), nameColor, lore);
    }

    private static Component statusLine(String label, boolean enabled) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(enabled ? "ATIVO" : "INATIVO", enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private ItemStack infoItem() {
        long desired = registry.all().stream().filter(module -> module.enabled(config)).count();
        long pending = registry.all().stream()
                .filter(module -> enabledAtBoot.contains(module.id()) != module.enabled(config))
                .count();
        return item(
                Material.NETHER_STAR,
                "ApolloCraft Essentials",
                NamedTextColor.LIGHT_PURPLE,
                List.of(
                        Component.text("Módulos no código: " + registry.all().size(), NamedTextColor.GRAY),
                        Component.text("Ativos após reinício: " + desired, NamedTextColor.GREEN),
                        Component.text("Alterações pendentes: " + pending, pending == 0 ? NamedTextColor.DARK_GREEN : NamedTextColor.YELLOW),
                        Component.empty(),
                        Component.text("Nenhum módulo é removido do plugin.", NamedTextColor.AQUA),
                        Component.text("O clique apenas altera o estado para o próximo boot.", NamedTextColor.DARK_GRAY)));
    }

    private static ItemStack button(Material material, String name, NamedTextColor color, List<String> loreLines) {
        List<Component> lore = loreLines.stream().map(line -> Component.text(line, NamedTextColor.GRAY)).toList();
        return item(material, name, color, lore);
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String displayName(ModuleId id) {
        return DISPLAY_NAMES.getOrDefault(id.value(), id.value());
    }

    private static final class Holder implements InventoryHolder {
        private final Map<Integer, ModuleId> modulesBySlot = new HashMap<>();
        private final Inventory inventory;

        Holder() {
            this.inventory = Bukkit.createInventory(
                    this,
                    SIZE,
                    Component.text("ApolloCraft", NamedTextColor.LIGHT_PURPLE)
                            .append(Component.text(" • Módulos", NamedTextColor.DARK_GRAY)));
        }

        void bind(int slot, ModuleId id) {
            modulesBySlot.put(slot, id);
        }

        @Nullable
        ModuleId module(int slot) {
            return modulesBySlot.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
