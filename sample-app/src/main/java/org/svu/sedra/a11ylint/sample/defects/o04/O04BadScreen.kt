package org.svu.sedra.a11ylint.sample.defects.o04

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.svu.sedra.a11ylint.sample.R

/**
 * O-04 bad examples: clickable containers whose content is button-like, so they should be a
 * real Button. Every reported line carries an EXPECT marker. Each clickable sets an
 * onClickLabel so only O-04 is reported here.
 */
@Composable
fun O04BadScreen(onAction: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // A row whose only content is its label.
        Row(modifier = Modifier.clickable(onClickLabel = "Buy now") { onAction() }) { // EXPECT: ComposeClickableContainer
            Text("Buy now")
        }

        // An icon and a label: still a button.
        Box(modifier = Modifier.clickable(onClickLabel = "Delete draft") { onAction() }) { // EXPECT: ComposeClickableContainer
            Icon(icon, contentDescription = null)
            Text("Delete draft")
        }

        // Spacers between the parts do not change what this is.
        Column(modifier = Modifier.clickable(onClickLabel = "Read more") { onAction() }) { // EXPECT: ComposeClickableContainer
            Spacer(Modifier)
            Text("Read more")
        }
    }
}
