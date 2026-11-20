package app.redflag.ui

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.redflag.AppContainer
import app.redflag.data.AppDatabase
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The list of apps is read again when an app is installed, removed or updated, and only then: coming back to a screen no longer
 * reads it. The system's events are replaced by [events] here, so that no app has to be installed.
 */
@RunWith(AndroidJUnit4::class)
class AppsChangedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    private val container = AppContainer(context, db, packageEvents = events)

    @After
    fun tearDown() = db.close()

    /** Counts the readings of the list while [block] runs. */
    private fun readings(block: suspend (AtomicInteger) -> Unit): Int = runBlocking {
        val count = AtomicInteger()
        val job = launch(Dispatchers.Default) { container.installedAppsCountFlow().collect { count.incrementAndGet() } }
        withTimeout(5_000) { while (count.get() == 0) delay(20) }
        block(count)
        job.cancelAndJoin()
        count.get()
    }

    @Test
    fun theListIsReadOnceAtFirstThenOnlyWhenAnAppChanges() {
        val total = readings { count ->
            delay(1_000)
            assertEquals("nothing changed: no new reading", 1, count.get())

            // An install sends several events in a row (added, then changed): one reading after the last one.
            events.emit("app.new")
            events.emit("app.new")
            events.emit("app.new")
            withTimeout(5_000) { while (count.get() < 2) delay(20) }
            delay(1_000)
        }
        assertEquals("one reading at first, one after the install", 2, total)
    }

    @Test
    fun anAppRemovedLaterIsSeenToo() {
        val total = readings { count ->
            events.emit("app.a")
            withTimeout(5_000) { while (count.get() < 2) delay(20) }
            delay(1_000) // further apart than the debounce: a second, separate change
            events.emit("app.b")
            withTimeout(5_000) { while (count.get() < 3) delay(20) }
        }
        assertEquals(3, total)
    }
}
