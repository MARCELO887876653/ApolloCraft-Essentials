package com.uxplima.uxmessentials.shared.application.message;

/**
 * The cross-cutting management-GUI message keys — the {@code gui.*} block the central hub and any
 * shared GUI scaffolding owns, distinct from a feature context's own menu keys.
 *
 * <p>The {@code /uxmess gui} hub lists every module's registered management-GUI entry as a clickable
 * icon. Its title, the per-entry name and lore, the navigation labels, and the empty-state line all
 * resolve here rather than being inlined, so the hub honours the locale pipeline and the UI-style canon
 * exactly as every feature menu does. The hub is shared infrastructure owned by no single context, so
 * its keys sit in the shared kernel under the {@code gui} prefix.
 *
 * <p>Like every {@link MessageKey} enum the constant name and the catalog key map 1:1
 * ({@code HUB_TITLE} ↔ {@code gui.hub.title}); the locale-parity guard asserts each has an {@code en}
 * entry and the catalog aggregate enumerates this enum.
 */
public enum GuiMessageKey implements MessageKey {

    // the /uxmess gui management hub
    HUB_TITLE("gui.hub.title"),
    HUB_ENTRY_LORE("gui.hub.entry.lore"),
    HUB_EMPTY("gui.hub.empty"),
    HUB_PREV("gui.hub.prev"),
    HUB_NEXT("gui.hub.next"),
    MODULE_CONTROL_ENTRY("gui.module-control.entry"),
    MODULE_CONTROL_TITLE("gui.module-control.title"),
    MODULE_CONTROL_PREVIOUS("gui.module-control.previous"),
    MODULE_CONTROL_NEXT("gui.module-control.next"),
    MODULE_CONTROL_REFRESH("gui.module-control.refresh"),
    MODULE_CONTROL_REFRESH_LORE("gui.module-control.refresh-lore"),
    MODULE_CONTROL_INFO("gui.module-control.info"),
    MODULE_CONTROL_INFO_TOTAL("gui.module-control.info-total"),
    MODULE_CONTROL_INFO_ENABLED("gui.module-control.info-enabled"),
    MODULE_CONTROL_INFO_PENDING("gui.module-control.info-pending"),
    MODULE_CONTROL_INFO_NOTE("gui.module-control.info-note"),
    MODULE_CONTROL_INFO_RESTART_NOTE("gui.module-control.info-restart-note"),
    MODULE_CONTROL_CLOSE("gui.module-control.close"),
    MODULE_CONTROL_CLOSE_LORE("gui.module-control.close-lore"),
    MODULE_CONTROL_MODULE_NAME("gui.module-control.module-name"),
    MODULE_CONTROL_MODULE_ID("gui.module-control.module-id"),
    MODULE_CONTROL_BOOT_ENABLED("gui.module-control.boot-enabled"),
    MODULE_CONTROL_BOOT_DISABLED("gui.module-control.boot-disabled"),
    MODULE_CONTROL_RESTART_ENABLED("gui.module-control.restart-enabled"),
    MODULE_CONTROL_RESTART_DISABLED("gui.module-control.restart-disabled"),
    MODULE_CONTROL_PENDING("gui.module-control.pending"),
    MODULE_CONTROL_NO_PENDING("gui.module-control.no-pending"),
    MODULE_CONTROL_CLICK_ENABLE("gui.module-control.click-enable"),
    MODULE_CONTROL_CLICK_DISABLE("gui.module-control.click-disable"),
    MODULE_CONTROL_BUSY("gui.module-control.busy"),
    MODULE_CONTROL_SAVED_ENABLED("gui.module-control.saved-enabled"),
    MODULE_CONTROL_SAVED_DISABLED("gui.module-control.saved-disabled"),
    MODULE_CONTROL_SAVE_FAILED("gui.module-control.save-failed"),

    // the shared text-input seam — the cancel acknowledgement and the chat-mode cancel hint
    INPUT_CANCELLED("gui.input.cancelled"),
    INPUT_CANCEL_HINT("gui.input.cancel-hint"),

    // the shared confirm window — the two button labels a Bedrock ModalForm needs (the chest paints wordless wool)
    CONFIRM_YES("gui.confirm.yes"),
    CONFIRM_NO("gui.confirm.no"),

    // the shared paged form — the Previous/Next nav buttons a Bedrock SimpleForm adds when a list spans pages
    PAGE_PREVIOUS("gui.page.previous"),
    PAGE_NEXT("gui.page.next"),

    // the shared colour-picker widget — its chrome buttons
    COLOUR_PICKER_TITLE("gui.colour-picker.title"),
    COLOUR_PICKER_CUSTOM("gui.colour-picker.custom"),
    COLOUR_PICKER_CUSTOM_PROMPT("gui.colour-picker.custom-prompt"),
    COLOUR_PICKER_CLEAR("gui.colour-picker.clear"),
    COLOUR_PICKER_BACK("gui.colour-picker.back"),

    // the shared player-picker widget — online head entries plus the offline-name anvil button and nav
    PLAYER_PICKER_HEAD_NAME("gui.player-picker.head-name"),
    PLAYER_PICKER_HEAD_LORE("gui.player-picker.head-lore"),
    PLAYER_PICKER_CUSTOM("gui.player-picker.custom"),
    PLAYER_PICKER_CUSTOM_LORE("gui.player-picker.custom-lore"),
    PLAYER_PICKER_CUSTOM_PROMPT("gui.player-picker.custom-prompt"),
    PLAYER_PICKER_PREV("gui.player-picker.prev"),
    PLAYER_PICKER_NEXT("gui.player-picker.next"),

    // the shared duration-picker widget — preset duration buttons plus the custom-span anvil button and back
    DURATION_PICKER_PRESET_NAME("gui.duration-picker.preset-name"),
    DURATION_PICKER_PRESET_LORE("gui.duration-picker.preset-lore"),
    DURATION_PICKER_CUSTOM("gui.duration-picker.custom"),
    DURATION_PICKER_CUSTOM_LORE("gui.duration-picker.custom-lore"),
    DURATION_PICKER_CUSTOM_PROMPT("gui.duration-picker.custom-prompt"),
    DURATION_PICKER_BACK("gui.duration-picker.back"),

    // the shared colour-picker widget — the 16 standard named-colour swatches
    COLOUR_WHITE("gui.colour.white"),
    COLOUR_ORANGE("gui.colour.orange"),
    COLOUR_MAGENTA("gui.colour.magenta"),
    COLOUR_LIGHT_BLUE("gui.colour.light-blue"),
    COLOUR_YELLOW("gui.colour.yellow"),
    COLOUR_LIME("gui.colour.lime"),
    COLOUR_PINK("gui.colour.pink"),
    COLOUR_GRAY("gui.colour.gray"),
    COLOUR_LIGHT_GRAY("gui.colour.light-gray"),
    COLOUR_CYAN("gui.colour.cyan"),
    COLOUR_PURPLE("gui.colour.purple"),
    COLOUR_BLUE("gui.colour.blue"),
    COLOUR_BROWN("gui.colour.brown"),
    COLOUR_GREEN("gui.colour.green"),
    COLOUR_RED("gui.colour.red"),
    COLOUR_BLACK("gui.colour.black");

    private final String key;

    GuiMessageKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
