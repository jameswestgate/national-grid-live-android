package com.crainiate.nationalgridlive.data.settings

/** Appearance preference, mirroring the iOS app's System / Light / Dark picker. */
enum class AppTheme(val displayName: String) {
    System("System"),
    Light("Light"),
    Dark("Dark");

    /** Resolves to the effective dark flag given the current system setting. */
    fun isDark(systemInDark: Boolean): Boolean = when (this) {
        System -> systemInDark
        Light -> false
        Dark -> true
    }
}

/** Which graphic the Generation card shows, mirroring iOS's Bar / Donut / None. */
enum class GenerationVisualisation(val displayName: String) {
    Bar("Bar"),
    Donut("Donut"),
    Hidden("None")
}
