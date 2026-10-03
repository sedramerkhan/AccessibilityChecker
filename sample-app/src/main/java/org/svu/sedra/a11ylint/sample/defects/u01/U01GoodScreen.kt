package org.svu.sedra.a11ylint.sample.defects.u01

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** U-01 good examples: headings marked with heading(), and text that is not a heading. U-01 must report nothing here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun U01GoodScreen(onSave: () -> Unit) {
    Column {
        // A top app bar title (ignored by the rule, see DECISIONS).
        TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.titleLarge) })

        // A heading marked as one.
        Text(
            text = "Account settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        // Body text, and large text that is not bold.
        Text("Signed in as Sedra", style = MaterialTheme.typography.bodyLarge)
        Text("42", fontSize = 32.sp)
        Text("Small bold label", fontSize = 14.sp, fontWeight = FontWeight.Bold)

        // Text inside a button is the button's label, not a heading.
        Button(onClick = onSave) {
            Text("Save", style = MaterialTheme.typography.titleLarge)
        }
    }
}
