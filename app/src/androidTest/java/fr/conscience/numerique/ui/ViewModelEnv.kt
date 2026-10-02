package fr.conscience.numerique.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import fr.conscience.numerique.AppContainer
import fr.conscience.numerique.data.AppDatabase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

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
        store.clear()
        db.close()
    }
}

/** Attend (au plus 5 s) que l'état vérifie [predicate] : les ViewModels calculent leur état en arrière-plan. */
suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T = withTimeout(5_000) { first(predicate) }
