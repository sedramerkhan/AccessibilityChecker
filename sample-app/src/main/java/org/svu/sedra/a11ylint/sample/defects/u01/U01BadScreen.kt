package org.svu.sedra.a11ylint.sample.defects.u01

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * U-01 bad examples: text that looks like a heading but is not marked as one.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun U01BadScreen() {
    Column {
        // A headline style.
        Text("Account settings", style = MaterialTheme.typography.headlineSmall) // EXPECT: ComposeMissingHeading
        Text("Signed in as Sedra")

        // titleLarge, also after a copy().
        Text( // EXPECT: ComposeMissingHeading
            text = "Notifications",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        )
        Text("Email and push messages")

        // A large bold font.
        Text("Privacy", fontSize = 22.sp, fontWeight = FontWeight.Bold) // EXPECT: ComposeMissingHeading

        // A large bold TextStyle.
        Text("Storage", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.W800)) // EXPECT: ComposeMissingHeading
    }
}
