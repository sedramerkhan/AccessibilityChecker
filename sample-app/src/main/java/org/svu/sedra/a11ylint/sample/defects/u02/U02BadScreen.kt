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

/**
 * U-02 bad examples: clickable elements that switch a state on and off without exposing it.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun U02BadScreen(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val subscribed = remember { mutableStateOf(false) }

    Column {
        // An expandable section.
        Row(modifier = Modifier.clickable { expanded = !expanded }) { // EXPECT: ComposeMissingStateDescription
            Text(if (expanded) "Hide details" else "Show details")
        }

        // A custom checkbox row backed by MutableState.
        Row(modifier = Modifier.clickable { subscribed.value = !subscribed.value }) { // EXPECT: ComposeMissingStateDescription
            Text("Subscribe to the newsletter")
        }

        // A custom switch row that reports the change to its caller.
        Row(modifier = Modifier.clickable { onDarkModeChange(!darkMode) }) { // EXPECT: ComposeMissingStateDescription
            Text("Dark mode")
        }
    }
}
