package own.moderpach.extinguish.service.hosts

import android.content.Context
import android.os.SystemClock
import android.util.Log
import extinguish.ipc.result.EventResult
import extinguish.root_service.IEventsListener
import extinguish.root_service.IEventsProvider

private const val TAG = "VolumeKeyEventShizukuHost"

/**
 * Volume-key listener backed by privileged `getevent` events.
 *
 * KEY_POWER (0074) is tracked so that a volume key pressed together with the
 * power key (the standard screenshot gesture) is ignored instead of toggling
 * the screen. A short guard window also covers the case where the power key is
 * released a few milliseconds before the volume key.
 */
class VolumeKeyEventShizukuHost(
    private val owner: Context,
    val service: IEventsProvider,
    var onKeyEvent: () -> Unit = {},
) {

    companion object {
        private const val EV_KEY = "0001"
        private const val KEY_VOLUME_DOWN = "0072"
        private const val KEY_VOLUME_UP = "0073"
        private const val KEY_POWER = "0074"
        private const val VALUE_RELEASE = "00000000"

        // If the power key was pressed within this window, treat the volume key
        // as part of a combination (screenshot) and ignore it.
        private const val POWER_GUARD_MS = 350L
    }

    var isRegister = false
    var isAwake = false

    private var powerKeyDown = false
    private var lastPowerDownAt = 0L

    private val listener = object : IEventsListener.Stub() {
        override fun onEvent(event: EventResult) {
            if (event.v0 != EV_KEY) return
            if (event.v1 == KEY_POWER) {
                if (event.v2 == VALUE_RELEASE) {
                    powerKeyDown = false
                } else {
                    // Any non-release value means the power key is held down.
                    powerKeyDown = true
                    lastPowerDownAt = SystemClock.elapsedRealtime()
                }
                return
            }
            val isVolumeKey = event.v1 == KEY_VOLUME_DOWN || event.v1 == KEY_VOLUME_UP
            if (!isVolumeKey || event.v2 != VALUE_RELEASE) return

            val powerCombo = powerKeyDown ||
                (SystemClock.elapsedRealtime() - lastPowerDownAt) < POWER_GUARD_MS
            if (powerCombo) {
                Log.d(TAG, "ignore volume key: power key combo (screenshot)")
                return
            }
            if (isAwake) {
                Log.d(TAG, "get volume key event - $event")
                onKeyEvent()
            }
        }
    }

    fun register() {
        if (!isRegister) {
            isRegister = true
            powerKeyDown = false
            lastPowerDownAt = 0L
            service.registerListener(listener)
            isAwake = true
        }
    }

    fun unregister() {
        if (isRegister) {
            isRegister = false
            service.unregisterListener(listener)
            isAwake = false
            powerKeyDown = false
        }
    }

    fun sleep() {
        isAwake = false
    }

    fun wake() {
        isAwake = true
    }

}
