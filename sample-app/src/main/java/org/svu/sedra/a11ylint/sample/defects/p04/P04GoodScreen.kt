package org.svu.sedra.a11ylint.sample.defects.p04

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** P-04 good examples: a literal sp size, or a typography style. P-04 must report nothing here. */
@Composable
fun P04GoodScreen() {
    Column {
        // A literal sp value scales correctly.
        Text("Caption", fontSize = 16.sp)

        // A typography style already scales correctly.
        Text("Caption", style = MaterialTheme.typography.bodyLarge)

        // A literal sp value set through a TextStyle.
        Text("Caption", style = TextStyle(fontSize = 16.sp))
    }
}
