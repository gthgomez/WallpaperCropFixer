package com.wallpapercropfixer.presentation.navigation

object Destinations {
    const val ENTRY = "entry"
    // uri passed as a query param so that encoded slashes (%2F) in file paths
    // are not misinterpreted as route path separators by the Nav component.
    const val EDITOR = "editor?uri={uri}"
    const val PREVIEW = "preview"
    const val SETTINGS = "settings"

    fun editorRoute(encodedUri: String) = "editor?uri=$encodedUri"
}
