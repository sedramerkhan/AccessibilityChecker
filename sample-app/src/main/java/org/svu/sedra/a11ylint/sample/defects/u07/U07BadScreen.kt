package org.svu.sedra.a11ylint.sample.defects.u07

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import org.svu.sedra.a11ylint.sample.R

/**
 * U-07 bad examples: accessibility text written as string literals, so it is never translated.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun U07BadScreen(onOpen: () -> Unit, onShare: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // The description only a screen reader hears, in English for every language.
        Icon(icon, contentDescription = "Delete draft") // EXPECT: ComposeHardcodedA11yText

        Row(
            modifier = Modifier
                .clickable(onClickLabel = "Open details", role = Role.Button) { onOpen() }, // EXPECT: ComposeHardcodedA11yText
        ) {
            Text("Shipping forecast")
        }

        // The same for every other kind of accessibility text.
        Row(
            modifier = Modifier.semantics {
                contentDescription = "Volume" // EXPECT: ComposeHardcodedA11yText
                stateDescription = "Muted" // EXPECT: ComposeHardcodedA11yText
                paneTitle = "Settings" // EXPECT: ComposeHardcodedA11yText
                error("Enter an email address") // EXPECT: ComposeHardcodedA11yText
                customActions = listOf(
                    CustomAccessibilityAction("Share") { onShare(); true }, // EXPECT: ComposeHardcodedA11yText
                )
            },
        ) {
            Text("Volume")
        }
    }
}
