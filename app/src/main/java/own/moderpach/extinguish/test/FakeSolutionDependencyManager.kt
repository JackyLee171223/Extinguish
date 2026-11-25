package own.moderpach.extinguish.test

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import own.moderpach.extinguish.ISolutionDependencyManager
import own.moderpach.extinguish.SolutionDependencyState

class FakeSolutionDependencyManager : ISolutionDependencyManager {
    override val state: StateFlow<SolutionDependencyState> =
        MutableStateFlow(SolutionDependencyState(isChecking = false))

    override fun updateImmediately() {}

    override fun requestRootPermission(onResult: (Boolean) -> Unit) {
        onResult(state.value.isRootGranted)
    }

    override fun requestShizukuPermission(onShouldShowRequestPermissionRationale: () -> Unit) {}

    override fun destroy() {}
}
