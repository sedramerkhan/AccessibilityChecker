package org.svu.sedra.a11ylint.sample.defects.u02

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * U-02 bad examples: clickable elements that switch a state on and off without exposing it.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun U02BadScreen(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val subscribed = remember { mutableStateOf(false) }

    Column {
        // An expandable section. The role is set, but the open or closed state is not.
        Row(
            modifier = Modifier.clickable( // EXPECT: ComposeMissingStateDescription
                onClickLabel = "Toggle details",
                role = Role.Button,
            ) { expanded = !expanded },
        ) {
            Text(if (expanded) "Hide details" else "Show details")
        }

        // A custom checkbox row backed by MutableState.
        Row(
            modifier = Modifier.clickable( // EXPECT: ComposeMissingStateDescription
                onClickLabel = "Toggle subscription",
                role = Role.Checkbox,
            ) { subscribed.value = !subscribed.value },
        ) {
            Text("Subscribe to the newsletter")
        }

        // A custom switch row that reports the change to its caller.
        Row(
            modifier = Modifier.clickable( // EXPECT: ComposeMissingStateDescription
                onClickLabel = "Toggle dark mode",
                role = Role.Switch,
            ) { onDarkModeChange(!darkMode) },
        ) {
            Text("Dark mode")
        }
    }
}
