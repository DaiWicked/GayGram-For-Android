package org.monogram.core.ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One glass surface: the effect chain drawn behind it, in backdrop's own vocabulary.
 *
 * Not a physics model. These are the combinations the reference catalog already ships and tunes,
 * per component class, so a new glass surface picks an existing one instead of inventing numbers.
 */
@Immutable
data class GlassSurface(
    val blurRadius: Dp,
    /** Light surfaces can afford a wider blur than dark ones. Same value unless a surface says otherwise. */
    val darkBlurRadius: Dp = blurRadius,
    /** Lens edge thickness. Zero disables refraction entirely. */
    val refractionHeight: Dp,
    /** How far the lens pushes the backdrop at the edge. */
    val refractionAmount: Dp,
    val depthEffect: Boolean = false,
) {
    fun blurRadius(dark: Boolean): Dp = if (dark) darkBlurRadius else blurRadius
}

object GlassTokens {
    /** Blur only, no lens. For content that must stay legible through the surface. */
    val Thin = GlassSurface(
        blurRadius = 2.dp,
        refractionHeight = 0.dp,
        refractionAmount = 0.dp,
    )

    /** Small interactive surfaces: composer bar, buttons. */
    val Button = GlassSurface(
        blurRadius = 2.dp,
        refractionHeight = 12.dp,
        refractionAmount = 24.dp,
    )

    /** Large flat panels: settings page background, menus. */
    val Panel = GlassSurface(
        blurRadius = 4.dp,
        refractionHeight = 16.dp,
        refractionAmount = 32.dp,
    )

    /** App bars: chat list top bar, dialog top bar. */
    val TabBar = GlassSurface(
        blurRadius = 8.dp,
        refractionHeight = 24.dp,
        refractionAmount = 24.dp,
    )

    /** Sheets and dialogs, floating over busy content, so the heaviest lens and a depth pass. */
    val Dialog = GlassSurface(
        blurRadius = 16.dp,
        darkBlurRadius = 8.dp,
        refractionHeight = 24.dp,
        refractionAmount = 48.dp,
        depthEffect = true,
    )

    // A tint absorbs part of the backdrop, so it has to absorb more of a bright one: the same alpha
    // that stays readable over dark content washes a light surface out completely.
    private const val LightTintAlpha = 0.22f
    private const val DarkTintAlpha = 0.44f

    /** Alpha of the tint layer drawn over a live glass surface. */
    fun tintAlpha(dark: Boolean): Float = if (dark) DarkTintAlpha else LightTintAlpha

    /**
     * Alpha of the replacement surface when the platform cannot draw glass at all. This is not glass:
     * it has to stay opaque enough for the text on it to survive without any blur underneath.
     */
    const val FallbackAlpha = 0.85f
}
