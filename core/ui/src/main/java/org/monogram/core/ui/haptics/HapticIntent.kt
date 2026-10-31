package org.monogram.core.ui.haptics

/**
 * The vocabulary of touch feedback this app speaks.
 *
 * Every intent must have a visible counterpart at its call site: a haptic on its own is noise,
 * not feedback. The intent also fixes the strength, so the same gesture always feels the same
 * wherever it appears.
 */
enum class HapticIntent {
    /** Light confirmation: switching a folder, picking a chip, flipping a setting. */
    Tick,

    /** The list is already at its end and the finger keeps pushing past it. */
    Edge,

    ToggleOn,
    ToggleOff,

    /** The finger went down on something that reacts. */
    Press,

    LongPress,

    /** A swipe crossed the point where letting go commits the action. */
    Threshold,

    Confirm,
    Reject,

    /** Reordering or dragging a handle. */
    HandleMove,

    /** A fling settled. */
    ScrollEnd,
}
