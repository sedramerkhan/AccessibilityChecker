package org.svu.sedra.a11ylint.sample.defects.r01

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import org.svu.sedra.a11ylint.sample.R

/** R-01 good examples: custom clickables with a role, and Material clickables. R-01 must report nothing here. */
@Composable
fun R01GoodScreen(onOpen: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // The role is passed to clickable.
        Box(modifier = Modifier.clickable(role = Role.Button) { onOpen() }) {
            Icon(icon, contentDescription = "Open gallery")
        }

        // The role is set in semantics.
        Row(
            modifier = Modifier
                .clickable { onOpen() }
                .semantics { role = Role.Tab },
        ) {
            Text("Messages")
            Text("3 new")
        }

        // A Material Card with onClick sets its own role.
        Card(onClick = onOpen) {
            Text("Order #1024")
        }

        // An image that acts as a button.
        Image(
            painter = icon,
            contentDescription = "Profile photo",
            modifier = Modifier.clickable(role = Role.Image) { onOpen() },
        )
    }
}
