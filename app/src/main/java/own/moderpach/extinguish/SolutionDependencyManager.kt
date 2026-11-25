package own.moderpach.extinguish

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Root-based replacement for the previous Shizuku dependency manager.
 *
 * Uses libsu to query and request root access. Root services are hosted in a
 * privileged process spawned by libsu (see extinguish.root_service).
 */
class SolutionDependencyManager(
    private val context: Context
) : ISolutionDependencyManager {

    private val _state = MutableStateFlow(SolutionDependencyState())
    override val state: StateFlow<SolutionDependencyState> = _state

    private val rootRequestLock = Any()
    private var rootRequestInFlight = false
    private val rootRequestCallbacks = mutableListOf<(Boolean) -> Unit>()

    override fun updateImmediately() {
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
            SolutionDependencyState(
                isRootAvailable = available,
                isRootGranted = granted,
                isChecking = false
            )
        }
    }

    override fun destroy() {}
}

data class SolutionDependencyState(
    val isRootAvailable: Boolean = false,
    val isRootGranted: Boolean = false,
    val isChecking: Boolean = true
)
