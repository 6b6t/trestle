@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package net.blockhost.trestle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import net.blockhost.trestle.auth.AccountAuthenticationMethod
import net.blockhost.trestle.auth.ManagedAccount
import net.blockhost.trestle.auth.MinecraftEdition
import net.blockhost.trestle.auth.SkinVariant
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.ic_add
import net.blockhost.trestle.resources.ic_arrow_back
import net.blockhost.trestle.resources.ic_more_vert
import net.blockhost.trestle.resources.ic_visibility
import net.blockhost.trestle.resources.ic_visibility_off
import net.blockhost.trestle.resources.ui_accounts
import net.blockhost.trestle.resources.ui_active
import net.blockhost.trestle.resources.ui_add_account
import net.blockhost.trestle.resources.ui_add_an_online_account_import_an_existing
import net.blockhost.trestle.resources.ui_add_offline_account
import net.blockhost.trestle.resources.ui_back
import net.blockhost.trestle.resources.ui_bedrock_authentication_is_stored_for_the_future
import net.blockhost.trestle.resources.ui_bedrock_can_t_launch_java
import net.blockhost.trestle.resources.ui_cancel
import net.blockhost.trestle.resources.ui_choose_an_account_to_view_its_profile
import net.blockhost.trestle.resources.ui_code_expires_at
import net.blockhost.trestle.resources.ui_cookies_grant_access_to_your_microsoft_session
import net.blockhost.trestle.resources.ui_copy_code
import net.blockhost.trestle.resources.ui_copy_player_name
import net.blockhost.trestle.resources.ui_copy_profile_id
import net.blockhost.trestle.resources.ui_edition
import net.blockhost.trestle.resources.ui_enter_a_microsoft_email_and_password_fails
import net.blockhost.trestle.resources.ui_enter_a_microsoft_email_and_password_for
import net.blockhost.trestle.resources.ui_enter_this_code
import net.blockhost.trestle.resources.ui_for_example_1_21_100
import net.blockhost.trestle.resources.ui_forget
import net.blockhost.trestle.resources.ui_forget_account
import net.blockhost.trestle.resources.ui_forget_account_2
import net.blockhost.trestle.resources.ui_get_sign_in_code
import net.blockhost.trestle.resources.ui_hide_password
import net.blockhost.trestle.resources.ui_hide_secret
import net.blockhost.trestle.resources.ui_import_a_session_from_another_launcher_with
import net.blockhost.trestle.resources.ui_import_a_session_from_exported_microsoft_browser
import net.blockhost.trestle.resources.ui_import_account
import net.blockhost.trestle.resources.ui_imported_secret
import net.blockhost.trestle.resources.ui_installed_bedrock_version
import net.blockhost.trestle.resources.ui_login_live_com_cookies_or_cookie_export
import net.blockhost.trestle.resources.ui_manage_skins
import net.blockhost.trestle.resources.ui_microsoft_account_email
import net.blockhost.trestle.resources.ui_microsoft_account_password
import net.blockhost.trestle.resources.ui_microsoft_discourages_direct_password_login_and_it
import net.blockhost.trestle.resources.ui_microsoft_refresh_token
import net.blockhost.trestle.resources.ui_minecraft_access_token
import net.blockhost.trestle.resources.ui_more
import net.blockhost.trestle.resources.ui_more_sign_in_options
import net.blockhost.trestle.resources.ui_no_accounts
import net.blockhost.trestle.resources.ui_offline_accounts_prove_no_ownership_they_only
import net.blockhost.trestle.resources.ui_offline_profile
import net.blockhost.trestle.resources.ui_offline_username
import net.blockhost.trestle.resources.ui_open_microsoft_sign_in
import net.blockhost.trestle.resources.ui_open_named
import net.blockhost.trestle.resources.ui_play_with_any_username_servers_in_online
import net.blockhost.trestle.resources.ui_player_model
import net.blockhost.trestle.resources.ui_profile
import net.blockhost.trestle.resources.ui_profile_id
import net.blockhost.trestle.resources.ui_raw_minecraft_access_tokens_cannot_be_renewed
import net.blockhost.trestle.resources.ui_ready_to_play
import net.blockhost.trestle.resources.ui_recommended
import net.blockhost.trestle.resources.ui_refresh
import net.blockhost.trestle.resources.ui_refresh_profile
import net.blockhost.trestle.resources.ui_reset_skin
import net.blockhost.trestle.resources.ui_reset_skin_explanation
import net.blockhost.trestle.resources.ui_reset_skin_question
import net.blockhost.trestle.resources.ui_retry
import net.blockhost.trestle.resources.ui_show_password
import net.blockhost.trestle.resources.ui_show_secret
import net.blockhost.trestle.resources.ui_sign_in
import net.blockhost.trestle.resources.ui_sign_in_again
import net.blockhost.trestle.resources.ui_sign_in_required
import net.blockhost.trestle.resources.ui_sign_in_to_a_bedrock_edition_account
import net.blockhost.trestle.resources.ui_sign_in_with_a_code_in_your
import net.blockhost.trestle.resources.ui_sign_out
import net.blockhost.trestle.resources.ui_sign_out_explanation
import net.blockhost.trestle.resources.ui_sign_out_question
import net.blockhost.trestle.resources.ui_the_active_account_plays_your_instances
import net.blockhost.trestle.resources.ui_this_removes_the_local_profile_and_saved_credentials_it_does_not_change_
import net.blockhost.trestle.resources.ui_to_letters_numbers_or_underscores
import net.blockhost.trestle.resources.ui_trestle_discards_the_password_after_this_attempt
import net.blockhost.trestle.resources.ui_use
import net.blockhost.trestle.resources.ui_use_a_minecraft_access_token_directly_it
import net.blockhost.trestle.resources.ui_use_a_refresh_token_issued_for_the
import net.blockhost.trestle.resources.ui_use_account
import net.blockhost.trestle.resources.ui_use_to_letters_numbers_or_underscores
import net.blockhost.trestle.resources.ui_waiting
import net.blockhost.trestle.text.labelResource
import net.blockhost.trestle.text.resolve
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AccountsPage(state: LauncherUiState, modifier: Modifier, actions: LauncherUiActions, layoutMode: TrestleLayoutMode) {
    if (state.skinStudio.visible && !state.skinStudio.editor.visible) {
        SkinStudioPage(state, actions, modifier)
        return
    }
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSignOut by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSkinReset by rememberSaveable { mutableStateOf(false) }
    val accountCallbacks: (ManagedAccount) -> AccountCallbacks = { account ->
        AccountCallbacks(
            onForget = { pendingRemoval = account.profile.profileId },
            onSignOut = { pendingSignOut = account.profile.profileId },
            onResetSkin = { pendingSkinReset = true },
            onSignInAgain = {
                actions.openAccountLogin()
                actions.setAccountLoginMethod(account.profile.authenticationMethod)
            },
        )
    }
    var selectedProfileId by rememberSaveable {
        mutableStateOf(state.accounts.firstOrNull { it.isActive }?.profile?.profileId)
    }
    val selectedAccount = state.accounts.firstOrNull { it.profile.profileId == selectedProfileId }
    val adaptiveInfo = LocalTrestleWindowAdaptiveInfo.current ?: currentWindowAdaptiveInfoV2()
    val navigator = rememberListDetailPaneScaffoldNavigator<String?>(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo),
    )
    val scope = rememberCoroutineScope()
    val listPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    val detailPaneHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Hidden
    val openAccount: (ManagedAccount) -> Unit = { account ->
        selectedProfileId = account.profile.profileId
        scope.launch {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, account.profile.profileId)
        }
    }
    val showAccounts: () -> Unit = {
        scope.launch {
            if (navigator.canNavigateBack()) {
                navigator.navigateBack()
            } else {
                navigator.navigateTo(ListDetailPaneScaffoldRole.List)
            }
        }
    }

    LaunchedEffect(state.accounts, detailPaneHidden) {
        if (selectedProfileId !in state.accounts.map { it.profile.profileId }) {
            selectedProfileId = state.accounts.firstOrNull { it.isActive }?.profile?.profileId
                ?: state.accounts.firstOrNull()?.profile?.profileId
        }
        if (!detailPaneHidden && selectedProfileId != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedProfileId)
        }
    }
    PlatformBackHandler(
        enabled = listPaneHidden && navigator.canNavigateBack(),
        onBack = showAccounts,
    )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(LauncherTestTags.ACCOUNTS),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (listPaneHidden && selectedAccount != null) {
                Column {
                    PageHeader(
                        title = selectedAccount.profile.playerName,
                        navigationIcon = {
                            TrestleTooltipIconButton(
                                label = stringResource(Res.string.ui_back),
                                onClick = showAccounts,
                            ) {
                                Icon(
                                    painterResource(Res.drawable.ic_arrow_back),
                                    contentDescription = stringResource(Res.string.ui_back),
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            } else {
                PageHero(
                    title = stringResource(Res.string.ui_accounts),
                    subtitle = stringResource(Res.string.ui_the_active_account_plays_your_instances),
                    accent = MaterialTheme.colorScheme.tertiary,
                ) {
                    if (layoutMode != TrestleLayoutMode.COMPACT && state.accounts.isNotEmpty()) {
                        Button(onClick = actions::openAccountLogin) {
                            Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.ui_add_account))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (layoutMode == TrestleLayoutMode.COMPACT && !listPaneHidden) {
                FloatingActionButton(onClick = actions::openAccountLogin) {
                    Icon(
                        painterResource(Res.drawable.ic_add),
                        contentDescription = stringResource(Res.string.ui_add_account),
                    )
                }
            }
        },
    ) { contentPadding ->
        if (state.accounts.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.ui_no_accounts), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(Res.string.ui_add_an_online_account_import_an_existing),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 520.dp),
                )
                Button(
                    onClick = actions::openAccountLogin,
                    modifier = Modifier.padding(top = 16.dp),
                ) { Text(stringResource(Res.string.ui_add_account)) }
            }
        } else {
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                scaffoldState = navigator.scaffoldState,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                listPane = {
                    AnimatedPane(Modifier.preferredWidth(360.dp)) {
                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(state.accounts, key = { it.profile.profileId }) { account ->
                                    AccountRow(
                                        account = account,
                                        texture = state.accountSkinTextures[account.profile.profileId],
                                        selected = account.profile.profileId == selectedProfileId,
                                        actions = actions,
                                        onOpen = { openAccount(account) },
                                        callbacks = accountCallbacks(account),
                                    )
                                }
                            }
                        }
                    }
                },
                detailPane = {
                    AnimatedPane {
                        if (selectedAccount == null) {
                            Column(
                                Modifier.fillMaxSize().padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(stringResource(Res.string.ui_accounts), style = MaterialTheme.typography.titleLarge)
                                Text(
                                    stringResource(Res.string.ui_choose_an_account_to_view_its_profile),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            AccountDetail(
                                account = selectedAccount,
                                texture = state.accountSkinTextures[selectedAccount.profile.profileId],
                                actions = actions,
                                callbacks = accountCallbacks(selectedAccount),
                                skinLabel = skinPreviewLabel(state, selectedAccount),
                                skinFailed = selectedAccount.profile.profileId in state.failedSkinTextures,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
            )
        }
    }
    pendingSignOut?.let { profileId ->
        ConfirmationDialog(
            ConfirmationRequest(
                title = stringResource(Res.string.ui_sign_out_question),
                text = stringResource(Res.string.ui_sign_out_explanation),
                confirmLabel = stringResource(Res.string.ui_sign_out),
            ) { actions.signOutAccount(profileId) },
        ) { pendingSignOut = null }
    }
    if (pendingSkinReset) {
        ConfirmationDialog(
            ConfirmationRequest(
                title = stringResource(Res.string.ui_reset_skin_question),
                text = stringResource(Res.string.ui_reset_skin_explanation),
                confirmLabel = stringResource(Res.string.ui_reset_skin),
            ) { actions.resetActiveSkin() },
        ) { pendingSkinReset = false }
    }
    pendingRemoval?.let { profileId ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(Res.string.ui_forget_account)) },
            text = {
                Text(stringResource(Res.string.ui_this_removes_the_local_profile_and_saved_credentials_it_does_not_change_))
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(Res.string.ui_cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.removeAccount(profileId)
                        pendingRemoval = null
                        if (listPaneHidden) showAccounts()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(Res.string.ui_forget_account_2)) }
            },
        )
    }
}

