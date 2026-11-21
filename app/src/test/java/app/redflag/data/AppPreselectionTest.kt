package app.redflag.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreselectionTest {
    @Test
    fun everyIssueKeyExistsInTheCatalog() {
        val unknown = AppPreselection.byPackage.flatMap { (pkg, keys) -> keys.filter { ProblemCatalog.find(it) == null }.map { "$pkg: $it" } }

        assertEquals(emptyList<String>(), unknown)
    }

    @Test
    fun everyAppIsLinkedToAtLeastOneIssueWithoutRepeats() {
        AppPreselection.byPackage.forEach { (pkg, keys) ->
            assertTrue("$pkg has no issue", keys.isNotEmpty())
            assertEquals("$pkg repeats an issue", keys.size, keys.toSet().size)
        }
    }

    @Test
    fun suggestionsFollowTheIssue() {
        val addictive = AppPreselection.suggestedFor(ProblemRef.catalog("addictive_design"))

        assertTrue("com.zhiliaoapp.musically" in addictive)
        assertTrue("com.ubercab" !in addictive)
    }

    @Test
    fun aCustomIssueHasNoSuggestion() {
        assertEquals(emptySet<String>(), AppPreselection.suggestedFor(ProblemRef.newCustom()))
    }
}
