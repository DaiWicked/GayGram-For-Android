package org.monogram.feature.settings.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import org.monogram.core.models.DeviceSession
import org.monogram.feature.settings.R

/**
 * MTProto reports the platform as free text ("Android 17", "Desktop Windows 11 x64"), so the
 * session kind is matched on those strings instead of a server-provided enum.
 */
internal enum class DevicePlatform {
    Android,
    Apple,
    Chrome,
    Windows,
    Linux,
    Browser,
    Unknown,
}

internal val DeviceSession.platformKind: DevicePlatform
    get() {
        val model = deviceModel.lowercase()
        val system = systemVersion.lowercase()
        val platform = platform.lowercase()
        return when {
            "chrome" in platform || "chrome" in model -> DevicePlatform.Chrome
            "oculus" in model || "android" in platform || "android" in system ->
                DevicePlatform.Android

            "iphone" in platform ||
                    "ipad" in platform ||
                    "ios" in platform ||
                    "macos" in platform ||
                    "mac os" in platform ||
                    "macos" in system -> DevicePlatform.Apple

            "windows" in platform || "windows" in system -> DevicePlatform.Windows
            "ubuntu" in platform || "linux" in platform || "linux" in system ->
                DevicePlatform.Linux

            "firefox" in platform ||
                    "safari" in platform ||
                    "edge" in platform ||
                    "opera" in platform ||
                    "vivaldi" in platform ||
                    "brave" in platform -> DevicePlatform.Browser

            else -> DevicePlatform.Unknown
        }
    }

internal fun DeviceSession.platformColor(): Color = when (platformKind) {
    DevicePlatform.Android -> Color(0xFF34A853)
    DevicePlatform.Apple -> Color(0xFF1C1C1E)
    DevicePlatform.Chrome -> Color(0xFF4285F4)
    DevicePlatform.Windows -> Color(0xFF0078D7)
    DevicePlatform.Linux -> Color(0xFFFCC624)
    DevicePlatform.Browser -> Color(0xFF4285F4)
    DevicePlatform.Unknown -> Color(0xFF6B7280)
}

/** A pending login is what the user has to act on, so it overrides the platform glyph. */
@Composable
internal fun DeviceSession.platformIcon(): ImageVector = when {
    passwordPending -> Icons.Rounded.Warning
    else -> when (platformKind) {
        DevicePlatform.Android -> Icons.Rounded.Android
        DevicePlatform.Apple -> ImageVector.vectorResource(R.drawable.ic_platform_apple)
        DevicePlatform.Chrome -> ImageVector.vectorResource(R.drawable.ic_platform_chrome)
        DevicePlatform.Windows -> ImageVector.vectorResource(R.drawable.ic_platform_windows)
        DevicePlatform.Linux -> Icons.Rounded.Terminal
        DevicePlatform.Browser -> Icons.Rounded.Language
        DevicePlatform.Unknown -> Icons.Rounded.Laptop
    }
}

/**
 * Multi-color logos keep their own palette, single-color glyphs are tinted with the brand color.
 * The pending warning follows the surface instead so it stays readable on its container.
 */
@Composable
internal fun DeviceSession.platformIconTint(brand: Color): Color = when {
    passwordPending -> MaterialTheme.colorScheme.onSurface
    platformKind == DevicePlatform.Chrome -> Color.Unspecified
    else -> brand
}
