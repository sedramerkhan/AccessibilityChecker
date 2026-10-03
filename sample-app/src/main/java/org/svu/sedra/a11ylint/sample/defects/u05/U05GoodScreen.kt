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

/** U-05 good examples: every text field has a label. U-05 must report nothing here. */
@Composable
fun U05GoodScreen() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
        )

        // A label and a placeholder together.
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            placeholder = { Text("you@example.com") },
        )
    }
}
