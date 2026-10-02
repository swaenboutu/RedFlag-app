package fr.conscience.numerique.data

/** Ce que provoque un appui sur le numéro de version. */
sealed interface DebugTap {
    /** Trop tôt pour dire quoi que ce soit. */
    data object Nothing : DebugTap

    /** Plus que [taps] appuis avant d'activer le mode debug. */
    data class Remaining(val taps: Int) : DebugTap

    data object Unlocked : DebugTap

    data object AlreadyEnabled : DebugTap
}

/**
 * Le mode debug s'active en appuyant [TAPS_REQUIRED] fois sur le numéro de version, comme le mode développeur d'Android :
 * rien ne s'affiche d'abord, puis le compte à rebours des derniers appuis.
 */
class DebugUnlock {
    private var taps = 0

    fun tap(alreadyEnabled: Boolean): DebugTap {
        if (alreadyEnabled) return DebugTap.AlreadyEnabled
        taps++
        val remaining = TAPS_REQUIRED - taps
        return when {
            remaining <= 0 -> {
                taps = 0
                DebugTap.Unlocked
            }
            remaining <= SHOW_COUNTDOWN_FROM -> DebugTap.Remaining(remaining)
            else -> DebugTap.Nothing
        }
    }

    companion object {
        const val TAPS_REQUIRED = 7
        private const val SHOW_COUNTDOWN_FROM = 3
    }
}
