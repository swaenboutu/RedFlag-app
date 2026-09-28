package fr.conscience.numerique.service

/**
 * Mémorise l'app à laquelle l'utilisateur a répondu « Oui », pour ne pas redemander tant qu'elle
 * reste au premier plan. Dès qu'une autre app passe devant, l'autorisation est oubliée.
 */
class FrictionGate {
    private var allowed: String? = null

    @Synchronized
    fun allow(packageName: String) {
        allowed = packageName
    }

    @Synchronized
    fun isAllowed(packageName: String): Boolean = allowed == packageName

    @Synchronized
    fun onOtherAppForeground(packageName: String) {
        if (allowed != null && allowed != packageName) allowed = null
    }
}
