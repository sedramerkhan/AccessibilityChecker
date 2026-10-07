package org.svu.sedra.a11ylint.sample.defects.o07

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import org.svu.sedra.a11ylint.sample.R

/**
 * O-07 bad examples: a clickable container that also holds several clickable children, with no
 * customActions, so a screen reader offers only the container's own action. Every reported line
 * carries an EXPECT marker. The nested buttons are O-03's defect and are marked too.
 */
@Composable
fun O07BadScreen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // A list row that opens the item and also carries a favourite and a share button.
        Card(onClick = onOpen) { // EXPECT: ComposeMissingCustomActions
            Text("Shipping forecast")
            Row {
                IconButton(onClick = onFavorite) { // EXPECT: ComposeNestedClickable
                    Icon(icon, contentDescription = "Add to favourites")
                }
                IconButton(onClick = onShare) { // EXPECT: ComposeNestedClickable
                    Icon(icon, contentDescription = "Share")
                }
            }
        }

        // The same defect on a clickable Row. The onClickLabel and role keep O-02 and R-01 quiet.
        Row( // EXPECT: ComposeMissingCustomActions
            modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onOpen() },
        ) {
            Text("Running shoes")
            IconButton(onClick = onFavorite) { // EXPECT: ComposeNestedClickable
                Icon(icon, contentDescription = "Add to favourites")
            }
            IconButton(onClick = onShare) { // EXPECT: ComposeNestedClickable
                Icon(icon, contentDescription = "Share")
            }
        }
    }
}
