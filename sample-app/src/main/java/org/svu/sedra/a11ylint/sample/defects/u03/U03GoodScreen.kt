package org.svu.sedra.a11ylint.sample.defects.u03

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics

/**
 * U-03 good examples: the error state always comes with a message that says what is wrong.
 * U-03 must report nothing here.
 */
@Composable
fun U03GoodScreen(
    email: String,
    password: String,
    age: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    hasPasswordError: Boolean,
) {
    Column {
        // The message is in the supporting text, where it is also visible.
        TextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            supportingText = { Text("Enter a valid email address") },
            isError = true,
        )

        // The message is set in semantics.
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.semantics { error("Use at least 8 characters") }, // EXPECT: ComposeHardcodedA11yText
            label = { Text("Password") },
            isError = hasPasswordError,
        )

        // No error state at all.
        TextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text("Age") },
            isError = false,
        )
    }
}
