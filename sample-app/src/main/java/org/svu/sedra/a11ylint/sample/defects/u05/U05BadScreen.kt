package org.svu.sedra.a11ylint.sample.defects.u05

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * U-05 bad examples: text fields without a label. The first three have no name at all and are
 * errors; the last has a name for screen readers but no visible label, and is a warning.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun U05BadScreen() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    Column {
        // No label at all.
        TextField(value = name, onValueChange = { name = it }) // EXPECT: ComposeTextFieldWithoutLabel

        // Only a placeholder, which disappears when the user types.
        OutlinedTextField( // EXPECT: ComposeTextFieldWithoutLabel
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("you@example.com") },
        )

        // A visible Text above the field is not connected to it.
        Text("Discount code")
        TextField(value = code, onValueChange = { code = it }) // EXPECT: ComposeTextFieldWithoutLabel

        // Screen readers can name this field, but nothing on screen says what to enter.
        TextField( // EXPECT: ComposeTextFieldWithoutLabel
            value = city,
            onValueChange = { city = it },
            modifier = Modifier.semantics { contentDescription = "City" }, // EXPECT: ComposeHardcodedA11yText
        )
    }
}
