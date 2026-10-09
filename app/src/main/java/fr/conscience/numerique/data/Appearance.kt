package fr.conscience.numerique.data

import androidx.appcompat.app.AppCompatDelegate

/** How the app looks: forced light, forced dark, or following the phone's setting (the default). */
enum class Appearance(val key: String, private val nightMode: Int) {
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES);

    /** Applies this appearance to every screen of the app (open ones are recreated). */
    fun apply() = AppCompatDelegate.setDefaultNightMode(nightMode)

    companion object {
        fun fromKey(key: String?): Appearance = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
