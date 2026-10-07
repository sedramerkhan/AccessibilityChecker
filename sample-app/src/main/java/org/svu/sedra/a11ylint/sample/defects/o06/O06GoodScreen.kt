package org.svu.sedra.a11ylint.sample.defects.o06

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * O-06 good examples: the enabled state is passed to the component, so accessibility services
 * are told about it too. O-06 must report nothing here.
 */
@Composable
fun O06GoodScreen(formComplete: Boolean, onSubmit: () -> Unit) {
    Column {
        // clickable both blocks the click and marks the element disabled.
        Row(
            modifier = Modifier.clickable(
                enabled = formComplete,
                onClickLabel = "Send",
                role = Role.Button,
                onClick = onSubmit
            )
        ) {
            Text("Send")
        }

        // A Material button takes the same parameter and handles the rest.
        Button(onClick = onSubmit, enabled = formComplete) {
            Text("Save draft")
        }
    }
}
