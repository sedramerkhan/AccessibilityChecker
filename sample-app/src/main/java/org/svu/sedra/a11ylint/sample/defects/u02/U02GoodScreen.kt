package org.svu.sedra.a11ylint.sample.defects.u02

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/** U-02 good examples: toggles that expose their state. U-02 must report nothing here. */
@Composable
fun U02GoodScreen(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit, onOpen: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var subscribed by remember { mutableStateOf(false) }

    Column {
        // The state is described in semantics.
        Row(
            modifier = Modifier
                .clickable(onClickLabel = "Toggle details") { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
        ) {
            Text(if (expanded) "Hide details" else "Show details")
        }

        // A toggleable row with a checkbox that does not handle clicks itself.
        Row(
            modifier = Modifier.toggleable(
                value = subscribed,
                role = Role.Checkbox,
                onValueChange = { subscribed = it },
            ),
        ) {
            Checkbox(checked = subscribed, onCheckedChange = null)
            Text("Subscribe to the newsletter")
        }

        // Material Switch exposes its state.
        Row {
            Text("Dark mode")
            Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
        }

        // A plain click that does not toggle anything.
        Row(modifier = Modifier.clickable(onClickLabel = "Open settings") { onOpen() }) {
            Text("Open settings")
        }
    }
}
