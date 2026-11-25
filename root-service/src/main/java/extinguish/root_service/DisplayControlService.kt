package extinguish.root_service

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import extinguish.hiddenAPI.server.display.DisplayControlProxy
import extinguish.hiddenAPI.view.SurfaceControlProxy
import extinguish.ipc.result.UnitResult

/**
 * Runs inside a privileged root process spawned by libsu (app_process, uid 0).
 *
 * The hidden-API calls below are identical to the previous Shizuku implementation;
 * only the hosting mechanism changed (Shizuku UserService -> libsu RootService).
 */
class DisplayControlService : RootService() {

    companion object {

        const val TAG = "DisplayControlService"

        const val POWER_MODE_OFF = SurfaceControlProxy.POWER_MODE_OFF
        const val POWER_MODE_NORMAL = SurfaceControlProxy.POWER_MODE_NORMAL

        const val BRIGHTNESS_MODE_MANUAL = 0
        const val BRIGHTNESS_MODE_AUTO = 1
    }

    private val binder = DisplayControlBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        binder.release()
        super.onDestroy()
    }

    private class DisplayControlBinder : IDisplayControl.Stub() {

        private val shLock = Object()
        private var _shProcess: Process? = null
        private val shProcess: Process
            get() = synchronized(shLock) {
                if (_shProcess?.isAlive == true) _shProcess
                Runtime.getRuntime().exec(arrayOf("sh")).also {
                    _shProcess = it
                }
            }

        private fun getPrimaryPhysicalDisplayToken(): IBinder {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val primaryPhysicalDisplayId = DisplayControlProxy.getPhysicalDisplayIds()[0]
                return DisplayControlProxy.getPhysicalDisplayToken(primaryPhysicalDisplayId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return SurfaceControlProxy.getInternalDisplayToken()
            }
            return SurfaceControlProxy.getBuiltInDisplay()
        }

        override fun setPowerModeToSurfaceControl(mode: Int): UnitResult {
            try {
                val displayToken = getPrimaryPhysicalDisplayToken()
                SurfaceControlProxy.setDisplayPowerMode(
                    displayToken, mode
                )
            } catch (e: Exception) {
                return UnitResult.Err(e)
            }
            return UnitResult.Ok()
        }

        override fun setBrightnessToSurfaceControl(brightness: Float): UnitResult {
            try {
                val displayToken = getPrimaryPhysicalDisplayToken()
                SurfaceControlProxy.setDisplayBrightness(
                    displayToken, brightness
                )
            } catch (e: Exception) {
                return UnitResult.Err(e)
            }
            return UnitResult.Ok()
        }

        override fun setBrightnessToSetting(brightness: Int): UnitResult {
            val output = shProcess.outputStream
            synchronized(shLock) {
                output.write("settings put system screen_brightness $brightness\n".toByteArray())
                output.flush()
            }
            return UnitResult.Ok()
        }

        override fun setBrightnessModeToSetting(mode: Int): UnitResult {
            if (mode != BRIGHTNESS_MODE_MANUAL && mode != BRIGHTNESS_MODE_AUTO) {
                return UnitResult.Err(
                    IllegalArgumentException("mode should be 0 or 1")
                )
            }
            val output = shProcess.outputStream
            synchronized(shLock) {
                output.write("settings put system screen_brightness_mode $mode\n".toByteArray())
                output.flush()
            }
            return UnitResult.Ok()
        }

        override fun destroy() {
            Log.d(TAG, "destroy")
            release()
        }

        fun release() {
            synchronized(shLock) {
                _shProcess?.destroy()
                _shProcess = null
            }
        }
    }
}
