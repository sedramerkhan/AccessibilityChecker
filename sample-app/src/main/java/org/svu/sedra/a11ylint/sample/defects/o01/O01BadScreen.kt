package org.svu.sedra.a11ylint.sample.defects.o01

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.svu.sedra.a11ylint.sample.R

/**
 * O-01 bad examples: clickable elements smaller than 48dp.
 * Every reported line carries an EXPECT marker.
 */
@Composable
fun O01BadScreen(onAction: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)
    var liked by remember { mutableStateOf(false) }

    Column {
        // A 24dp clickable icon.
        Icon(
            painter = icon,
            contentDescription = "Close",
            modifier = Modifier
                .size(24.dp) // EXPECT: ComposeSmallTouchTarget
                .clickable(role = Role.Button) { onAction() },
        )

        // The size is set after the clickable, which still limits its bounds.
        Box(
            modifier = Modifier
                .clickable(onClickLabel = "Open", role = Role.Button) { onAction() }
                .size(32.dp), // EXPECT: ComposeSmallTouchTarget
        ) {
            Icon(icon, contentDescription = "Open")
        }

        // Padding inside a 48dp box makes the clickable area only 32dp.
        Box(
            modifier = Modifier
                .size(48.dp) // EXPECT: ComposeSmallTouchTarget
                .padding(8.dp)
                .clickable(onClickLabel = "Share", role = Role.Button) { onAction() },
        ) {
            Icon(icon, contentDescription = "Share")
        }

        // Only the width is too small.
        Icon(
            painter = icon,
            contentDescription = "Like",
            modifier = Modifier
                .toggleable(value = liked, onValueChange = { liked = it })
                .width(40.dp) // EXPECT: ComposeSmallTouchTarget
                .height(56.dp),
        )

        // The size comes from a local value.
        val small = Modifier.requiredSize(20.dp) // EXPECT: ComposeSmallTouchTarget
        Box(modifier = small.clickable(onClickLabel = "More", role = Role.Button) { onAction() }) {
            Icon(icon, contentDescription = "More")
        }
    }
}
