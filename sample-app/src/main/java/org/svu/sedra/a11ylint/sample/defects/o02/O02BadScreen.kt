package org.svu.sedra.a11ylint.sample.defects.o02

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * O-02 bad examples: a clickable container with no onClickLabel, so screen readers only
 * announce "double tap to activate" with nothing describing the action. Every reported line
 * carries an EXPECT marker. Each clickable also sets a role so only O-02 is reported here.
 */
@Composable
fun O02BadScreen(onAction: () -> Unit) {
    Column {
        Row(modifier = Modifier.clickable(role = Role.Button) { onAction() }) { // EXPECT: ComposeMissingOnClickLabel
            Text("Open")
        }

        Box(modifier = Modifier.clickable(role = Role.Button) { onAction() }) { // EXPECT: ComposeMissingOnClickLabel
            Text("Open")
        }

        Card(modifier = Modifier.clickable(role = Role.Button) { onAction() }) { // EXPECT: ComposeMissingOnClickLabel
            Text("Open")
        }

        ListItem(
            headlineContent = { Text("Open") },
            modifier = Modifier.clickable(role = Role.Button) { onAction() }, // EXPECT: ComposeMissingOnClickLabel
        )

        // An explicit null is the same as leaving it out.
        Row(
            modifier = Modifier.clickable( // EXPECT: ComposeMissingOnClickLabel
                onClickLabel = null,
                role = Role.Button,
                onClick = onAction,
            ),
        ) {
            Text("Open")
        }
    }
}
