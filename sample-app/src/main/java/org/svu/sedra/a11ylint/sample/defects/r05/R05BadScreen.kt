package org.svu.sedra.a11ylint.sample.defects.r05

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * R-05 bad examples: progress drawn by hand, with nothing telling accessibility services what
 * the value is. Every reported line carries an EXPECT marker.
 */

// A bar whose width is the only sign of how far along the download is.
@Composable
fun R05BadProgressBar(progress: Float) { // EXPECT: ComposeMissingProgressRange
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        drawRect(
            color = Color(0xFF1565C0),
            size = Size(width = size.width * progress, height = size.height),
        )
    }
}

// The same defect drawn with drawBehind instead of Canvas.
@Composable
fun R05BadProgressRing(fraction: Float) { // EXPECT: ComposeMissingProgressRange
    Box(
        modifier = Modifier
            .size(48.dp)
            .drawBehind {
                drawRect(
                    color = Color(0xFF1565C0),
                    size = Size(width = size.width * fraction, height = size.height),
                )
            },
    )
}
