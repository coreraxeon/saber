package com.example.stylusdraw

/**
 * Provides a callback for opening notes from the side drawer in the current
 * TabbedEditor if available.
 */
object DrawerInterop {
    /**
     * When not null, SideDrawer will call this instead of navigating to a new
     * editor instance. TabbedEditor sets this when active.
     */
    var openInTab: ((String) -> Unit)? = null
}
