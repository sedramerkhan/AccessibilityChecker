package org.svu.sedra.a11ylint.sample.defects.p03

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * P-03 bad examples: literal text colors with too little contrast against a literal background.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun P03BadScreen() {
    Column {
        // Light gray text on a white Surface: about 2.2:1, well below the 4.5:1 minimum.
        Surface(color = Color(0xFFFFFFFF)) {
            Text("Delete draft", color = Color(0xFFB0B0B0)) // EXPECT: ComposeLowContrastColors
        }

        // The same low-contrast pair, set through the Text's own background modifier.
        Text(
            "Delete draft",
            color = Color(0xFFB0B0B0), // EXPECT: ComposeLowContrastColors
            modifier = Modifier.background(Color(0xFFFFFFFF)),
        )

        // The same pair again, with the background on the enclosing Box.
        Box(modifier = Modifier.background(Color(0xFFFFFFFF))) {
            Text("Delete draft", color = Color(0xFFB0B0B0)) // EXPECT: ComposeLowContrastColors
        }

        // Large text still needs 3:1. This pair is far below even that.
        Surface(color = Color(0xFFFFFFFF)) {
            Text("Delete draft", color = Color(0xFFB0B0B0), fontSize = 24.sp) // EXPECT: ComposeLowContrastColors
        }
    }
}
