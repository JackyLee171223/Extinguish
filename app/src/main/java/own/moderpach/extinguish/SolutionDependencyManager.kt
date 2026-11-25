package own.moderpach.extinguish

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import rikka.shizuku.Shizuku

/**
 * Tracks both privilege backends at the same time so the user can freely
 * switch between Root (libsu) and Shizuku without reinstalling.
 *
 * - Root services run in a privileged process spawned by libsu.
 * - Shizuku services run as a Shizuku user service.
 * Both implementations share one AIDL interface (:root-service).
 */
class SolutionDependencyManager(
    private val context: Context
) : ISolutionDependencyManager {

    private val _state = MutableStateFlow(SolutionDependencyState())
    override val state: StateFlow<SolutionDependencyState> = _state

    //== Root (libsu)

    private val rootRequestLock = Any()
    private var rootRequestInFlight = false
    private val rootRequestCallbacks = mutableListOf<(Boolean) -> Unit>()

    //== Shizuku

    private interface ShizukuDependencyListener {
        val binderReceivedListener: Shizuku.OnBinderReceivedListener
        val binderDeadListener: Shizuku.OnBinderDeadListener
        val requestPermissionResultListener: Shizuku.OnRequestPermissionResultListener
    }

    private val shizuku = object : ShizukuDependencyListener {
        override val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            _state.getAndUpdate {
                it.copy(isShizukuBinderAlive = true)
            }
        }
        override val binderDeadListener = Shizuku.OnBinderDeadListener {
            _state.getAndUpdate {
                it.copy(isShizukuBinderAlive = false, isShizukuPermissionGranted = false)
            }
        }
        override val requestPermissionResultListener =
            Shizuku.OnRequestPermissionResultListener { _, grantResult ->
                _state.getAndUpdate {
                    it.copy(isShizukuPermissionGranted = grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
    }

    init {
        runCatching {
            Shizuku.addBinderReceivedListener(shizuku.binderReceivedListener)
            Shizuku.addBinderDeadListener(shizuku.binderDeadListener)
            Shizuku.addRequestPermissionResultListener(shizuku.requestPermissionResultListener)
        }
    }

    override fun updateImmediately() {
        updateShizukuState()
        updateRootState()
    }

    private fun updateShizukuState() {
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrElse { false }
        val permissionGranted = runCatching {
            binderAlive && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrElse { false }
        _state.update {
            it.copy(
                isShizukuBinderAlive = binderAlive,
                isShizukuPermissionGranted = permissionGranted
            )
        }
    }

    private fun updateRootState() {
        // isAppGrantedRoot() returns null after a cold start until at least one
        // root shell has been created. In that case, actively create/acquire a
        // shell instead of incorrectly treating the unknown state as denied.
        when (runCatching { Shell.isAppGrantedRoot() }.getOrNull()) {
            true -> publishRootState(granted = true, available = true)
            false -> publishRootState(granted = false, available = false)
            null -> requestRootShell()
        }
    }

    override fun requestRootPermission(onResult: (Boolean) -> Unit) {
        if (runCatching { Shell.isAppGrantedRoot() }.getOrNull() == true) {
            publishRootState(granted = true, available = true)
            onResult(true)
            return
        }
        requestRootShell(onResult)
    }

    private fun requestRootShell(onResult: ((Boolean) -> Unit)? = null) {
        synchronized(rootRequestLock) {
            if (onResult != null) rootRequestCallbacks += onResult
            if (rootRequestInFlight) return
            rootRequestInFlight = true
        }
        _state.update { it.copy(isChecking = true) }

        // getShell() reuses a cached shell or creates one asynchronously. If the
        // app was permanently granted in Magisk, this succeeds without asking
        // again; otherwise it triggers the normal su/Magisk authorization flow.
        runCatching {
            Shell.getShell { shell ->
                completeRootRequest(granted = shell.isRoot, available = true)
            }
        }.onFailure {
            completeRootRequest(granted = false, available = false)
        }
    }

    private fun completeRootRequest(granted: Boolean, available: Boolean) {
        val callbacks = synchronized(rootRequestLock) {
            rootRequestInFlight = false
            rootRequestCallbacks.toList().also { rootRequestCallbacks.clear() }
        }
        publishRootState(granted, available)
        callbacks.forEach { callback -> runCatching { callback(granted) } }
    }

    private fun publishRootState(granted: Boolean, available: Boolean) {
        _state.update {
            it.copy(
                isRootAvailable = available,
                isRootGranted = granted,
                isChecking = false
            )
        }
    }

    override fun requestShizukuPermission(onShouldShowRequestPermissionRationale: () -> Unit) {
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrElse { false }
        val permissionGranted = runCatching {
            binderAlive && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrElse { false }
        if (permissionGranted) return
        if (runCatching { Shizuku.shouldShowRequestPermissionRationale() }.getOrElse { false }) {
            onShouldShowRequestPermissionRationale()
            return
        }
        runCatching { Shizuku.requestPermission(1) }
    }

    override fun destroy() {
        runCatching {
            Shizuku.removeBinderReceivedListener(shizuku.binderReceivedListener)
            Shizuku.removeBinderDeadListener(shizuku.binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(shizuku.requestPermissionResultListener)
        }
    }
}

data class SolutionDependencyState(
    val isRootAvailable: Boolean = false,
    val isRootGranted: Boolean = false,
    val isShizukuBinderAlive: Boolean = false,
    val isShizukuPermissionGranted: Boolean = false,
    val isChecking: Boolean = true
)
