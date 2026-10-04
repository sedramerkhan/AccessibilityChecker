package org.svu.sedra.a11ylint.sample.defects.p03

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/** P-03 good examples: enough contrast, or a theme color already checked. P-03 must report nothing here. */
@Composable
fun P03GoodScreen() {
    Column {
        // Black on white is 21:1.
        Surface(color = Color(0xFFFFFFFF)) {
            Text("Delete draft", color = Color(0xFF000000))
        }

        // Large text only needs 3:1, and this pair clears it.
        Surface(color = Color(0xFFFFFFFF)) {
            Text("Delete draft", color = Color(0xFF898989), fontSize = 24.sp)
        }

        // A theme color is assumed already checked for contrast.
        Surface(color = MaterialTheme.colorScheme.surface) {
            Text("Delete draft", color = MaterialTheme.colorScheme.onSurface)
        }

        // Same background modifier pattern as the bad screen, but with enough contrast.
        Box(modifier = Modifier.background(Color(0xFFFFFFFF))) {
            Text("Delete draft", color = Color(0xFF000000))
        }
    }
}
