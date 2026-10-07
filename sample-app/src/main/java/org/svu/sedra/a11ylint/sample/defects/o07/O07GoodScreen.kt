package org.svu.sedra.a11ylint.sample.defects.o07

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

/**
 * O-07 good examples: either the extra actions are offered through customActions, or the
 * container is not clickable itself so every button is reached on its own. O-07 must report
 * nothing here.
 */
@Composable
fun O07GoodScreen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // One element with one action, and the two extra actions in the actions menu.
        Card(
            onClick = onOpen,
            modifier = Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Add to favourites") { onFavorite(); true },
                    CustomAccessibilityAction("Share") { onShare(); true },
                )
            },
        ) {
            Text("Shipping forecast")
        }

        // The container is not clickable, so each button is its own element already.
        Card {
            Text("Running shoes")
            Row {
                IconButton(onClick = onFavorite) {
                    Icon(icon, contentDescription = "Add to favourites")
                }
                IconButton(onClick = onShare) {
                    Icon(icon, contentDescription = "Share")
                }
            }
        }
    }
}
