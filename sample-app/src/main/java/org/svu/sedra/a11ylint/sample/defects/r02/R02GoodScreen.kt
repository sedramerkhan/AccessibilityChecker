package org.svu.sedra.a11ylint.sample.defects.r02

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import org.svu.sedra.a11ylint.sample.R

/**
 * R-02 good examples: whatever the content provided is put back in the block.
 * R-02 must report nothing here.
 */
@Composable
fun R02GoodScreen(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onOpen: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // Two texts become one sentence, which is the point of clearing them.
        Row(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Running shoes, 42 euro"
            },
        ) {
            Text("Running shoes")
            Text("42 euro")
        }

        // The name and the role are both put back.
        Row(
            modifier = Modifier
                .clickable(onClickLabel = "Open", role = Role.Button) { onOpen() }
                .clearAndSetSemantics {
                    contentDescription = "Running shoes, 42 euro"
                    role = Role.Button
                },
        ) {
            Text("Running shoes")
        }

        // The checkbox state is described in words.
        Row(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Subscribe to the newsletter"
                stateDescription = if (checked) "Subscribed" else "Not subscribed"
            },
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text("Subscribe to the newsletter")
        }

        // Decorative stars carried no name of their own, and the block adds one.
        Row(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Rated 2 out of 5"
            },
        ) {
            Icon(icon, contentDescription = null)
            Icon(icon, contentDescription = null)
        }
    }
}
