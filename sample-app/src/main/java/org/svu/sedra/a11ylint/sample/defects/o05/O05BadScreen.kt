package org.svu.sedra.a11ylint.sample.defects.o05

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * O-05 bad examples: clickable elements with nothing readable inside. Every reported line
 * carries an EXPECT marker. Each clickable sets an onClickLabel and a role so only O-05 is
 * reported here; an onClickLabel describes the action, not the element, so it is not a name.
 */
@Composable
fun O05BadScreen(onAction: () -> Unit) {
    Column {
        // A tappable square with no content at all.
        Box( // EXPECT: ComposeEmptyClickable
            modifier = Modifier
                .size(48.dp)
                .clickable(onClickLabel = "Open", role = Role.Button) { onAction() }, // EXPECT: ComposeHardcodedA11yText
        )

        // The content is only spacing, so there is still nothing to announce.
        Box( // EXPECT: ComposeEmptyClickable
            modifier = Modifier
                .size(48.dp)
                .clickable(onClickLabel = "Open", role = Role.Button) { onAction() }, // EXPECT: ComposeHardcodedA11yText
        ) {
            Spacer(Modifier.size(8.dp))
        }

        // A tappable scrim: easy to miss, and invisible to screen readers.
        Spacer( // EXPECT: ComposeEmptyClickable
            modifier = Modifier
                .size(48.dp)
                .clickable(onClickLabel = "Dismiss", role = Role.Button) { onAction() }, // EXPECT: ComposeHardcodedA11yText
        )

        // An icon button that never got its icon.
        IconButton(onClick = onAction) { // EXPECT: ComposeEmptyClickable
        }

        // A custom drawn control with no semantics.
        Canvas( // EXPECT: ComposeEmptyClickable
            modifier = Modifier
                .size(48.dp)
                .clickable(onClickLabel = "Pick a colour", role = Role.Button) { onAction() }, // EXPECT: ComposeHardcodedA11yText
        ) {
        }
    }
}
