package fr.conscience.numerique.service

/**
 * Mémorise les réponses de l'utilisateur à l'écran d'interruption, pour ne pas le redemander à tort.
 *
 * - « Oui » : l'app reste autorisée tant que l'utilisateur s'en sert. L'autorisation est oubliée dès qu'une autre app
 *   (ou l'écran d'accueil) passe devant, quand l'écran s'éteint, ou après [ALLOW_MILLIS] sans activité de l'app.
 * - « Non » : l'app est renvoyée à l'accueil ; ses événements parasites des [DECLINE_GUARD_MILLIS] suivantes sont
 *   ignorés, sinon l'écran d'interruption se rouvrirait aussitôt, en boucle.
 *
 * [now] est injectable pour tester le temps.
 */
class FrictionGate(private val now: () -> Long = System::currentTimeMillis) {
    private var allowed: String? = null
    private var allowedUntil = 0L
    private var declined: String? = null
    private var declinedUntil = 0L

    @Synchronized
    fun allow(packageName: String) {
        allowed = packageName
        allowedUntil = now() + ALLOW_MILLIS
    }

    /** Vrai si [packageName] a reçu un « Oui » encore valable. Chaque passage prolonge l'autorisation (l'app est utilisée). */
    @Synchronized
    fun isAllowed(packageName: String): Boolean {
        if (allowed != packageName) return false
        val time = now()
        if (time >= allowedUntil) {
            allowed = null
            return false
        }
        allowedUntil = time + ALLOW_MILLIS
        return true
    }

    /** Une vraie app (ou l'accueil) passe au premier plan : l'autorisation d'une autre app est oubliée. */
    @Synchronized
    fun onOtherAppForeground(packageName: String) {
        if (allowed != null && allowed != packageName) allowed = null
    }

    /** L'écran s'éteint : le prochain retour dans l'app doit redemander. */
    @Synchronized
    fun revokeAll() {
        allowed = null
    }

    @Synchronized
    fun declined(packageName: String) {
        declined = packageName
        declinedUntil = now() + DECLINE_GUARD_MILLIS
        if (allowed == packageName) allowed = null
    }

    @Synchronized
    fun isDeclineGuarded(packageName: String): Boolean = declined == packageName && now() < declinedUntil

    companion object {
        /** Durée d'un « Oui » sans aucune activité de l'app (un événement de fenêtre la prolonge). */
        const val ALLOW_MILLIS = 15 * 60_000L

        /** Après un « Non », les événements de la même app sont ignorés pendant ce délai. */
        const val DECLINE_GUARD_MILLIS = 3_000L
    }
}
