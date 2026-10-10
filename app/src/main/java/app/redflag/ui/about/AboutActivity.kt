package app.redflag.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.redflag.R
import app.redflag.databinding.ActivityAboutBinding

/**
 * "About": the license of the app and the link to its source code. (The privacy policy will be linked here once it is hosted on a
 * website: see TODO.md.)
 */
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener { finish() }
        binding.licenseLink.setOnClickListener { open(LICENSE_URL) }
        binding.sourceLink.setOnClickListener { open(SOURCE_URL) }
    }

    private fun open(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.about_no_browser, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        /** The public repository of the app. */
        const val SOURCE_URL = "https://github.com/swaenboutu/RedFlag-app"

        /** The text of the license (GNU GPL version 3). */
        const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
    }
}
