package app.redflag.ui

import app.redflag.data.ProblemRef
import app.redflag.ui.common.isLabelTaken
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemLabelsTest {
    private val fomo = ProblemRef.catalog("fomo")
    private val sexism = ProblemRef.catalog("sexism")
    private val mine = ProblemRef("custom:1")

    private val labels = mapOf(fomo to "FOMO", sexism to "Sexisme", mine to "Mon souci")

    @Test
    fun `un nom deja utilise par une autre problematique est refuse`() {
        assertTrue(labels.isLabelTaken("Sexisme", except = fomo))
    }

    @Test
    fun `la casse ne change rien`() {
        assertTrue(labels.isLabelTaken("sexISME", except = fomo))
    }

    @Test
    fun `un nom libre est accepte`() {
        assertFalse(labels.isLabelTaken("Autre chose", except = fomo))
    }

    @Test
    fun `une problematique peut garder son propre nom`() {
        assertFalse(labels.isLabelTaken("FOMO", except = fomo))
    }

    @Test
    fun `elle peut aussi n'en changer que la casse`() {
        assertFalse(labels.isLabelTaken("fomo", except = fomo))
    }

    @Test
    fun `un nom personnalise ne peut pas reprendre un intitule du catalogue`() {
        assertTrue(labels.isLabelTaken("FOMO", except = mine))
    }

    @Test
    fun `a la creation, rien n'est exclu`() {
        assertTrue(labels.isLabelTaken("Mon souci"))
        assertTrue(labels.isLabelTaken("fomo"))
        assertFalse(labels.isLabelTaken("Nouveau"))
    }
}
