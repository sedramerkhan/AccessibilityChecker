package org.svu.sedra.a11ylint.sample.defects.o04

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * O-04 good examples: a real Button, a container that says it is a button, and a container
 * with rich content that is not a button. O-04 must report nothing here.
 */
@Composable
fun O04GoodScreen(onAction: () -> Unit) {
    Column {
        // The fix the rule asks for.
        Button(onClick = onAction) {
            Text("Buy now")
        }

        // A container that keeps its layout but says what it is.
        Row(modifier = Modifier.clickable(onClickLabel = "Buy now", role = Role.Button) { onAction() }) {
            Text("Buy now")
        }

        // Rich content is a list row, not a button, and it says so.
        Row(modifier = Modifier.clickable(onClickLabel = "Open chat", role = Role.Tab) { onAction() }) {
            Text("Sedra Merkhan")
            Text("Last seen today")
        }
    }
}
