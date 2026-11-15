package io.github.ulysseszh.pwainstaller

import android.app.Application
import com.google.android.material.color.DynamicColors
import java.util.concurrent.Executors

class InstallerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        if (manager.runningAppProcesses.orEmpty().any { it.pid == android.os.Process.myPid() && it.processName == packageName })
            { AdblockSettings.schedule(this); signing.execute { runCatching { SigningKeys.load(this) } } }
    }
    companion object {
        val worker = Executors.newSingleThreadExecutor()
        val readers = Executors.newFixedThreadPool(2)
        val installEvents = Executors.newSingleThreadExecutor()
        val installPipeline = Executors.newSingleThreadExecutor()
        val editor = Executors.newSingleThreadExecutor()
        val signing = Executors.newSingleThreadExecutor()
    }
}
