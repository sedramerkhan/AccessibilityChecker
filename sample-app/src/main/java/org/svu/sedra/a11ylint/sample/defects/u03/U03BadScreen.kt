package org.svu.sedra.a11ylint.sample.defects.u03

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable

/**
 * U-03 bad examples: fields in their error state with no message saying what is wrong. Material
 * only adds its generic "Error" announcement (see DECISIONS). Every reported line carries an
 * EXPECT marker. Each field has a label so only U-03 is reported here.
 */
@Composable
fun U03BadScreen(
    email: String,
    password: String,
    age: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    hasPasswordError: Boolean,
) {
    Column {
        // The error state is on, but nothing says why.
        TextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            isError = true, // EXPECT: ComposeMissingSemanticError
        )

        // The same with a state the rule cannot evaluate.
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            isError = hasPasswordError, // EXPECT: ComposeMissingSemanticError
        )

        // An explicit null supporting text is the same as leaving it out.
        TextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text("Age") },
            supportingText = null,
            isError = age.isEmpty(), // EXPECT: ComposeMissingSemanticError
        )
    }
}
