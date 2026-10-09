package app.redflag.service

/**
 * Mémorise les réponses de l'utilisateur à l'écran d'interruption, pour ne pas le redemander à tort.
 *
 * - « Oui » : l'app reste autorisée [ALLOW_MILLIS] (15 minutes) après son dernier passage au premier plan. Quitter l'app, même
 *   par erreur, puis y revenir dans ce délai ne redemande rien ; chaque passage de l'app prolonge l'autorisation tant qu'elle est
 *   utilisée. Plusieurs apps peuvent être autorisées en même temps. L'autorisation est oubliée quand l'écran s'éteint.
 * - « Non » : l'app est renvoyée à l'accueil ; ses événements parasites des [DECLINE_GUARD_MILLIS] suivantes sont
 *   ignorés, sinon l'écran d'interruption se rouvrirait aussitôt, en boucle. Un « Non » retire un « Oui » précédent.
 *
 * [now] est injectable pour tester le temps.
 */
class FrictionGate(private val now: () -> Long = System::currentTimeMillis) {
    /** Pour chaque app autorisée, l'instant où son « Oui » cesse d'être valable. */
    private val allowedUntil = HashMap<String, Long>()
    private var declined: String? = null
    private var declinedUntil = 0L

    @Synchronized
    fun allow(packageName: String) {
        allowedUntil[packageName] = now() + ALLOW_MILLIS
    }

    /** Vrai si [packageName] a reçu un « Oui » encore valable. Chaque passage prolonge l'autorisation (l'app est utilisée). */
    @Synchronized
    fun isAllowed(packageName: String): Boolean {
        val until = allowedUntil[packageName] ?: return false
        val time = now()
        if (time >= until) {
            allowedUntil.remove(packageName)
            return false
        }
        allowedUntil[packageName] = time + ALLOW_MILLIS
        return true
    }

    /** L'écran s'éteint : le prochain retour dans une app doit redemander. */
    @Synchronized
    fun revokeAll() {
        allowedUntil.clear()
    }

    @Synchronized
    fun declined(packageName: String) {
        declined = packageName
        declinedUntil = now() + DECLINE_GUARD_MILLIS
        allowedUntil.remove(packageName)
    }

    @Synchronized
    fun isDeclineGuarded(packageName: String): Boolean = declined == packageName && now() < declinedUntil

    companion object {
        /** Durée d'un « Oui » après le dernier passage de l'app (un événement de fenêtre de l'app la prolonge). */
        const val ALLOW_MILLIS = 15 * 60_000L

        /** Après un « Non », les événements de la même app sont ignorés pendant ce délai. */
        const val DECLINE_GUARD_MILLIS = 3_000L
    }
}
