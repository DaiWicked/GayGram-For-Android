package org.monogram.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.monogram.R
import org.monogram.core.ui.GayGramSettings
import org.monogram.core.ui.LocalBottomBarInset
import org.monogram.core.ui.components.ItemPosition
import org.monogram.core.ui.components.SectionHeader
import org.monogram.core.ui.components.SettingsCard
import org.monogram.core.ui.components.SettingsCardRow

/** The GayGram switches. One row per shipped feature; nothing is listed before it works. */
@Composable
internal fun ExtensionsContent(modifier: Modifier = Modifier) {
    val settings by GayGramSettings.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .statusBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.tab_more),
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = LocalBottomBarInset.current + 24.dp,
            ),
        ) {
            extensionItems(
                keepRevoked = settings.keepRevokedMessages,
                onKeepRevoked = GayGramSettings::setKeepRevokedMessages,
            )
            item {
                Text(
                    text = stringResource(R.string.extensions_coming_soon),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
        }
    }
}

private fun LazyListScope.extensionItems(
    keepRevoked: Boolean,
    onKeepRevoked: (Boolean) -> Unit,
) {
    item { SectionHeader(stringResource(R.string.extensions_section_chat)) }
    item {
        SettingsCard(position = ItemPosition.STANDALONE) {
            SettingsCardRow(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.extensions_keep_revoked),
                subtitle = stringResource(R.string.extensions_keep_revoked_sub),
                iconColor = MaterialTheme.colorScheme.primary,
                toggle = keepRevoked,
                onToggle = onKeepRevoked,
                trailingContent = {
                    Switch(checked = keepRevoked, onCheckedChange = null)
                },
            )
        }
    }
}
