package org.monogram.core.ui.glass

import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp

/**
 * Blurs what is behind the current window.
 *
 * Glass cannot cross a window boundary: a `Popup` gets its own surface, so its backdrop can never be
 * the layer the app records behind it. The platform can still blur that window's background, which
 * is why overflow menus and context menus on Android 12+ look like frosted glass without sampling
 * anything.
 */
@Composable
fun BlurBehindWindow(radius: Dp) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BlurBehindWindowS(radius)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun BlurBehindWindowS(radius: Dp) {
    val view = LocalView.current
    val radiusPx = with(LocalDensity.current) { radius.roundToPx() }
    DisposableEffect(view, radiusPx) {
        val root = view.rootView
        val params = root.layoutParams as? WindowManager.LayoutParams
        if (params == null) {
            return@DisposableEffect onDispose {}
        }
        val previousFlags = params.flags
        val previousRadius = params.blurBehindRadius
        runCatching {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            params.blurBehindRadius = radiusPx
            root.context.getSystemService(WindowManager::class.java)?.updateViewLayout(root, params)
        }
        onDispose {
            runCatching {
                params.flags = previousFlags
                params.blurBehindRadius = previousRadius
                root.context.getSystemService(WindowManager::class.java)
                    ?.updateViewLayout(root, params)
            }
        }
    }
}

/**
 * False below Android 12. A ROM that disables cross-window blur makes the platform skip the effect
 * silently, leaving the surface a little translucent rather than wrong.
 */
fun isWindowBlurEnabled(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
