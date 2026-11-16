package io.github.ulysseszh.pwainstaller

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ActivityResultTest {
	private val instrumentation = InstrumentationRegistry.getInstrumentation()
	private val context = instrumentation.targetContext

	private fun await(
		message: String,
		condition: () -> Boolean,
	) {
		val deadline = System.currentTimeMillis() + 10000
		while (System.currentTimeMillis() < deadline) {
			if (condition()) {
				return
			}
			Thread.sleep(50)
		}
		fail(message)
	}

	@Test
	fun externalActivityResultsReachTheRecreatedCallerOnce() {
		val original =
			instrumentation.startActivitySync(
				Intent(context, ActivityResultTestHost::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
			) as ActivityResultTestHost
		var target: ActivityResultTestTarget? = null
		var replacement: ActivityResultTestHost? = null
		try {
			instrumentation.runOnMainSync { original.openTarget() }
			await("The result target must open") {
				instrumentation.runOnMainSync {
					target =
						ActivityLifecycleMonitorRegistry
							.getInstance()
							.getActivitiesInStage(Stage.RESUMED)
							.filterIsInstance<ActivityResultTestTarget>()
							.singleOrNull()
				}
				target != null
			}
			instrumentation.runOnMainSync { original.recreate() }
			await("The caller must be recreated while its result is pending") {
				instrumentation.runOnMainSync {
					replacement =
						listOf(Stage.CREATED, Stage.STARTED, Stage.STOPPED, Stage.PAUSED, Stage.RESUMED)
							.flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
							.filterIsInstance<ActivityResultTestHost>()
							.firstOrNull { it !== original }
				}
				replacement != null
			}
			instrumentation.runOnMainSync { requireNotNull(target).complete() }
			await("The recreated caller must receive the pending result") {
				var delivered = false
				instrumentation.runOnMainSync {
					delivered = replacement?.received == "returned after recreation"
				}
				delivered
			}
			instrumentation.runOnMainSync {
				assertEquals(0, original.deliveries)
				assertEquals(1, requireNotNull(replacement).deliveries)
			}
		} finally {
			instrumentation.runOnMainSync {
				target?.finish()
				replacement?.finish()
				original.finish()
			}
		}
	}
}
