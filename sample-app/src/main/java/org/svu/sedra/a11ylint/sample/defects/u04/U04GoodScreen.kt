package org.svu.sedra.a11ylint.sample.defects.u04

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** U-04 good examples: every button names its action. U-04 must report nothing here. */
@Composable
fun U04GoodScreen(onConfirm: () -> Unit, onOpen: () -> Unit, onSend: () -> Unit) {
    Column {
        // The label names the action.
        Button(onClick = onConfirm) {
            Text("Delete draft")
        }

        // A vague word is fine inside a label that says what happens.
        TextButton(onClick = onOpen) {
            Text("Read more about shipping")
        }

        // The visible label stays short, but screen readers get the full action.
        OutlinedButton(
            onClick = onSend,
            modifier = Modifier.semantics { contentDescription = "Send the order" },
        ) {
            Text("Submit")
        }
    }
}
