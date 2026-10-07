package org.svu.sedra.a11ylint.sample.defects.o02

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * O-02 good examples: a clickable container with an onClickLabel describing the action, or a
 * Material button that already announces its role. O-02 must report nothing here.
 */
@Composable
fun O02GoodScreen(onAction: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.clickable(onClickLabel = "Open details", role = Role.Button) { onAction() }, // EXPECT: ComposeHardcodedA11yText
        ) {
            Text("Open")
        }

        Box(
            modifier = Modifier.clickable(onClickLabel = "Open details", role = Role.Button) { onAction() }, // EXPECT: ComposeHardcodedA11yText
        ) {
            Text("Open")
        }

        Card(
            modifier = Modifier.clickable(onClickLabel = "Open details", role = Role.Button) { onAction() }, // EXPECT: ComposeHardcodedA11yText
        ) {
            Text("Open")
        }

        // A Material button already announces its own role; no Modifier.clickable involved.
        Button(onClick = onAction) {
            Text("Open")
        }
    }
}
