package org.svu.sedra.a11ylint.sample.defects.p01

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.svu.sedra.a11ylint.sample.R

/** P-01 good examples: the same elements with a correct label, or decorative images. P-01 must report nothing here. */
@Composable
fun P01GoodScreen(onAction: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // The icon describes the action.
        IconButton(onClick = onAction) {
            Icon(painter = icon, contentDescription = stringResource(R.string.app_name))
        }

        // The icon is decorative because the button has a Text.
        Button(onClick = onAction) {
            Icon(icon, contentDescription = null)
            Text("Open")
        }

        // The label is set in semantics on the clickable parent.
        IconButton(
            onClick = onAction,
            modifier = Modifier.semantics { contentDescription = "Open" }, // EXPECT: ComposeHardcodedA11yText
        ) {
            Icon(icon, contentDescription = null)
        }

        // A clickable Row whose Text names it.
        Row(modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onAction() }) { // EXPECT: ComposeHardcodedA11yText
            Icon(icon, contentDescription = null)
            Text("Open")
        }

        // Non-interactive images may be decorative.
        Image(icon, contentDescription = null)
        Card {
            Box {
                Icon(icon, contentDescription = "")
            }
        }
    }
}
