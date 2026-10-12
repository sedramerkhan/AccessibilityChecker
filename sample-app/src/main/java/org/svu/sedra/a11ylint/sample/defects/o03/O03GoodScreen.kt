package org.svu.sedra.a11ylint.sample.defects.o03

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import org.svu.sedra.a11ylint.sample.R

/** O-03 good examples: one clickable per element. O-03 must report nothing here. */
@Composable
fun O03GoodScreen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)
    var subscribed by remember { mutableStateOf(false) }

    Column {
        // The card is not clickable; its actions are separate buttons.
        Card {
            Text("Compose accessibility")
            Row {
                Button(onClick = onOpen) { Text("Read") }
                IconButton(onClick = onFavorite) {
                    Icon(icon, contentDescription = "Favorite") // EXPECT: ComposeHardcodedA11yText
                }
            }
        }

        // One clickable row; the secondary action is a custom accessibility action.
        Row(
            modifier = Modifier
                .clickable(onClickLabel = "Open", role = Role.Button) { onOpen() } // EXPECT: ComposeHardcodedA11yText
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Share") { // EXPECT: ComposeHardcodedA11yText
                            onShare()
                            true
                        },
                    )
                },
        ) {
            Text("Running shoes")
        }

        // Sibling clickables.
        Row {
            Button(onClick = onOpen) { Text("Open") }
            Button(onClick = onShare) { Text("Share") }
        }

        // The row owns the interaction and the Checkbox passes null, so the control draws the
        // state without becoming a second touch target. Label and state are announced together.
        Row(
            modifier = Modifier.toggleable(
                value = subscribed,
                role = Role.Checkbox,
                onValueChange = { subscribed = it },
            ),
        ) {
            Checkbox(checked = subscribed, onCheckedChange = null)
            Text("Subscribe to updates")
        }
    }
}
