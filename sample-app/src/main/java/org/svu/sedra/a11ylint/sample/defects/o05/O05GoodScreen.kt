package org.svu.sedra.a11ylint.sample.defects.o05

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.svu.sedra.a11ylint.sample.R

/**
 * O-05 good examples: every clickable has something to announce. O-05 must report nothing here.
 */
@Composable
fun O05GoodScreen(onAction: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // A visible label.
        Box(
            modifier = Modifier.clickable(onClickLabel = "Open", role = Role.Button) { onAction() },
        ) {
            Text("Open")
        }

        // A labelled icon.
        Box(
            modifier = Modifier.clickable(onClickLabel = "Delete", role = Role.Button) { onAction() },
        ) {
            Icon(icon, contentDescription = "Delete draft")
        }

        // Nothing visible, but the name is set in semantics.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClickLabel = "Dismiss", role = Role.Button) { onAction() }
                .semantics { contentDescription = "Dismiss the dialog" },
        )

        // A Material icon button with a labelled icon.
        IconButton(onClick = onAction) {
            Icon(icon, contentDescription = "Share")
        }
    }
}
