package fr.conscience.numerique.ui

import fr.conscience.numerique.R
import fr.conscience.numerique.data.ProblemRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Les écrans « Vos problématiques », fiche d'une problématique et fiche d'une app, sur une base en mémoire. */
class ProblemViewModelsTest {
    private lateinit var env: ViewModelEnv
    private val fomo = ProblemRef(catalogKey = "fomo")

    @Before
    fun setUp() {
        env = ViewModelEnv()
    }

    @After
    fun tearDown() = env.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(20)
    }

    /** Appelle une fonction à rappel et attend son résultat. */
    private fun awaitResult(call: ((Boolean) -> Unit) -> Unit): Boolean {
        var result: Boolean? = null
        call { result = it }
        awaitCondition { result != null }
        return checkNotNull(result) { "aucun résultat" }
    }

    private fun problemDetail(args: Map<String, Any?>) =
        env.viewModel(args) { ProblemDetailViewModel(env.container, env.context, it) }

    @Test
    fun managerListsFavoritesFirstAndCustomProblemsInTheirTheme() = run {
        val vm = env.viewModel { ProblemsManagerViewModel(env.container, env.context) }
        env.repository.setFavorite(fomo, true)
        assertTrue(awaitResult { vm.add("Mon souci", "privacy", it) })
        vm.expandTheme("privacy")

        val rows = vm.rows.await { r -> r.any { it is ManagerRow.Custom } }

        val firstTheme = rows.first() as ManagerRow.Theme
        assertEquals("« Vos favoris » en premier", R.string.category_favorites, firstTheme.id)
        assertTrue(rows.filterIsInstance<ManagerRow.Custom>().any { it.label == "Mon souci" })
    }

    @Test
    fun managerRefusesANameAlreadyUsedByTheCatalogOrACustomProblem() = run {
        val vm = env.viewModel { ProblemsManagerViewModel(env.container, env.context) }
        assertTrue(awaitResult { vm.add("Mon souci", null, it) })

        assertFalse("doublon personnalisé", awaitResult { vm.add("mon SOUCI", null, it) })
        assertFalse("intitulé du catalogue", awaitResult { vm.add(env.context.getString(R.string.problem_fomo), null, it) })
    }

    @Test
    fun problemDetailRenamesACustomProblemAndKeepsFollowingIt() = run {
        env.repository.addCustomProblem("ancien")
        env.repository.addCustomProblem("pris")
        val vm = problemDetail(mapOf(DetailArgs.CUSTOM_LABEL to "ancien"))
        vm.state.await { it != null }

        assertFalse("nom déjà pris", awaitResult { vm.rename("PRIS", onResult = it) })
        assertTrue(awaitResult { vm.rename("nouveau", onResult = it) })

        val state = vm.state.await { it?.ref?.customLabel == "nouveau" }
        assertEquals(ProblemRef(customLabel = "nouveau"), state?.ref)
        assertEquals(setOf("nouveau", "pris"), env.repository.customLabels.first().toSet())
    }

    @Test
    fun problemDetailLinksUnlinksAndTogglesTheFavorite() = run {
        val vm = problemDetail(mapOf(DetailArgs.CATALOG_KEY to "fomo"))
        vm.state.await { it != null }

        vm.link(listOf(LinkedApp("app.a", "Alpha"), LinkedApp("app.b", "Bravo")))
        assertEquals(listOf("Alpha", "Bravo"), vm.state.await { it?.linkedApps?.size == 2 }?.linkedApps?.map { it.appName })

        vm.unlink("app.a")
        assertEquals(listOf("Bravo"), vm.state.await { it?.linkedApps?.size == 1 }?.linkedApps?.map { it.appName })

        vm.toggleFavorite()
        assertTrue(vm.state.await { it?.favorite == true }!!.favorite)
    }

    @Test
    fun problemDetailRenamingACatalogProblemCanBeUndone() = run {
        val original = env.context.getString(R.string.problem_fomo)
        val vm = problemDetail(mapOf(DetailArgs.CATALOG_KEY to "fomo"))
        vm.state.await { it != null }

        assertTrue(awaitResult { vm.rename("Peur de rater", original, it) })
        assertEquals("Peur de rater", vm.state.await { it?.override != null }?.override)

        // Retaper l'intitulé d'origine rétablit l'original au lieu d'enregistrer un texte figé.
        assertTrue(awaitResult { vm.rename(original, original, it) })
        assertNull(vm.state.await { it?.override == null }?.override)
    }

    @Test
    fun deletingACustomProblemFromItsDetailRemovesItEverywhere() = run {
        env.repository.addCustomProblem("a supprimer")
        env.repository.linkProblem("app.a", "Alpha", ProblemRef(customLabel = "a supprimer"))
        val vm = problemDetail(mapOf(DetailArgs.CUSTOM_LABEL to "a supprimer"))
        var done = false

        vm.deleteCustom { done = true }
        awaitCondition { done }

        assertTrue(done)
        assertTrue(env.repository.customLabels.first().isEmpty())
        assertNull(env.repository.find("app.a"))
    }

    @Test
    fun appDetailShowsLinkedProblemsFirstAndSavesEachToggle() = run {
        env.repository.linkProblem("app.a", "Alpha", fomo)
        val vm = env.viewModel(mapOf(AppDetailArgs.PACKAGE to "app.a", AppDetailArgs.LABEL to "Alpha")) {
            AppDetailViewModel(env.container, it)
        }

        val rows = vm.rows.await { it.any { r -> r is DetailRow.Choice } }
        assertEquals(1, (rows.first() as DetailRow.Header).count)
        assertEquals("« Associées » en premier", R.string.category_linked, rows.filterIsInstance<DetailRow.Theme>().first().id)

        val sexism = ProblemRef(catalogKey = "sexism")
        vm.toggle(sexism, checked = false)
        vm.rows.await { (it.first() as DetailRow.Header).count == 2 }

        vm.toggle(fomo, checked = true)
        vm.toggle(sexism, checked = true)
        vm.rows.await { (it.first() as DetailRow.Header).count == 0 }
        assertNull("sans problématique, l'app sort de la surveillance", env.repository.find("app.a"))
    }
}
