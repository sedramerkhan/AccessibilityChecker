package org.svu.sedra.a11ylint.sample.defects.o06

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * O-06 bad examples: the click handler decides for itself whether it is enabled, so the element
 * still looks enabled to accessibility services. Every reported line carries an EXPECT marker.
 */
@Composable
fun O06BadScreen(formComplete: Boolean, onSubmit: () -> Unit) {
    Column {
        // The guard stops the action, but TalkBack still offers the row.
        Row(
            modifier = Modifier.clickable(onClickLabel = "Send", role = Role.Button) { // EXPECT: ComposeDisabledButClickable, ComposeHardcodedA11yText
                if (!formComplete) return@clickable
                onSubmit()
            }
        ) {
            Text("Send")
        }

        // The same thing written as a wrapper around the whole handler.
        Row(
            modifier = Modifier.clickable(onClickLabel = "Save draft", role = Role.Button) { // EXPECT: ComposeDisabledButClickable, ComposeHardcodedA11yText
                if (formComplete) {
                    onSubmit()
                }
            }
        ) {
            Text("Save draft")
        }
    }
}
