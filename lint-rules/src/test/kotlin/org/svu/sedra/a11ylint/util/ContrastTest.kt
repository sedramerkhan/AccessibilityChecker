package org.svu.sedra.a11ylint.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ContrastTest {
    @Test
    fun blackOnWhiteIsTwentyOneToOne() {
        assertEquals(21.0, Contrast.ratio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
    }

    @Test
    fun lightGrayOnWhiteIsAboutTwoPointOneSevenToOne() {
        assertEquals(2.17, Contrast.ratio(0xFFB0B0B0.toInt(), 0xFFFFFFFF.toInt()), 0.01)
    }

    @Test
    fun grayOnWhiteJustPassesTheNormalTextThreshold() {
        assertEquals(4.54, Contrast.ratio(0xFF767676.toInt(), 0xFFFFFFFF.toInt()), 0.01)
    }

    @Test
    fun ratioDoesNotDependOnArgumentOrder() {
        val first = 0xFF336699.toInt()
        val second = 0xFFFFCC00.toInt()
        assertEquals(Contrast.ratio(first, second), Contrast.ratio(second, first), 0.0)
    }

    @Test
    fun alphaChannelIsIgnored() {
        assertEquals(21.0, Contrast.ratio(0x80000000.toInt(), 0x00FFFFFF), 0.01)
    }
}