@Composable
private fun AccountRow(
    account: ManagedAccount,
    texture: ByteArray?,
    selected: Boolean,
    actions: LauncherUiActions,
    onOpen: () -> Unit,
    callbacks: AccountCallbacks,
) {
    val copyText = rememberCopyText()
    val profileId = account.profile.profileId
    var menuExpanded by rememberSaveable(profileId) { mutableStateOf(false) }
    val statusText = accountStatusText(account)
    val contextActions = buildList {
        if (!account.isActive) {
            add(ContextAction(stringResource(Res.string.ui_use_account)) { actions.selectAccount(profileId) })
        }
        if (account.canManageOfficialProfile) {
            add(ContextAction(stringResource(Res.string.ui_manage_skins)) { actions.openSkinStudio() })
            add(ContextAction(stringResource(Res.string.ui_refresh_profile)) { actions.refreshActiveAccount() })
            add(ContextAction(stringResource(Res.string.ui_reset_skin), onClick = callbacks.onResetSkin))
        }
        if (account.canSignInAgain) {
            add(ContextAction(stringResource(Res.string.ui_sign_in_again), onClick = callbacks.onSignInAgain))
        }
        if (account.isAuthenticated) {
            add(ContextAction(stringResource(Res.string.ui_sign_out), separatorBefore = isNotEmpty(), onClick = callbacks.onSignOut))
        }
        add(
            ContextAction(
                stringResource(Res.string.ui_copy_player_name),
                separatorBefore = true,
            ) { copyText(account.profile.playerName) },
        )
        add(ContextAction(stringResource(Res.string.ui_copy_profile_id)) { copyText(profileId) })
        add(ContextAction(stringResource(Res.string.ui_forget_account_2), separatorBefore = true, onClick = callbacks.onForget))
    }

    ContextActionArea(contextActions) {
        ListItem(
            headlineContent = { Text(account.profile.playerName, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Text(
                    "$statusText · ${stringResource(account.profile.authenticationMethod.labelResource)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = { AccountMark(account, texture, compact = true) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (account.isActive) {
                        Text(
                            stringResource(Res.string.ui_active),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    AccountOverflowMenu(
                        account = account,
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        actions = actions,
                        callbacks = callbacks,
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .selectionOutline(selected)
                .trestleSelectable(
                    selected = selected,
                    onClickLabel = stringResource(Res.string.ui_open_named, account.profile.playerName),
                    onClick = onOpen,
                )
                .testTag(LauncherTestTags.account(profileId)),
        )
    }
}

@Composable
private fun AccountDetail(
    account: ManagedAccount,
    texture: ByteArray?,
    actions: LauncherUiActions,
    callbacks: AccountCallbacks,
    skinLabel: String,
    skinFailed: Boolean,
    modifier: Modifier = Modifier,
) {
    val profileId = account.profile.profileId
    val ready = account.isReady && account.profile.edition == MinecraftEdition.JAVA
    val statusText = accountStatusText(account)
    BoxWithConstraints(modifier) {
        val sideBySide = maxWidth >= 640.dp
        val preview: @Composable (Modifier) -> Unit = { previewModifier ->
            Box(
                previewModifier.clip(MaterialTheme.shapes.extraLarge).background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceContainer),
                    ),
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (account.profile.edition == MinecraftEdition.JAVA) {
                    MinecraftSkinPreview(
                        texture = texture,
                        variant = account.profile.skin?.variant ?: SkinVariant.CLASSIC,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        interactive = texture != null,
                        emptyLabel = skinLabel,
                        background = Color.Transparent,
                    )
                    if (skinFailed) {
                        TextButton(onClick = actions::reloadAccountSkins, modifier = Modifier.align(Alignment.BottomCenter)) {
                            Text(stringResource(Res.string.ui_retry))
                        }
                    }
                } else {
                    AccountMark(account, texture)
                }
            }
        }
        val details: @Composable ColumnScope.() -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(account.profile.playerName, style = MaterialTheme.typography.headlineLarge)
                if (account.isActive) StatusBadge(stringResource(Res.string.ui_active), MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(8.dp).background(
                        if (ready) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        CircleShape,
                    ),
                )
                Text(
                    "$statusText · ${stringResource(account.profile.authenticationMethod.labelResource)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (account.canSignInAgain) {
                    Button(onClick = callbacks.onSignInAgain) { Text(stringResource(Res.string.ui_sign_in_again)) }
                }
                if (!account.isActive) {
                    Button(onClick = { actions.selectAccount(profileId) }) { Text(stringResource(Res.string.ui_use)) }
                }
                if (account.canManageOfficialProfile) {
                    Button(onClick = actions::openSkinStudio) { Text(stringResource(Res.string.ui_manage_skins)) }
                    FilledTonalButton(onClick = actions::refreshActiveAccount) { Text(stringResource(Res.string.ui_refresh)) }
                }
                if (account.isAuthenticated) {
                    FilledTonalButton(onClick = callbacks.onSignOut) {
                        Text(stringResource(Res.string.ui_sign_out))
                    }
                }
            }
            SectionCard(title = stringResource(Res.string.ui_profile)) {
                PropertyRow(
                    stringResource(Res.string.ui_edition),
                    account.profile.edition.name.lowercase().replaceFirstChar(Char::uppercase),
                )
                PropertyRow(stringResource(Res.string.ui_profile_id), profileId)
                account.profile.skin?.let { skin ->
                    PropertyRow(stringResource(Res.string.ui_player_model), skin.variant.label, divider = false)
                }
            }
            TextButton(onClick = callbacks.onForget) {
                Text(stringResource(Res.string.ui_forget), color = MaterialTheme.colorScheme.error)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        ) {
            if (sideBySide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    preview(Modifier.width(260.dp).height(360.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), content = details)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    preview(Modifier.fillMaxWidth().height(280.dp))
                    details()
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.copy(alpha = 0.16f), contentColor = color, shape = CircleShape) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

private val ManagedAccount.canManageOfficialProfile: Boolean
    get() = isActive &&
        isAuthenticated &&
        profile.edition == MinecraftEdition.JAVA

@Composable
private fun AccountOverflowMenu(
    account: ManagedAccount,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: LauncherUiActions,
    callbacks: AccountCallbacks,
) {
    val menuAction: (() -> Unit) -> Unit = { action ->
        onExpandedChange(false)
        action()
    }
    Box {
        TrestleTooltipIconButton(
            label = stringResource(Res.string.ui_more),
            onClick = { onExpandedChange(true) },
        ) {
            Icon(
                painterResource(Res.drawable.ic_more_vert),
                contentDescription = stringResource(Res.string.ui_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            if (!account.isActive) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.ui_use_account)) },
                    onClick = { menuAction { actions.selectAccount(account.profile.profileId) } },
                )
            }
            if (account.canSignInAgain) {
                DropdownMenuItem(text = {
                    Text(stringResource(Res.string.ui_sign_in_again))
                }, onClick = { menuAction(callbacks.onSignInAgain) })
            }
            if (account.canManageOfficialProfile) {
                DropdownMenuItem(text = {
                    Text(stringResource(Res.string.ui_refresh_profile))
                }, onClick = { menuAction(actions::refreshActiveAccount) })
                DropdownMenuItem(text = { Text(stringResource(Res.string.ui_reset_skin)) }, onClick = { menuAction(callbacks.onResetSkin) })
                HorizontalDivider()
            }
            if (account.isAuthenticated) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.ui_sign_out)) }, onClick = { menuAction(callbacks.onSignOut) })
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.ui_forget), color = MaterialTheme.colorScheme.error) },
                onClick = { menuAction(callbacks.onForget) },
            )
        }
    }
}

