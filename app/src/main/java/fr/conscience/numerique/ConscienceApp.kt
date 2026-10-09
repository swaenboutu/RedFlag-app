package fr.conscience.numerique

import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConscienceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.settings.appearance.value.apply()
        // Les icônes des apps signalées sont prêtes avant qu'un écran ou une interruption en ait besoin.
        container.applicationScope.launch {
            container.icons.preload(container.repository.monitoredApps.first().map { it.app.packageName })
        }
    }
}

/** Le conteneur de dépendances de l'app, depuis n'importe quel contexte (activité, ViewModel, service). */
val Context.container: AppContainer get() = (applicationContext as ConscienceApp).container
