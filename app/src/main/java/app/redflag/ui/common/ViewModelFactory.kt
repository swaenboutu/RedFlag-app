package app.redflag.ui.common

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.redflag.AppContainer
import app.redflag.container

/**
 * Crée le ViewModel d'un écran en lui passant ses dépendances (le conteneur, le contexte de l'app, les arguments de l'intent)
 * plutôt que de le laisser aller les chercher : un test peut ainsi construire le même ViewModel avec une base de données de test.
 */
inline fun <reified VM : ViewModel> ComponentActivity.screenViewModel(
    crossinline create: (container: AppContainer, context: Context, handle: SavedStateHandle) -> VM,
) = viewModels<VM> {
    viewModelFactory {
        initializer { create(container, applicationContext, createSavedStateHandle()) }
    }
}
