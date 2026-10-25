package org.monogram.core.ui.glass

import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported

/**
 * Which liquid glass implementation a device can run.
 *
 * [Refraction] needs [android.graphics.RuntimeShader] (API 33), [Blur] needs
 * [android.graphics.RenderEffect] (API 31), [Tint] is the translucent fallback down to minSdk 24.
 */
enum class GlassTier {
    Refraction,
    Blur,
    Tint,

    /** User turned glass off. Renders the plain opaque surface this app had before glass existed. */
    Off,
}

object PlatformCapabilities {
    /**
     * Both come from backdrop, which marks them `@ChecksSdkIntAtLeast`; routing through them keeps
     * lint's NewApi check able to see the guard instead of trusting a hand-written comparison.
     */
    val supportsRuntimeShader: Boolean = isRuntimeShaderSupported()

    val supportsRenderEffect: Boolean = isRenderEffectSupported()

    /** What the hardware allows, ignoring user preference. Fixed for the process. */
    val supportedTier: GlassTier = when {
        supportsRuntimeShader -> GlassTier.Refraction
        supportsRenderEffect -> GlassTier.Blur
        else -> GlassTier.Tint
    }

    fun tier(glassEnabled: Boolean): GlassTier = if (glassEnabled) supportedTier else GlassTier.Off
}
