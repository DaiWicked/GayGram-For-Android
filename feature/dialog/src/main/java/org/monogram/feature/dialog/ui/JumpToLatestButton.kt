package org.monogram.feature.dialog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import org.monogram.core.ui.glass.GlassTokens
import org.monogram.core.ui.glass.LiquidGlassSurface

/**
 * The round "jump to newest" control. It floats over the message list, so it is the app's first
 * glass surface with something worth sampling behind it.
 */
@Composable
internal fun JumpToLatestButton(
    backdrop: Backdrop,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    LiquidGlassSurface(
        backdrop = backdrop,
        shape = shape,
        surface = GlassTokens.Button,
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
