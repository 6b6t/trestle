package net.blockhost.trestle.ui

enum class LauncherCommand {
    NEW_INSTANCE,
    IMPORT_LOCAL_FILE,
    FOCUS_SEARCH,
    LAUNCH_SELECTED,
    REMOVE_SELECTED,
    TOGGLE_SELECTED_PIN,
    SHOW_LIBRARY,
    SHOW_DISCOVER,
    SHOW_ACCOUNTS,
    SHOW_SETTINGS,
    SHOW_SHORTCUTS,
}

data class LauncherCommandRequest(
    val sequence: Long,
    val command: LauncherCommand,
)
