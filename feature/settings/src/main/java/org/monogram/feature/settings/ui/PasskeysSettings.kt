package org.monogram.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import org.monogram.core.ui.auth.PasskeyCredentials
import org.monogram.core.ui.loading.MonogramLinearProgress
import org.monogram.feature.settings.PasskeysStore
import org.monogram.feature.settings.R
import org.monogram.feature.settings.SettingsComponent

@Composable
internal fun PasskeysSettings(component: SettingsComponent, modifier: Modifier) {
    val state by component.passkeys.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(component) { component.onPasskeys(PasskeysStore.Intent.Refresh) }
    LaunchedEffect(state.options) {
        val options = state.options ?: return@LaunchedEffect
        try {
            component.onPasskeys(PasskeysStore.Intent.Created(PasskeyCredentials.request(context, options, true)))
        } catch (e: CancellationException) {
            component.onPasskeys(PasskeysStore.Intent.Created(null))
            throw e
        } catch (_: androidx.credentials.exceptions.CreateCredentialCancellationException) {
            component.onPasskeys(PasskeysStore.Intent.Created(null))
        } catch (_: PasskeyCredentials.ClientDataMismatchException) {
            component.onPasskeys(PasskeysStore.Intent.Created(null, clientDataMismatch = true))
        } catch (_: Exception) {
            component.onPasskeys(PasskeysStore.Intent.Created(null, failed = true))
        }
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "introduction") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(80.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_passkeys_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = { component.onPasskeys(PasskeysStore.Intent.Create) },
                    enabled = !state.loading && PasskeyCredentials.supported,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_passkeys_create))
                }
            }
        }
        if (state.loading) item(key = "loading") {
            MonogramLinearProgress(modifier = Modifier.fillMaxWidth())
        }
        if (!PasskeyCredentials.supported || state.providerFailed || state.clientDataMismatch) item(key = "provider") {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    stringResource(
                        if (state.clientDataMismatch) org.monogram.core.ui.R.string.passkey_client_data_mismatch
                        else if (!PasskeyCredentials.supported) R.string.settings_passkeys_unsupported
                        else R.string.settings_passkeys_provider_error,
                    ),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.error?.let { error ->
            item(key = "error") {
                Box(Modifier.clip(MaterialTheme.shapes.large)) {
                    org.monogram.core.ui.components.AppStatusBanner(
                        sync = org.monogram.core.ui.components.AppSyncStatus.Hidden,
                        error = error,
                        onRetry = { component.onPasskeys(PasskeysStore.Intent.Refresh) },
                    )
                }
            }
        }
        if (!state.loading && state.keys.isEmpty() && state.error == null && !state.providerFailed && !state.clientDataMismatch) {
            item(key = "empty") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.settings_passkeys_empty),
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (state.keys.isNotEmpty()) item(key = "keys-heading") {
            Text(
                stringResource(R.string.settings_passkeys),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(state.keys, key = { it.id }) { key ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                ListItem(
                    headlineContent = { Text(key.name) },
                    leadingContent = { Icon(Icons.Outlined.VpnKey, contentDescription = null) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    trailingContent = {
                        TextButton(
                            onClick = { component.onPasskeys(PasskeysStore.Intent.Delete(key.id)) },
                            enabled = !state.loading,
                        ) { Text(stringResource(R.string.settings_passkeys_delete)) }
                    },
                )
            }
        }
    }
}
