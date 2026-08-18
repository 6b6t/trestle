@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package net.blockhost.trestle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import net.blockhost.trestle.domain.InstanceId
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_account
import net.blockhost.trestle.resources.ic_extension
import net.blockhost.trestle.resources.ic_library
import net.blockhost.trestle.resources.ic_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val globalDestinations = listOf(
    LauncherDestination.LIBRARY,
    LauncherDestination.DISCOVER,
    LauncherDestination.ACCOUNTS,
    LauncherDestination.SETTINGS,
)

@Composable
internal fun LauncherNavigationLayout(
    destination: LauncherDestination,
    onDestinationChange: (LauncherDestination) -> Unit,
    adaptiveInfo: WindowAdaptiveInfo,
    modifier: Modifier,
    operation: OperationStatus?,
    onCancelOperation: () -> Unit,
    onOpenOperation: (InstanceId) -> Unit,
    destinationContent: @Composable (Modifier, TrestleLayoutMode) -> Unit,
) {
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val layoutMode = adaptiveInfo.trestleLayoutMode()
    val navigationSuiteType = when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) &&
            windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) ->
            NavigationSuiteType.WideNavigationRailExpanded
        else -> NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
    }
    NavigationSuiteScaffold(
        navigationItems = {
            globalDestinations.forEach { destinationItem ->
                NavigationSuiteItem(
                    selected = destination == destinationItem ||
                        destinationItem == LauncherDestination.LIBRARY &&
                        destination == LauncherDestination.INSTANCE,
                    onClick = { onDestinationChange(destinationItem) },
                    icon = {
                        Icon(painterResource(destinationIcon(destinationItem)), contentDescription = null)
                    },
                    label = { Text(stringResource(destinationItem.label)) },
                    modifier = Modifier.testTag(LauncherTestTags.navigation(destinationItem)),
                    navigationSuiteType = navigationSuiteType,
                )
            }
        },
        modifier = modifier.testTag(LauncherTestTags.TOP_NAVIGATION),
        navigationSuiteType = navigationSuiteType,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            shortNavigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        containerColor = MaterialTheme.colorScheme.background,
        primaryActionContent = {
            if (layoutMode != TrestleLayoutMode.COMPACT) {
                Box(
                    modifier = Modifier.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BridgeMark(Modifier.size(width = 36.dp, height = 28.dp))
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            destinationContent(Modifier.weight(1f).fillMaxWidth(), layoutMode)
            operation?.let { status ->
                OperationBar(
                    status = status,
                    onCancel = onCancelOperation,
                    onClick = status.instanceId?.let { instanceId ->
                        { onOpenOperation(instanceId) }
                    },
                )
            }
        }
    }
}

private fun destinationIcon(destination: LauncherDestination): DrawableResource = when (destination) {
    LauncherDestination.LIBRARY -> Res.drawable.ic_library
    LauncherDestination.INSTANCE -> Res.drawable.ic_library
    LauncherDestination.DISCOVER -> Res.drawable.ic_extension
    LauncherDestination.ACCOUNTS -> Res.drawable.ic_account
    LauncherDestination.SETTINGS -> Res.drawable.ic_settings
}
