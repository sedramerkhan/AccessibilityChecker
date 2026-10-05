package org.svu.sedra.a11ylint.sample.defects.r02

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription

/**
 * R-02 bad examples: clearAndSetSemantics that throws the content's semantics away without
 * putting them back. Every reported line carries an EXPECT marker.
 */
@Composable
fun R02BadScreen(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onOpen: () -> Unit) {
    Column {
        // An empty block hides the row completely.
        Row(modifier = Modifier.clearAndSetSemantics { }) { // EXPECT: ComposeClearAndSetSemanticsLoss
            Text("Running shoes")
            Text("42 euro")
        }

        // The state is replaced, but the name is gone.
        Row(
            modifier = Modifier.clearAndSetSemantics { // EXPECT: ComposeClearAndSetSemanticsLoss
                stateDescription = "In stock"
            },
        ) {
            Text("Running shoes")
        }

        // The name is replaced, but the row is still clickable and no longer says so.
        Row(
            modifier = Modifier
                .clickable(onClickLabel = "Open", role = Role.Button) { onOpen() }
                .clearAndSetSemantics { // EXPECT: ComposeClearAndSetSemanticsLoss
                    contentDescription = "Running shoes, 42 euro"
                },
        ) {
            Text("Running shoes")
        }

        // The checkbox state disappears behind a name-only replacement.
        Row(
            modifier = Modifier.clearAndSetSemantics { // EXPECT: ComposeClearAndSetSemanticsLoss
                contentDescription = "Subscribe to the newsletter"
            },
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text("Subscribe to the newsletter")
        }
    }
}
