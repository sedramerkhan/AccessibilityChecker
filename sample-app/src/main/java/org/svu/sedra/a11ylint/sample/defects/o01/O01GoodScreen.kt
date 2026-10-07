package org.svu.sedra.a11ylint.sample.defects.o01

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.svu.sedra.a11ylint.sample.R

/** O-01 good examples: clickable areas of at least 48dp. O-01 must report nothing here. */
@Composable
fun O01GoodScreen(onAction: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // IconButton keeps a 48dp touch target even when the icon is small.
        IconButton(onClick = onAction) {
            Icon(icon, contentDescription = "Close", modifier = Modifier.size(24.dp)) // EXPECT: ComposeHardcodedA11yText
        }

        // A small icon with minimumInteractiveComponentSize().
        Icon(
            painter = icon,
            contentDescription = "Open", // EXPECT: ComposeHardcodedA11yText
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(24.dp)
                .clickable(role = Role.Button) { onAction() },
        )

        // Padding inside the clickable area makes it 48dp around a 20dp icon.
        Box(
            modifier = Modifier
                .clickable(onClickLabel = "Share", role = Role.Button) { onAction() } // EXPECT: ComposeHardcodedA11yText
                .padding(14.dp)
                .size(20.dp),
        ) {
            Icon(icon, contentDescription = "Share") // EXPECT: ComposeHardcodedA11yText
        }

        // A full-width row of 56dp.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClickLabel = "Settings", role = Role.Button) { onAction() }, // EXPECT: ComposeHardcodedA11yText
        ) {
            Icon(icon, contentDescription = "Settings") // EXPECT: ComposeHardcodedA11yText
        }
    }
}
