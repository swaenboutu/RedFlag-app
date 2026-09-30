package fr.conscience.numerique

import android.app.Application
import android.content.Context

class ConscienceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Le conteneur de dépendances de l'app, depuis n'importe quel contexte (activité, ViewModel, service). */
val Context.container: AppContainer get() = (applicationContext as ConscienceApp).container
