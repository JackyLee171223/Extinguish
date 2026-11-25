package own.moderpach.extinguish

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.StateFlow
import own.moderpach.extinguish.settings.data.SettingsTokens.PrivilegeModeValue

interface ISolutionDependencyManager {
    val state: StateFlow<SolutionDependencyState>

    fun updateImmediately()
    fun requestRootPermission(onResult: (Boolean) -> Unit = {})
    fun requestShizukuPermission(onShouldShowRequestPermissionRationale: () -> Unit)
    fun destroy()
}

/** Returns whether the requested privilege method is currently usable. */
fun SolutionDependencyState.isGranted(mode: PrivilegeModeValue): Boolean = when (mode) {
    PrivilegeModeValue.Root -> isRootGranted
    PrivilegeModeValue.Shizuku -> isShizukuBinderAlive && isShizukuPermissionGranted
}

val LocalSolutionDependencyManager = compositionLocalOf<ISolutionDependencyManager> {
    createNoting()
}

private fun createNoting(): Nothing {
    throw IllegalAccessException("LocalSolutionDependencyManager should be provided manually")
}
