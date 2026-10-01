package org.svu.sedra.a11ylint.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Calculates WCAG contrast ratios for opaque or alpha-packed ARGB colors. */
object Contrast {
    /** Returns the WCAG relative luminance of an ARGB color. */
    fun relativeLuminance(color: Int): Double {
        val red = channel(color shr 16)
        val green = channel(color shr 8)
        val blue = channel(color)
        return 0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
    }

    /** Returns the WCAG contrast ratio between two ARGB colors. */
    fun ratio(first: Int, second: Int): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        val lighter = max(firstLuminance, secondLuminance)
        val darker = min(firstLuminance, secondLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun channel(value: Int): Double = (value and 0xFF) / 255.0

    private fun linearize(value: Double): Double =
        if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}
