package org.svu.sedra.a11ylint.sample.defects.u06

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import org.svu.sedra.a11ylint.sample.R

/**
 * U-06 bad examples: the same icon described in different words, so one control sounds like two.
 * The second wording lives in [U06BadOtherScreen] below, because this is the one rule that
 * compares across files. Both ways of naming an icon are shown: a Material `Icons.X.Y` and a
 * drawable read at the call site. Every reported line carries an EXPECT marker.
 */
@Composable
fun U06BadScreen() {
    Column {
        // Here the bin is "Delete"...
        Icon(Icons.Default.Delete, contentDescription = "Delete") // EXPECT: ComposeInconsistentLabels, ComposeHardcodedA11yText
        Text("Draft")

        // ...the share icon is "Share"...
        Icon(Icons.Default.Share, contentDescription = "Share") // EXPECT: ComposeInconsistentLabels, ComposeHardcodedA11yText

        // ...and the drawable is "Profile".
        Icon(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Profile", // EXPECT: ComposeInconsistentLabels, ComposeHardcodedA11yText
        )
    }
}

/**
 * A second screen that calls the same three icons something else. Either wording on its own is
 * fine; having both in one app is the defect.
 */
@Composable
fun U06BadOtherScreen() {
    Column {
        // The same bin, now called something else.
        Icon(Icons.Default.Delete, contentDescription = "Remove item") // EXPECT: ComposeInconsistentLabels, ComposeHardcodedA11yText
        Text("Draft")

        Icon(Icons.Filled.Share, contentDescription = "Send to") // EXPECT: ComposeInconsistentLabels, ComposeHardcodedA11yText

        Icon(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Account", // EXPECT: ComposeInconsistentLabels, ComposeHardcodedA11yText
        )
    }
}
