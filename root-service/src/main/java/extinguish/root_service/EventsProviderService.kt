package extinguish.root_service

import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import extinguish.ipc.result.EventResult

/**
 * Runs inside a privileged root process spawned by libsu (app_process, uid 0).
 * Reads input events via `getevent` executed as root and dispatches them to listeners.
 */
class EventsProviderService : RootService() {

    companion object {

        const val TAG = "EventsProviderService"

        private fun debugLog(msg: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, msg)
        }
    }

    private val binder = EventsProviderBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        binder.release()
        super.onDestroy()
    }

    private class EventsProviderBinder : IEventsProvider.Stub() {

        private val mLock = Object()
        private var isRunning = false
        private var mThread: Thread? = null
        private val listenerList = RemoteCallbackList<IEventsListener>()

        private fun newProviderThread(filter: String?) = Thread {
            val process: Process = Runtime.getRuntime().exec(arrayOf("sh"))
            val output = process.outputStream
            val input = process.inputStream

            output.write("getevent | grep $filter\n".toByteArray())
            output.flush()
            output.close()

            try {
                val bufferedReader = input.bufferedReader()
                while (!Thread.interrupted()) {
                    if (!bufferedReader.ready()) {
                        Thread.sleep(100)
                        continue
                    }
                    bufferedReader.readLine().let {
                        debugLog("get event - $it")
                        val eventTexts = it.split(' ')
                        if (eventTexts.size == 4) {
                            val listenerSize = listenerList.beginBroadcast()
                            for (i in 0 until listenerSize) {
                                listenerList.getBroadcastItem(i).onEvent(
                                    EventResult(
                                        event = eventTexts[0],
                                        v0 = eventTexts[1],
                                        v1 = eventTexts[2],
                                        v2 = eventTexts[3],
                                    )
                                )
                            }
                        }
                        listenerList.finishBroadcast()
                    }
                }
            } catch (e: Exception) {
                debugLog("mThread: getting interrupt. $e")
            } finally {
                input.close()
                process.destroy()
            }
        }

        override fun isRunning(): Boolean {
            return isRunning
        }

        override fun launch(filter: String?) {
            debugLog("#launch with $filter")
            if (isRunning) return
            synchronized(mLock) {
                if (isRunning) return
                isRunning = true
                mThread = newProviderThread(filter)
                mThread?.start()
            }
        }

        override fun stop() {
            debugLog("#stop")
            if (!isRunning) return
            synchronized(mLock) {
                if (!isRunning) return
                isRunning = false
                debugLog("#stop: interrupt")
                mThread?.interrupt()
                debugLog("#stop: join")
                mThread?.join()
                debugLog("#stop: mThread = null")
                mThread = null
            }
        }

        override fun registerListener(listener: IEventsListener?) {
            debugLog("#registerListener")
            if (listener == null) return
            listenerList.register(listener)
        }

        override fun unregisterListener(listener: IEventsListener?) {
            debugLog("#unregisterListener")
            if (listener == null) return
            listenerList.unregister(listener)
        }

        override fun getForegroundPackage(): String {
            return try {
                val process = Runtime.getRuntime().exec(
                    arrayOf(
                        "sh", "-c",
                        "dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity'"
                    )
                )
                val output = process.inputStream.bufferedReader().use { it.readText() }
                process.destroy()
                parseForegroundPackage(output)
            } catch (e: Exception) {
                debugLog("getForegroundPackage: $e")
                ""
            }
        }

        private fun parseForegroundPackage(output: String): String {
            // Component form: com.example.app/.MainActivity or
            // com.example.app/com.example.app.MainActivity
            val regex = Regex("([a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+)/")
            return regex.find(output)?.groupValues?.get(1) ?: ""
        }

        override fun destroy() {
            debugLog("#destroy")
            release()
        }

        fun release() {
            stop()
            listenerList.kill()
        }
    }
}
