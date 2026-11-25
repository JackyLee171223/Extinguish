package own.moderpach.extinguish.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import own.moderpach.extinguish.ISolutionDependencyManager
import own.moderpach.extinguish.ISystemPermissionsManager
import own.moderpach.extinguish.R
import own.moderpach.extinguish.SolutionDependencyManager
import own.moderpach.extinguish.SpecificPermission
import own.moderpach.extinguish.SystemPermissionsManager
import own.moderpach.extinguish.isGranted
import own.moderpach.extinguish.settings.data.SyncSettingsRepository
import own.moderpach.extinguish.settings.data.settingsDataStore

/**
 * Single unified quick-settings tile acting as the service power switch.
 *
 * First tap starts ExtinguishService and turns the screen off. Once the
 * service is running (screen on/off is then controlled by the volume keys),
 * the next tap stops the service entirely. If the required privilege has not
 * been granted yet, the same tap requests it and continues automatically.
 */
class ExtinguishTile : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listeningScope: CoroutineScope? = null
    private var clickJob: Job? = null
    private lateinit var solutionDependencyManager: ISolutionDependencyManager
    private lateinit var systemPermissionsManager: ISystemPermissionsManager
    private lateinit var settingsRepository: SyncSettingsRepository

    override fun onCreate() {
        super.onCreate()
        solutionDependencyManager = SolutionDependencyManager(this).also {
            it.updateImmediately()
        }
        systemPermissionsManager = SystemPermissionsManager(this)
        settingsRepository = SyncSettingsRepository(settingsDataStore)
    }

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        listeningScope?.cancel()
        listeningScope = scope
        scope.launch {
            ExtinguishService.state.collectLatest { serviceState ->
                updateTileState(serviceState)
            }
        }
        scope.launch {
            settingsRepository.keptUpdate()
        }
    }

    override fun onStopListening() {
        listeningScope?.cancel()
        listeningScope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (clickJob?.isActive == true) return
        clickJob = serviceScope.launch {
            settingsRepository.update()
            solutionDependencyManager.updateImmediately()

            // If the service is already running, this tap turns the service off.
            // Screen on/off while running is handled by the volume keys.
            if (ExtinguishService.state.value == ExtinguishService.State.Prepared) {
                stopService()
                return@launch
            }

            val mode = settingsRepository.privilegeMode
            if (solutionDependencyManager.state.value.isGranted(mode)) {
                startServiceAndTurnScreenOff()
            } else {
                when (mode) {
                    own.moderpach.extinguish.settings.data.SettingsTokens.PrivilegeModeValue.Root ->
                        solutionDependencyManager.requestRootPermission { granted ->
                            if (granted) serviceScope.launch { startServiceAndTurnScreenOff() }
                        }

                    own.moderpach.extinguish.settings.data.SettingsTokens.PrivilegeModeValue.Shizuku ->
                        solutionDependencyManager.requestShizukuPermission { }
                }
            }
        }
    }

    private fun stopService() {
        val intent = Intent(this, ExtinguishService::class.java).apply {
            putExtra(
                ExtinguishService.EXTRA_STOP,
                ExtinguishService.EXTRA_VALUE_STOP
            )
        }
        startService(intent)
    }

    private fun startServiceAndTurnScreenOff() {
        if (
            settingsRepository.floatingButton.enabled &&
            !systemPermissionsManager.checkSpecial(SpecificPermission.CanDrawOverlays)
        ) return

        // SWITCH handles the initial start: processArgs waits until the
        // privileged services are bound and prepared, then turns the screen off.
        val intent = Intent(this, ExtinguishService::class.java).apply {
            putExtra(
                ExtinguishService.EXTRA_SCREEN,
                ExtinguishService.EXTRA_VALUE_SCREEN_SWITCH
            )
        }
        startService(intent)
    }

    private fun updateTileState(serviceState: ExtinguishService.State) {
        val tile = qsTile ?: return
        val running = serviceState == ExtinguishService.State.Prepared
        setQs(
            tile,
            if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
            if (running) R.string.On else R.string.Off
        )
    }

    private fun setQs(tile: Tile, state: Int, descriptionRes: Int) {
        tile.state = state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = getText(descriptionRes)
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        listeningScope?.cancel()
        listeningScope = null
        serviceScope.cancel()
        solutionDependencyManager.destroy()
        super.onDestroy()
    }
}
