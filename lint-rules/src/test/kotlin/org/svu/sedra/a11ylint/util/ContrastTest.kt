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
}
