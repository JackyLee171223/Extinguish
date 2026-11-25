package own.moderpach.extinguish.home.cards

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import own.moderpach.extinguish.ExtinguishNavGraph
import own.moderpach.extinguish.ExtinguishNavRoute
import own.moderpach.extinguish.R
import own.moderpach.extinguish.home.HomeScreenCardKey
import own.moderpach.extinguish.home.HomeScreenCardKeys
import own.moderpach.extinguish.service.ExtinguishService
import own.moderpach.extinguish.settings.AppWhitelist
import own.moderpach.extinguish.settings.data.ISettingsRepository
import own.moderpach.extinguish.ui.components.ExtinguishCard
import own.moderpach.extinguish.ui.components.ExtinguishListItem

val HomeScreenCardKeys.appWhitelist: HomeScreenCardKey get() = "AppWhitelist"

fun LazyStaggeredGridScope.appWhitelistCard(
    extinguishServiceState: ExtinguishService.State,
    settingsRepository: ISettingsRepository,
    onNavigateTo: (ExtinguishNavRoute) -> Unit
) = item(
    key = HomeScreenCardKeys.appWhitelist
) {
    AppWhitelistCard(
        settingsRepository,
        onNavigateTo
    )
}

@Composable
fun AppWhitelistCard(
    settingsRepository: ISettingsRepository,
    onNavigateTo: (ExtinguishNavRoute) -> Unit
) {
    val count = settingsRepository.appWhitelist.packages.size
    val enabled = settingsRepository.appWhitelist.enabled
    val supporting = if (enabled && count > 0) {
        stringResource(R.string.str_appWhitelistCardSelected, count)
    } else {
        stringResource(R.string.str_appWhitelistCardOff)
    }
    ExtinguishCard(
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        Text(
            stringResource(R.string.Volume_key_control),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )
        ExtinguishListItem(
            headlineContent = {
                Text(stringResource(R.string.App_whitelist))
            },
            supportingContent = {
                Text(supporting)
            },
            onClick = { onNavigateTo(ExtinguishNavGraph.AppWhitelist) },
            morePaddingForPureText = false,
            headlineTextStyle = MaterialTheme.typography.titleMedium,
            overlineTextStyle = MaterialTheme.typography.labelMedium,
            supportingTextStyle = MaterialTheme.typography.bodySmall
        )
    }
}
