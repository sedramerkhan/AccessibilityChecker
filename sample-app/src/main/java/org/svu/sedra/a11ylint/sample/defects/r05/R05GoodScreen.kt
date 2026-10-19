package org.svu.sedra.a11ylint.sample.defects.r05

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * R-05 good examples: the range is set by hand, by the `progressSemantics` shortcut, or by a
 * Material indicator; or the drawing is not progress at all. R-05 must report nothing here.
 */

// The drawing is the same, but the value is now announced and announced again when it changes.
@Composable
fun R05GoodProgressBar(progress: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(current = progress, range = 0f..1f)
            },
    ) {
        drawRect(
            color = Color(0xFF1565C0),
            size = Size(width = size.width * progress, height = size.height),
        )
    }
}

// The Material indicator sets the range itself, so nothing has to be written out.
@Composable
fun R05GoodMaterialProgress(progress: Float) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
    )
}

// The foundation shortcut sets the same progressBarRangeInfo in one call.
@Composable
fun R05GoodProgressSemantics(progress: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .progressSemantics(progress),
    ) {
        drawRect(
            color = Color(0xFF1565C0),
            size = Size(width = size.width * progress, height = size.height),
        )
    }
}

// `value` inside a longer name is not progress: this is a fade, not a bar.
@Composable
fun R05GoodFadingBand(alphaValue: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        drawRect(color = Color(0xFF1565C0).copy(alpha = alphaValue))
    }
}
