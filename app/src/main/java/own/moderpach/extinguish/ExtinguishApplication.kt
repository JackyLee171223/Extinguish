package own.moderpach.extinguish

import android.app.Application
import com.topjohnwu.superuser.Shell

class ExtinguishApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        configureRootShell()
        createNotificationChannels()
    }

    private fun configureRootShell() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }
}

object BuildExt {
    const val INIT_VERSION = 3
}
