package org.svu.sedra.a11ylint.sample.defects.p04

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * P-04 bad examples: a text size converted from a dp value, so it will not scale again when the
 * user increases their system font size. Every reported line carries an EXPECT marker.
 */
@Composable
fun P04BadScreen() {
    Column {
        // A literal dp size, converted inline.
        Text(
            "Caption",
            fontSize = with(LocalDensity.current) { 16.dp.toSp() }, // EXPECT: ComposeTextSizeInDp
        )

        // A dp value stored in a variable before the conversion.
        val cardPadding = 16.dp
        Text(
            "Caption",
            fontSize = with(LocalDensity.current) { cardPadding.toSp() }, // EXPECT: ComposeTextSizeInDp
        )

        // The same conversion set through a TextStyle.
        Text(
            "Caption",
            style = TextStyle(
                fontSize = with(LocalDensity.current) { 16.dp.toSp() }, // EXPECT: ComposeTextSizeInDp
            ),
        )
    }
}
