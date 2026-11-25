package own.moderpach.extinguish.settings

import android.content.pm.ApplicationInfo
import android.icu.text.Transliterator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import own.moderpach.extinguish.ExtinguishNavGraph
import own.moderpach.extinguish.ExtinguishNavRoute
import own.moderpach.extinguish.R
import own.moderpach.extinguish.settings.components.EnablerCard
import own.moderpach.extinguish.settings.components.SettingCard
import own.moderpach.extinguish.settings.components.SettingListItem
import own.moderpach.extinguish.settings.data.ISettingsRepository
import own.moderpach.extinguish.ui.components.ExtinguishTopAppBarWithNavigationBack
import own.moderpach.extinguish.ui.navigation.extinguishComposable
import java.util.Locale

val ExtinguishNavGraph.AppWhitelist: ExtinguishNavRoute get() = "AppWhitelist"

fun NavGraphBuilder.appWhitelist(
    onBack: () -> Unit,
    settingsRepository: ISettingsRepository,
) = extinguishComposable(
    ExtinguishNavGraph.AppWhitelist,
) {
    AppWhitelistScreen(onBack, settingsRepository)
}

private data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val sortKey: String,
    val sectionLetter: Char,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppWhitelistScreen(
    onBack: () -> Unit,
    settingsRepository: ISettingsRepository,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var installedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var selected by remember { mutableStateOf(settingsRepository.appWhitelist.packages) }

    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            // "Any-Latin; Latin-ASCII" transliterates Chinese into pinyin without tones,
            // so Chinese app names land in the correct A-Z section instead of "Z#".
            val transliterator = runCatching {
                Transliterator.getInstance("Any-Latin; Latin-ASCII")
            }.getOrNull()
            pm.getInstalledPackages(0)
                .mapNotNull { info ->
                    val appInfo = info.applicationInfo ?: return@mapNotNull null
                    val label = runCatching { appInfo.loadLabel(pm).toString() }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: info.packageName
                    if (info.packageName == context.packageName) return@mapNotNull null
                    if (pm.getLaunchIntentForPackage(info.packageName) == null) return@mapNotNull null
                    AppEntry(
                        packageName = info.packageName,
                        label = label,
                        isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        sortKey = buildSortKey(label, transliterator),
                        sectionLetter = sectionLetterOf(label, transliterator),
                    )
                }
                .sortedWith(
                    compareBy<AppEntry> { sectionOrder(it.sectionLetter) }
                        .thenBy { it.sortKey }
                )
        }
        installedApps = apps
    }

    val selectedSet = remember(selected) { selected }
    val selectedApps = remember(installedApps, selectedSet) {
        installedApps.filter { it.packageName in selectedSet }
    }
    val availableApps = remember(installedApps, selectedSet) {
        installedApps.filterNot { it.packageName in selectedSet }
    }

    // Build the letter sections from the available (unselected) apps.
    val sections = remember(availableApps) { buildLetterSections(availableApps) }
    val letterToIndex = remember(sections, selectedApps) {
        // item 0 = enabler card, item 1 = help card.
        var index = 2
        if (selectedApps.isNotEmpty()) {
            index += 1 // "selected" sticky header
            index += selectedApps.size
        }
        val map = mutableMapOf<Char, Int>()
        sections.forEach { (letter, apps) ->
            map[letter] = index // sticky header slot for this letter
            index += 1 + apps.size
        }
        map
    }

    fun toggle(packageName: String) {
        val newSet = if (packageName in selected) selected - packageName else selected + packageName
        selected = newSet
        settingsRepository.appWhitelist.packages = newSet
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.displayCutout),
        topBar = {
            ExtinguishTopAppBarWithNavigationBack(
                onBack = onBack,
                titleString = stringResource(R.string.App_whitelist),
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                // Leave room on the end so rows never sit under the alphabet index.
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 52.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    EnablerCard(
                        text = stringResource(R.string.str_appWhitelistEnabled),
                        checked = settingsRepository.appWhitelist.enabled,
                        onCheckedChange = { settingsRepository.appWhitelist.enabled = it }
                    )
                }
                item {
                    SettingCard {
                        SettingListItem(
                            headline = stringResource(R.string.str_appWhitelistHelp),
                        )
                    }
                }

                if (selectedApps.isNotEmpty()) {
                    stickyHeader {
                        SectionHeader(
                            text = stringResource(R.string.str_appWhitelistSelected) + " (${selectedApps.size})"
                        )
                    }
                    items(selectedApps, key = { "sel_" + it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = true,
                            onToggle = { toggle(app.packageName) },
                        )
                    }
                }

                sections.forEach { (letter, apps) ->
                    stickyHeader {
                        SectionHeader(text = letter.toString())
                    }
                    items(apps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = false,
                            onToggle = { toggle(app.packageName) },
                        )
                    }
                }
            }

            // Right-side alphabet quick index: tap or drag to jump, kept away from the edge.
            AlphabetIndex(
                letters = sections.keys.toList(),
                modifier = Modifier.align(Alignment.CenterEnd),
                onLetterSelect = { letter ->
                    letterToIndex[letter]?.let { idx ->
                        coroutineScope.launch { listState.scrollToItem(idx) }
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

@Composable
private fun AppRow(
    app: AppEntry,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val iconBitmap = remember(app.packageName) {
        runCatching {
            val drawable = pm.getApplicationIcon(app.packageName)
            drawable.toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(modifier = Modifier.size(40.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun AlphabetIndex(
    letters: List<Char>,
    modifier: Modifier = Modifier,
    onLetterSelect: (Char) -> Unit,
) {
    if (letters.isEmpty()) return
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { INDEX_ITEM_HEIGHT.toPx() }

    Box(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                .pointerInput(letters, itemHeightPx) {
                    var lastIndex = -1
                    fun selectAt(y: Float) {
                        val index = (y / itemHeightPx).toInt().coerceIn(0, letters.lastIndex)
                        if (index != lastIndex) {
                            lastIndex = index
                            activeLetter = letters[index]
                            onLetterSelect(letters[index])
                        }
                    }
                    detectDragGestures(
                        onDragStart = { selectAt(it.y) },
                        onDrag = { change, _ ->
                            change.consume()
                            selectAt(change.position.y)
                        },
                        onDragEnd = {
                            activeLetter = null
                            lastIndex = -1
                        },
                        onDragCancel = {
                            activeLetter = null
                            lastIndex = -1
                        },
                    )
                },
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                val isActive = letter == activeLetter
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(INDEX_ITEM_HEIGHT)
                        .then(
                            if (isActive) {
                                Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onLetterSelect(letter) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private val INDEX_ITEM_HEIGHT = 20.dp

private fun sectionOrder(letter: Char): Int = if (letter == '#') 26 else letter - 'A'

private fun sectionLetterOf(label: String, transliterator: Transliterator?): Char {
    val trimmed = label.trim()
    val first = trimmed.firstOrNull() ?: return '#'
    val upper = first.uppercaseChar()
    if (upper in 'A'..'Z') return upper
    // Chinese / other non-Latin scripts: transliterate to pinyin and take the first letter.
    if (first.code > 0x7F && transliterator != null) {
        val romanized = runCatching { transliterator.transliterate(trimmed) }
            .getOrNull()
            .orEmpty()
        val letter = romanized.firstOrNull { it.isLetter() }?.uppercaseChar()
        if (letter != null && letter in 'A'..'Z') return letter
    }
    return '#'
}

private fun buildSortKey(label: String, transliterator: Transliterator?): String {
    val trimmed = label.trim()
    if (trimmed.all { it.code < 0x80 }) return trimmed.lowercase(Locale.ROOT)
    val romanized = runCatching { transliterator?.transliterate(trimmed) }
        .getOrNull()
        .orEmpty()
    val normalized = romanized.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)
    return normalized.ifEmpty { trimmed.lowercase(Locale.ROOT) }
}

private fun buildLetterSections(apps: List<AppEntry>): Map<Char, List<AppEntry>> =
    apps.groupBy { it.sectionLetter }
        .toSortedMap(compareBy<Char> { if (it == '#') 1 else 0 }.thenBy { it })
        .mapValues { (_, list) -> list.sortedBy { it.sortKey } }
