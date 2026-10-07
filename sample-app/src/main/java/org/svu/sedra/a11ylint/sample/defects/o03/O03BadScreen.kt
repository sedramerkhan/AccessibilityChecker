package org.svu.sedra.a11ylint.sample.defects.o03

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import org.svu.sedra.a11ylint.sample.R

/**
 * O-03 bad examples: clickable elements inside other clickable elements.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun O03BadScreen(onOpen: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // A clickable card with two icon buttons inside. Two clickable children with no
        // customActions is also O-07, reported on the card itself.
        Card(onClick = onOpen) { // EXPECT: ComposeMissingCustomActions
            Text("Compose accessibility")
            Row {
                IconButton(onClick = onFavorite) { // EXPECT: ComposeNestedClickable
                    Icon(icon, contentDescription = "Favorite")
                }
                IconButton(onClick = onShare) { // EXPECT: ComposeNestedClickable
                    Icon(icon, contentDescription = "Share")
                }
            }
        }

        // A clickable row that contains a button.
        Row(modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onOpen() }) {
            Text("Running shoes")
            Button(onClick = onShare) { // EXPECT: ComposeNestedClickable
                Text("Buy")
            }
        }

        // A clickable surface with a clickable box inside.
        Surface(onClick = onOpen) {
            Box(modifier = Modifier.clickable(onClickLabel = "More options", role = Role.Button) { onShare() }) { // EXPECT: ComposeNestedClickable
                Text("More options")
            }
        }
    }
}
