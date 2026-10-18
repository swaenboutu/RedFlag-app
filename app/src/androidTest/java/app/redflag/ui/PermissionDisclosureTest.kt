package app.redflag.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.R
import app.redflag.container
import app.redflag.service.FrictionPermission
import app.redflag.service.hasOverlayPermission
import app.redflag.service.hasUsageAccess
import app.redflag.service.settingsAction
import app.redflag.ui.common.PermissionDisclosureDialog
import app.redflag.ui.settings.SettingsActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Before each of Android's permission screens opens ("Usage access", then "Display over other apps"), a window explains what it is
 * for and asks for an explicit consent.
 */
@RunWith(AndroidJUnit4::class)
class PermissionDisclosureTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var usageMonitor: Instrumentation.ActivityMonitor
    private lateinit var overlayMonitor: Instrumentation.ActivityMonitor
    private var doneBefore = false
    private var usageBefore = false
    private var overlayBefore = false

    /** Android's settings opened on our app: the action with a `package:` URI. */
    private fun filterFor(permission: FrictionPermission) =
        IntentFilter(settingsAction(permission)).apply { addDataScheme("package") }

    private fun appOps(op: String, mode: String) {
        instrumentation.uiAutomation.executeShellCommand("appops set ${context.packageName} $op $mode").close()
        Thread.sleep(300)
    }

    @Before
    fun setUp() {
        doneBefore = context.container.settings.onboardingDone
        context.container.settings.onboardingDone = true
        usageBefore = hasUsageAccess(context)
        overlayBefore = hasOverlayPermission(context)
        appOps("GET_USAGE_STATS", "ignore")
        appOps("SYSTEM_ALERT_WINDOW", "ignore")
        // Watches for Android's settings being opened (it must never come before the consent) and blocks it: the attempts are
        // counted without really leaving the app.
        usageMonitor = instrumentation.addMonitor(
            filterFor(FrictionPermission.USAGE_ACCESS), Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()), true,
        )
        overlayMonitor = instrumentation.addMonitor(
            filterFor(FrictionPermission.OVERLAY), Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()), true,
        )
    }

    @After
    fun tearDown() {
        instrumentation.removeMonitor(usageMonitor)
        instrumentation.removeMonitor(overlayMonitor)
        appOps("GET_USAGE_STATS", if (usageBefore) "allow" else "default")
        appOps("SYSTEM_ALERT_WINDOW", if (overlayBefore) "allow" else "default")
        context.container.settings.onboardingDone = doneBefore
    }

    private fun <A : AppCompatActivity> ActivityScenario<A>.disclosure(): PermissionDisclosureDialog? {
        var dialog: PermissionDisclosureDialog? = null
        val deadline = System.currentTimeMillis() + 5_000
        while (dialog == null && System.currentTimeMillis() < deadline) {
            onActivity {
                it.supportFragmentManager.executePendingTransactions()
                dialog = it.supportFragmentManager.fragments.filterIsInstance<PermissionDisclosureDialog>().firstOrNull()
            }
            if (dialog == null) Thread.sleep(50)
        }
        return dialog
    }

    private fun <A : AppCompatActivity> ActivityScenario<A>.texts(dialog: PermissionDisclosureDialog): Pair<String, String> {
        var title = ""
        var message = ""
        onActivity {
            val alert = dialog.dialog as AlertDialog
            title = alert.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.text?.toString().orEmpty()
            message = alert.findViewById<TextView>(android.R.id.message)!!.text.toString()
        }
        return title to message
    }

    private fun <A : AppCompatActivity> ActivityScenario<A>.press(button: Int) {
        onActivity { activity ->
            val dialog = activity.supportFragmentManager.fragments.filterIsInstance<PermissionDisclosureDialog>().first().dialog
            (dialog as AlertDialog).getButton(button).performClick()
        }
    }

    private fun ActivityScenario<SettingsActivity>.tapPermissionsRow() =
        onActivity { it.findViewById<View>(R.id.rowPermissions).performClick() }

    private fun awaitHit(monitor: Instrumentation.ActivityMonitor) {
        val deadline = System.currentTimeMillis() + 5_000
        while (monitor.hits == 0 && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    /**
     * One scenario on the Settings screen: the permissions row shows the usage access warning first (Android's settings stay
     * closed), it says what is read and that nothing is shared, "Cancel" opens nothing, agreeing opens the usage access settings
     * once; once usage access is granted, the next warning is about displaying over other apps, and agreeing opens its settings.
     */
    @Test
    fun eachPermissionIsExplainedFirstAndOnlyAgreeingOpensAndroidSettings() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.tapPermissionsRow()
            val usage = scenario.disclosure()
            assertNotNull("the warning shows", usage)
            assertEquals("Android's settings are not open yet", 0, usageMonitor.hits)
            val (title, text) = scenario.texts(usage!!)
            assertEquals(context.getString(R.string.usage_disclosure_title), title)
            assertTrue("says what is read: the app in the foreground", text.contains("foreground", true) || text.contains("premier plan", true))
            assertTrue("says that nothing leaves the device", text.contains("leaves your device") || text.contains("ne quitte"))

            scenario.press(DialogInterface.BUTTON_NEGATIVE)
            instrumentation.waitForIdleSync() // the settings would be opened right from the click
            assertEquals("Cancel opens nothing", 0, usageMonitor.hits)

            scenario.awaitNoDisclosure()
            scenario.tapPermissionsRow()
            assertNotNull("the warning comes back", scenario.disclosure())
            scenario.press(DialogInterface.BUTTON_POSITIVE)
            awaitHit(usageMonitor)
            assertEquals("Agree and continue opens the usage access settings", 1, usageMonitor.hits)
            assertEquals(0, overlayMonitor.hits)

            appOps("GET_USAGE_STATS", "allow")
            scenario.awaitNoDisclosure()
            scenario.tapPermissionsRow()
            val overlay = scenario.disclosure()
            assertNotNull("the second warning shows", overlay)
            assertEquals(context.getString(R.string.overlay_disclosure_title), scenario.texts(overlay!!).first)
            assertEquals("not opened before the consent", 0, overlayMonitor.hits)
            scenario.press(DialogInterface.BUTTON_POSITIVE)
            awaitHit(overlayMonitor)
            assertEquals("Agree and continue opens the overlay settings", 1, overlayMonitor.hits)
            assertEquals(1, usageMonitor.hits)
        }
    }

    /** Waits (at most 5 s) until the warning is gone, so that the next tap shows a new one. */
    private fun <A : AppCompatActivity> ActivityScenario<A>.awaitNoDisclosure() {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            var shown = true
            onActivity {
                it.supportFragmentManager.executePendingTransactions()
                shown = it.supportFragmentManager.fragments.any { f -> f is PermissionDisclosureDialog }
            }
            if (!shown) return
            Thread.sleep(50)
        }
    }
}
