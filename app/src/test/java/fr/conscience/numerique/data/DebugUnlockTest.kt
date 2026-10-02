package fr.conscience.numerique.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Les sept appuis sur le numéro de version qui activent le mode debug. */
class DebugUnlockTest {
    private val unlock = DebugUnlock()

    @Test
    fun `les premiers appuis ne disent rien`() {
        repeat(3) { assertEquals(DebugTap.Nothing, unlock.tap(alreadyEnabled = false)) }
    }

    @Test
    fun `le compte a rebours apparait pour les trois derniers appuis`() {
        repeat(3) { unlock.tap(alreadyEnabled = false) }

        assertEquals(DebugTap.Remaining(3), unlock.tap(alreadyEnabled = false))
        assertEquals(DebugTap.Remaining(2), unlock.tap(alreadyEnabled = false))
        assertEquals(DebugTap.Remaining(1), unlock.tap(alreadyEnabled = false))
    }

    @Test
    fun `le septieme appui active le mode debug`() {
        repeat(DebugUnlock.TAPS_REQUIRED - 1) { unlock.tap(alreadyEnabled = false) }

        assertEquals(DebugTap.Unlocked, unlock.tap(alreadyEnabled = false))
        assertEquals(7, DebugUnlock.TAPS_REQUIRED)
    }

    @Test
    fun `une fois active, un appui le rappelle sans rien compter`() {
        repeat(5) { assertEquals(DebugTap.AlreadyEnabled, unlock.tap(alreadyEnabled = true)) }
    }

    @Test
    fun `six appuis ne suffisent pas`() {
        val results = List(6) { unlock.tap(alreadyEnabled = false) }

        assertEquals(false, results.any { it == DebugTap.Unlocked })
    }
}
