package org.svu.sedra.a11ylint.sample.defects.u06

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import org.svu.sedra.a11ylint.sample.R

/**
 * U-06 good examples: each icon keeps one wording everywhere it appears, including across the
 * two screens below, and for both ways of naming an icon. U-06 must report nothing here. The
 * labels are still literals, which is U-07's separate point, so those markers are present.
 */
@Composable
fun U06GoodScreen() {
    Column {
        Icon(Icons.Default.Add, contentDescription = "Add to list") // EXPECT: ComposeHardcodedA11yText
        Text("New item")

        Icon(Icons.Default.Favorite, contentDescription = "Add to favourites") // EXPECT: ComposeHardcodedA11yText

        Icon(
            painterResource(R.drawable.ic_launcher_background),
            contentDescription = "Open profile", // EXPECT: ComposeHardcodedA11yText
        )
    }
}

/** A second screen using the same three icons, with exactly the same words. */
@Composable
fun U06GoodOtherScreen() {
    Column {
        Icon(Icons.Default.Add, contentDescription = "Add to list") // EXPECT: ComposeHardcodedA11yText
        Text("New item")

        Icon(Icons.Filled.Favorite, contentDescription = "Add to favourites") // EXPECT: ComposeHardcodedA11yText

        Icon(
            painterResource(R.drawable.ic_launcher_background),
            contentDescription = "Open profile", // EXPECT: ComposeHardcodedA11yText
        )
    }
}
