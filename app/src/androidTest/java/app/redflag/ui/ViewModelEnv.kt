package app.redflag.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.AppContainer
import app.redflag.container
import app.redflag.data.AppDatabase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue

/**
 * De quoi tester un ViewModel seul, sans écran : un conteneur sur une base en mémoire (la vraie base de l'app n'est pas touchée),
 * et un moyen de créer le ViewModel puis de l'arrêter proprement (`close`).
 */
class ViewModelEnv {
    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    val container = AppContainer(context, db)
    val repository get() = container.repository

    private val store = ViewModelStore()
    private var count = 0

    /** [args] : ce que l'intent de l'écran transmettrait au ViewModel (SavedStateHandle). */
    inline fun <reified VM : ViewModel> viewModel(args: Map<String, Any?> = emptyMap(), crossinline create: (SavedStateHandle) -> VM): VM =
        provide(VM::class.java) { create(SavedStateHandle(args)) }

    fun <VM : ViewModel> provide(type: Class<VM>, create: () -> VM): VM {
        val factory = viewModelFactory { addInitializer(type.kotlin) { create() } }
        return ViewModelProvider.create(store, factory)["vm-${count++}", type]
    }

    fun close() {
        // Les ViewModels travaillent sur le fil principal : on les arrête là, puis on laisse finir ce qui était déjà en file avant
        // de fermer la base. Sans cela, un traitement en cours touchait la base fermée et faisait tomber le test suivant.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { store.clear() }
        instrumentation.waitForIdleSync()
        db.close()
    }
}

/** Attend (au plus 5 s) que l'état vérifie [predicate] : les ViewModels calculent leur état en arrière-plan. */
suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T = withTimeout(5_000) { first(predicate) }

/**
 * Ces tests ont besoin d'apps non système installées, que l'accueil liste (un émulateur de base, surtout ancien, n'en a pas
 * toujours : seules des apps système, masquées par défaut). Sans elles, ils sont ignorés plutôt qu'en échec.
 */
fun assumeUserApps(context: Context, count: Int) {
    val found = context.container.installedApps.list(hideSystemApps = true).size
    assumeTrue("il faut au moins $count app(s) non système installée(s) (trouvées : $found)", found >= count)
}
