package org.svu.sedra.a11ylint.sample.defects.o03

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import org.svu.sedra.a11ylint.sample.R

/** O-03 good examples: one clickable per element. O-03 must report nothing here. */
@Composable
fun O03GoodScreen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // The card is not clickable; its actions are separate buttons.
        Card {
            Text("Compose accessibility")
            Row {
                Button(onClick = onOpen) { Text("Read") }
                IconButton(onClick = onFavorite) {
                    Icon(icon, contentDescription = "Favorite")
                }
            }
        }

        // One clickable row; the secondary action is a custom accessibility action.
        Row(
            modifier = Modifier
                .clickable(onClickLabel = "Open") { onOpen() }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Share") {
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
    }
}
