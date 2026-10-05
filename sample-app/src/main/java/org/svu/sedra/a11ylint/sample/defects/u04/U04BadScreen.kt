package org.svu.sedra.a11ylint.sample.defects.u04

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * U-04 bad examples: buttons whose label says that something happens but not what.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun U04BadScreen(onConfirm: () -> Unit, onOpen: () -> Unit, onSend: () -> Unit) {
    Column {
        // A confirmation dialog button: out of context, "OK" means nothing.
        Button(onClick = onConfirm) {
            Text("OK") // EXPECT: ComposeVagueButtonLabel
        }

        // The classic link label.
        TextButton(onClick = onOpen) {
            Text("Click here") // EXPECT: ComposeVagueButtonLabel
        }

        // Spacing and case do not change the label.
        OutlinedButton(onClick = onSend) {
            Text(" Submit ") // EXPECT: ComposeVagueButtonLabel
        }
    }
}
