package org.svu.sedra.a11ylint.sample.defects.p01

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import org.svu.sedra.a11ylint.sample.R

/**
 * P-01 bad examples: interactive icons and images whose only possible label is missing.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun P01BadScreen(onAction: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)
    var favorite by remember { mutableStateOf(false) }

    Column {
        // IconButton whose only content is an unlabelled icon.
        IconButton(onClick = onAction) {
            Icon(painter = icon, contentDescription = null) // EXPECT: ComposeMissingContentDescription
        }

        // An empty string is announced the same as no label.
        FloatingActionButton(onClick = onAction) {
            Icon(icon, "") // EXPECT: ComposeMissingContentDescription
        }

        // The icon itself is clickable.
        Icon(
            painter = icon,
            contentDescription = null, // EXPECT: ComposeMissingContentDescription
            modifier = Modifier.clickable(role = Role.Button) { onAction() },
        )

        // A toggleable image.
        Image(
            painter = icon,
            contentDescription = null, // EXPECT: ComposeMissingContentDescription
            modifier = Modifier.toggleable(value = favorite, onValueChange = { favorite = it }),
        )

        // A clickable container whose only content is an icon, nested in a Row.
        Box(modifier = Modifier.clickable(role = Role.Button) { onAction() }) {
            Row {
                Icon(icon, contentDescription = null) // EXPECT: ComposeMissingContentDescription
            }
        }

        // A clickable Card with only an icon.
        Card(onClick = onAction) {
            Icon(icon, contentDescription = null) // EXPECT: ComposeMissingContentDescription
        }
    }
}
