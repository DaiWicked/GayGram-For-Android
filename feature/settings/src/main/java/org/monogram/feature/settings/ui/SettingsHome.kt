package org.monogram.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.monogram.core.common.CountryManager
import org.monogram.core.ui.components.ItemPosition
import org.monogram.core.ui.components.PeerAvatar
import org.monogram.core.ui.components.SectionHeader
import org.monogram.core.ui.components.SettingsTile
import org.monogram.feature.settings.R
import org.monogram.feature.settings.SettingsComponent
import org.monogram.feature.settings.SettingsPage
import java.io.File

private const val GITHUB_URL = "https://github.com/DaiWicked/GayGram-For-Android"

internal fun LazyListScope.homeItems(
    component: SettingsComponent,
    stateTitle: String,
    stateSubtitle: String,
    avatarFile: File?,
    avatarPreview: ByteArray? = null,
    appVersion: String,
    buildStamp: String,
    onOpen: (SettingsPage) -> Unit,
    onLogout: () -> Unit,
    loading: Boolean,
    devicesSubtitle: Int? = null,
    apiLayer: Int = 0,
) {
    item {
        AccountHeader(
            title = stateTitle,
            subtitle = stateSubtitle,
            avatarFile = avatarFile,
            previewBytes = avatarPreview,
            onClick = component::onOpenProfile,
        )
    }
    item { SectionHeader(stringResource(R.string.settings_section_settings)) }
    item {
        SettingsTile(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.settings_chat),
            subtitle = stringResource(R.string.settings_chat_sub),
            iconColor = MaterialTheme.colorScheme.primary,
            position = ItemPosition.TOP,
            onClick = { onOpen(SettingsPage.Appearance) },
            trailingContent = { Chevron() },
        )
    }
    item {
        SettingsTile(
            icon = Icons.Outlined.Folder,
            title = stringResource(R.string.settings_folders),
            subtitle = stringResource(R.string.settings_folders_sub),
            iconColor = MaterialTheme.colorScheme.tertiary,
            position = ItemPosition.MIDDLE,
            onClick = { onOpen(SettingsPage.Folders) },
            trailingContent = { Chevron() },
        )
    }
    item {
        SettingsTile(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.settings_notifications),
            subtitle = stringResource(R.string.settings_notifications_sub),
            iconColor = MaterialTheme.colorScheme.secondary,
            position = ItemPosition.MIDDLE,
            onClick = { onOpen(SettingsPage.Notifications) },
            trailingContent = { Chevron() },
        )
    }
    item {
        SettingsTile(
            icon = Icons.Outlined.VpnKey,
            title = stringResource(R.string.settings_proxy),
            subtitle = stringResource(R.string.settings_proxy_sub),
            iconColor = MaterialTheme.colorScheme.primary,
            position = ItemPosition.MIDDLE,
            onClick = { onOpen(SettingsPage.Proxy) },
            trailingContent = { Chevron() },
        )
    }
    item {
        SettingsTile(
            icon = Icons.Outlined.Devices,
            title = stringResource(R.string.settings_devices),
            subtitle = devicesSubtitle?.let { count ->
                pluralStringResource(R.plurals.settings_devices_active_count, count, count)
            } ?: stringResource(R.string.settings_devices_sub),
            iconColor = MaterialTheme.colorScheme.secondary,
            position = ItemPosition.MIDDLE,
            onClick = { onOpen(SettingsPage.Devices) },
            trailingContent = { Chevron() },
        )
    }
    item {
        SettingsTile(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.settings_data),
            subtitle = stringResource(R.string.settings_data_sub),
            iconColor = MaterialTheme.colorScheme.tertiary,
            position = ItemPosition.BOTTOM,
            onClick = { onOpen(SettingsPage.Data) },
            trailingContent = { Chevron() },
        )
    }
    item { SectionHeader(stringResource(R.string.settings_section_help)) }
    item {
        val uri = LocalUriHandler.current
        SettingsTile(
            icon = Icons.Outlined.Code,
            title = stringResource(R.string.settings_github),
            subtitle = stringResource(R.string.settings_github_sub),
            iconColor = MaterialTheme.colorScheme.tertiary,
            position = ItemPosition.TOP,
            onClick = { uri.openUri(GITHUB_URL) },
            trailingContent = { Chevron() },
        )
    }
    item {
        val uri = LocalUriHandler.current
        SettingsTile(
            icon = Icons.AutoMirrored.Outlined.Help,
            title = stringResource(R.string.settings_faq),
            subtitle = stringResource(R.string.settings_faq_sub),
            iconColor = MaterialTheme.colorScheme.secondary,
            position = ItemPosition.MIDDLE,
            onClick = { uri.openUri("https://telegram.org/faq") },
            trailingContent = { Chevron() },
        )
    }
    item {
        val uri = LocalUriHandler.current
        SettingsTile(
            icon = Icons.AutoMirrored.Outlined.Chat,
            title = stringResource(R.string.settings_ask),
            subtitle = stringResource(R.string.settings_ask_sub),
            iconColor = MaterialTheme.colorScheme.primary,
            position = ItemPosition.BOTTOM,
            onClick = { uri.openUri("https://telegram.org/support") },
            trailingContent = { Chevron() },
        )
    }
    item { SectionHeader(stringResource(R.string.settings_section_session)) }
    item {
        SettingsTile(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = stringResource(R.string.settings_logout),
            iconColor = MaterialTheme.colorScheme.error,
            titleColor = MaterialTheme.colorScheme.error,
            position = ItemPosition.STANDALONE,
            enabled = !loading,
            onClick = onLogout,
        )
    }
    item {
        val about = buildString {
            append(stringResource(R.string.settings_about))
            append(' ')
            append(appVersion)
            append('\n')
            append(buildStamp)
            if (apiLayer > 0) {
                append('\n')
                append(stringResource(R.string.settings_api_layer, apiLayer))
            }
        }
        Text(
            text = about,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AccountHeader(
    title: String,
    subtitle: String,
    avatarFile: File?,
    previewBytes: ByteArray?,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            supportingContent = subtitle.takeIf { it.isNotBlank() }?.let {
                {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            leadingContent = {
                PeerAvatar(
                    title = title,
                    size = 64.dp,
                    imageFile = avatarFile,
                    previewBytes = previewBytes,
                )
            },
            trailingContent = { Chevron() },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized)
        }
    }
}

// The signed-in account has no presence to show: Telegram reports its own status too.
@Composable
internal fun profileSubtitle(username: String?, phone: String?, loaded: Boolean): String {
    val handle = username?.takeIf { it.isNotBlank() }?.let { "@$it" }
    val number = phone?.takeIf { it.isNotBlank() }?.let(::displayPhone)
    return when {
        handle != null && number != null -> "$handle\n$number"
        handle != null -> handle
        number != null -> number
        loaded -> ""
        else -> stringResource(R.string.settings_account_loading)
    }
}

private fun displayPhone(raw: String): String {
    val trimmed = raw.trim()
    val international = runCatching { CountryManager.formatPhoneNumber(trimmed) }.getOrNull()
        ?.takeIf { it.isNotBlank() && it != trimmed }
    if (international != null) return international
    val withPlus = if (trimmed.startsWith("+")) trimmed else "+${trimmed.filter { it.isDigit() }}"
    return runCatching { CountryManager.formatPhoneNumber(withPlus) }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: withPlus
}
