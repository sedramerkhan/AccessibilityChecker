package org.svu.sedra.a11ylint.sample.defects.r01

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.svu.sedra.a11ylint.sample.R

/**
 * R-01 bad examples: custom clickable elements that do not say what kind of control they are.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun R01BadScreen(onOpen: () -> Unit, onMenu: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // A clickable box that holds only an icon.
        Box(modifier = Modifier.clickable(onClickLabel = "Open gallery") { onOpen() }) { // EXPECT: ComposeClickableWithoutRole, ComposeHardcodedA11yText
            Icon(icon, contentDescription = "Open gallery") // EXPECT: ComposeHardcodedA11yText
        }

        // A clickable row with rich content.
        Row(modifier = Modifier.clickable(onClickLabel = "Open profile") { onOpen() }) { // EXPECT: ComposeClickableWithoutRole, ComposeHardcodedA11yText
            Text("Sedra Merkhan")
            Text("Last seen today")
        }

        // A Card made clickable with a modifier instead of onClick.
        Card(modifier = Modifier.clickable(onClickLabel = "Open order") { onOpen() }) { // EXPECT: ComposeClickableWithoutRole, ComposeHardcodedA11yText
            Text("Order #1024")
        }

        // An image with click and long click.
        Image(
            painter = icon,
            contentDescription = "Profile photo", // EXPECT: ComposeHardcodedA11yText
            modifier = Modifier.combinedClickable(onLongClick = onMenu) { onOpen() }, // EXPECT: ComposeClickableWithoutRole
        )
    }
}
