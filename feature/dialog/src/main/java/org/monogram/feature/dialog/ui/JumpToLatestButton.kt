package org.monogram.feature.dialog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * The "jump to newest" control. It floats over the message list, so it is the app's first glass
 * surface with something worth sampling behind it.
 *
 * A pill rather than a circle: at 40dp a circle has too little edge for the refraction to read, and
 * folding the unread counter inside gives the shape enough width to be legible as glass.
 */
@Composable
internal fun JumpToLatestButton(
    backdrop: Backdrop,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int = 0,
) {
    val shape = RoundedCornerShape(percent = 50)
    LiquidGlassSurface(
        backdrop = backdrop,
        shape = shape,
        surface = GlassTokens.Button,
        tint = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .height(40.dp)
            .widthIn(min = 40.dp)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (count > 0) 14.dp else 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            if (count > 0) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