@Composable
internal fun AccountLoginDialog(state: LauncherUiState, actions: LauncherUiActions) {
    val form = state.accountLogin
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    val copyText = rememberCopyText()
    var passwordVisible by remember(form.method) { mutableStateOf(false) }
    var importedSecretVisible by remember(form.method) { mutableStateOf(false) }
    TrestleDialog(
        onDismissRequest = { if (!form.isWaiting) actions.closeAccountLogin() },
        maxWidth = 520.dp,
        maxHeight = 720.dp,
        modifier = Modifier
            .dismissOnEscape(enabled = !form.isWaiting, onDismiss = actions::closeAccountLogin)
            .testTag(LauncherTestTags.ACCOUNT_LOGIN_DIALOG),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column {
            TrestleDialogHeader(
                title = stringResource(Res.string.ui_add_account),
                onClose = actions::closeAccountLogin,
                closeEnabled = !form.isWaiting,
            )
            Column(
                Modifier.weight(1f, fill = false).padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AccountMethodPicker(
                    selected = form.method,
                    enabled = !form.isWaiting,
                    onSelect = actions::setAccountLoginMethod,
                )
                if (form.edition == MinecraftEdition.BEDROCK) {
                    OutlinedTextField(
                        value = form.bedrockGameVersion,
                        onValueChange = actions::setBedrockGameVersion,
                        label = { Text(stringResource(Res.string.ui_installed_bedrock_version)) },
                        placeholder = { Text(stringResource(Res.string.ui_for_example_1_21_100)) },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(Res.string.ui_bedrock_authentication_is_stored_for_the_future),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (
                    form.method == AccountAuthenticationMethod.MICROSOFT_CREDENTIALS ||
                    form.method == AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS
                ) {
                    OutlinedTextField(
                        value = form.email,
                        onValueChange = actions::setAccountEmail,
                        label = { Text(stringResource(Res.string.ui_microsoft_account_email)) },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = form.password.reveal(),
                        onValueChange = actions::setAccountPassword,
                        label = { Text(stringResource(Res.string.ui_microsoft_account_password)) },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TrestleTooltipIconButton(
                                label = if (passwordVisible) {
                                    stringResource(
                                        Res.string.ui_hide_password,
                                    )
                                } else {
                                    stringResource(Res.string.ui_show_password)
                                },
                                onClick = { passwordVisible = !passwordVisible },
                            ) {
                                Icon(
                                    painterResource(
                                        if (passwordVisible) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility,
                                    ),
                                    contentDescription = if (passwordVisible) {
                                        stringResource(
                                            Res.string.ui_hide_password,
                                        )
                                    } else {
                                        stringResource(Res.string.ui_show_password)
                                    },
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (form.canSubmit && !form.isWaiting) actions.signInAccount() },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(Res.string.ui_microsoft_discourages_direct_password_login_and_it) +
                            stringResource(Res.string.ui_trestle_discards_the_password_after_this_attempt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (form.method.requiresImportedSecret) {
                    OutlinedTextField(
                        value = form.importedSecret.reveal(),
                        onValueChange = actions::setImportedAccountSecret,
                        label = { Text(form.method.secretInputLabel) },
                        enabled = !form.isWaiting,
                        singleLine = form.method != AccountAuthenticationMethod.MICROSOFT_COOKIES,
                        minLines = if (form.method == AccountAuthenticationMethod.MICROSOFT_COOKIES) 3 else 1,
                        maxLines = if (form.method == AccountAuthenticationMethod.MICROSOFT_COOKIES) 5 else 1,
                        visualTransformation = if (importedSecretVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TrestleTooltipIconButton(
                                label = if (importedSecretVisible) {
                                    stringResource(
                                        Res.string.ui_hide_secret,
                                    )
                                } else {
                                    stringResource(Res.string.ui_show_secret)
                                },
                                onClick = { importedSecretVisible = !importedSecretVisible },
                            ) {
                                Icon(
                                    painterResource(
                                        if (importedSecretVisible) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility,
                                    ),
                                    contentDescription = if (importedSecretVisible) {
                                        stringResource(
                                            Res.string.ui_hide_secret,
                                        )
                                    } else {
                                        stringResource(Res.string.ui_show_secret)
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(form.method.importWarning, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (form.method == AccountAuthenticationMethod.OFFLINE) {
                    OutlinedTextField(
                        value = form.offlineUsername,
                        onValueChange = actions::setOfflineUsername,
                        label = { Text(stringResource(Res.string.ui_offline_username)) },
                        isError = form.offlineUsername.isNotEmpty() &&
                            !form.offlineUsername.matches(Regex("^[A-Za-z0-9_]{1,16}$")),
                        supportingText = {
                            Text(
                                if (form.offlineUsername.isNotEmpty() &&
                                    !form.offlineUsername.matches(Regex("^[A-Za-z0-9_]{1,16}$"))
                                ) {
                                    stringResource(Res.string.ui_use_to_letters_numbers_or_underscores)
                                } else {
                                    stringResource(Res.string.ui_to_letters_numbers_or_underscores)
                                },
                            )
                        },
                        enabled = !form.isWaiting,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { if (form.canSubmit && !form.isWaiting) actions.signInAccount() },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(Res.string.ui_offline_accounts_prove_no_ownership_they_only),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                form.authorization?.let { authorization ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.ui_enter_this_code), color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                authorization.userCode,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(authorization.verificationUri, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                stringResource(Res.string.ui_code_expires_at, formatLocalDateTime(authorization.expiresAtEpochMillis)),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { uriHandler.openUri(authorization.directVerificationUri) },
                                ) { Text(stringResource(Res.string.ui_open_microsoft_sign_in)) }
                                TextButton(onClick = { copyText(authorization.userCode) }) { Text(stringResource(Res.string.ui_copy_code)) }
                            }
                        }
                    }
                }
            }
            form.error?.let { error ->
                Text(
                    error.resolve(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                        .testTag(LauncherTestTags.ACCOUNT_LOGIN_ERROR),
                )
            }
            TrestleDialogActions {
                TextButton(onClick = actions::closeAccountLogin) { Text(stringResource(Res.string.ui_cancel)) }
                if (form.authorization == null) {
                    Button(
                        onClick = actions::signInAccount,
                        enabled = !form.isWaiting && form.canSubmit,
                    ) {
                        Text(
                            if (form.isWaiting) {
                                stringResource(Res.string.ui_waiting)
                            } else {
                                when (form.method) {
                                    AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE,
                                    AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE,
                                    -> stringResource(Res.string.ui_get_sign_in_code)

                                    AccountAuthenticationMethod.MICROSOFT_CREDENTIALS,
                                    AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS,
                                    -> stringResource(Res.string.ui_sign_in)

                                    AccountAuthenticationMethod.OFFLINE -> stringResource(Res.string.ui_add_offline_account)

                                    else -> stringResource(Res.string.ui_import_account)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private val AccountAuthenticationMethod.requiresImportedSecret: Boolean
    get() = this == AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN ||
        this == AccountAuthenticationMethod.MICROSOFT_COOKIES ||
        this == AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN

private val AccountAuthenticationMethod.secretInputLabel: String
    @Composable get() = when (this) {
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN -> stringResource(Res.string.ui_microsoft_refresh_token)
        AccountAuthenticationMethod.MICROSOFT_COOKIES -> stringResource(Res.string.ui_login_live_com_cookies_or_cookie_export)
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> stringResource(Res.string.ui_minecraft_access_token)
        else -> stringResource(Res.string.ui_imported_secret)
    }

private val AccountAuthenticationMethod.importWarning: String
    @Composable get() = when (this) {
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN ->
            stringResource(Res.string.ui_use_a_refresh_token_issued_for_the)

        AccountAuthenticationMethod.MICROSOFT_COOKIES ->
            stringResource(Res.string.ui_cookies_grant_access_to_your_microsoft_session)

        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN ->
            stringResource(Res.string.ui_raw_minecraft_access_tokens_cannot_be_renewed)

        else -> ""
    }

private val AccountLoginState.canSubmit: Boolean
    get() = when (method) {
        AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE -> true

        AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE -> bedrockGameVersion.isNotBlank()

        AccountAuthenticationMethod.MICROSOFT_CREDENTIALS -> email.isNotBlank() && !password.isBlank()

        AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS ->
            bedrockGameVersion.isNotBlank() && email.isNotBlank() && !password.isBlank()

        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN,
        AccountAuthenticationMethod.MICROSOFT_COOKIES,
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN,
        -> !importedSecret.isBlank()

        AccountAuthenticationMethod.OFFLINE -> offlineUsername.matches(Regex("^[A-Za-z0-9_]{1,16}$"))
    }

private val AccountAuthenticationMethod.summary: String
    @Composable get() = when (this) {
        AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE -> stringResource(Res.string.ui_sign_in_with_a_code_in_your)
        AccountAuthenticationMethod.MICROSOFT_BEDROCK_DEVICE_CODE -> stringResource(Res.string.ui_sign_in_to_a_bedrock_edition_account)
        AccountAuthenticationMethod.MICROSOFT_CREDENTIALS -> stringResource(Res.string.ui_enter_a_microsoft_email_and_password_fails)
        AccountAuthenticationMethod.MICROSOFT_BEDROCK_CREDENTIALS -> stringResource(Res.string.ui_enter_a_microsoft_email_and_password_for)
        AccountAuthenticationMethod.MICROSOFT_REFRESH_TOKEN -> stringResource(Res.string.ui_import_a_session_from_another_launcher_with)
        AccountAuthenticationMethod.MICROSOFT_COOKIES -> stringResource(Res.string.ui_import_a_session_from_exported_microsoft_browser)
        AccountAuthenticationMethod.MICROSOFT_ACCESS_TOKEN -> stringResource(Res.string.ui_use_a_minecraft_access_token_directly_it)
        AccountAuthenticationMethod.OFFLINE -> stringResource(Res.string.ui_play_with_any_username_servers_in_online)
    }

/** Lists sign-in methods with what each one does, so the recommended choice is obvious. */
@Composable
private fun AccountMethodPicker(selected: AccountAuthenticationMethod, enabled: Boolean, onSelect: (AccountAuthenticationMethod) -> Unit) {
    var showAll by rememberSaveable { mutableStateOf(selected != AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE) }
    val primary = listOf(AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE, AccountAuthenticationMethod.OFFLINE)
    val methods = if (showAll) AccountAuthenticationMethod.entries else primary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        methods.forEach { method ->
            val isSelected = method == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.medium,
                    )
                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) { onSelect(method) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(selected = isSelected, onClick = null, enabled = enabled)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(method.labelResource), style = MaterialTheme.typography.titleSmall)
                        if (method == AccountAuthenticationMethod.MICROSOFT_DEVICE_CODE) {
                            StatusBadge(stringResource(Res.string.ui_recommended), MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Text(method.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!showAll) {
            TextButton(onClick = { showAll = true }, enabled = enabled) { Text(stringResource(Res.string.ui_more_sign_in_options)) }
        }
    }
}

/** Account actions that need the page's confirmation dialogs or navigation. */
private class AccountCallbacks(
    val onForget: () -> Unit,
    val onSignOut: () -> Unit,
    val onResetSkin: () -> Unit,
    val onSignInAgain: () -> Unit,
)

/** Signed-out online accounts can sign in again with the method they were added with. */
private val ManagedAccount.canSignInAgain: Boolean
    get() = !isAuthenticated && profile.authenticationMethod != AccountAuthenticationMethod.OFFLINE

@Composable
private fun accountStatusText(account: ManagedAccount): String = when {
    account.profile.authenticationMethod == AccountAuthenticationMethod.OFFLINE -> stringResource(Res.string.ui_offline_profile)
    !account.isAuthenticated -> stringResource(Res.string.ui_sign_in_required)
    account.profile.edition == MinecraftEdition.JAVA -> stringResource(Res.string.ui_ready_to_play)
    else -> stringResource(Res.string.ui_bedrock_can_t_launch_java)
}
