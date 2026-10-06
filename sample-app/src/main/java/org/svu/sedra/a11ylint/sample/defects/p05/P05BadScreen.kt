package org.svu.sedra.a11ylint.sample.defects.p05

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import org.svu.sedra.a11ylint.sample.R

/**
 * P-05 bad examples: text drawn straight onto a picture, where the contrast depends on what the
 * picture happens to show. Every reported line carries an EXPECT marker.
 */
@Composable
fun P05BadScreen() {
    val photo = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // A hero banner: the caption sits directly on the photo.
        Box {
            Image(photo, contentDescription = null)
            Text("Summer sale") // EXPECT: ComposeTextOverImage
        }

        // The first text above the picture is the one reported.
        Box {
            Image(photo, contentDescription = null)
            Text("Shop now") // EXPECT: ComposeTextOverImage
            Text("Ends on Sunday")
        }
    }
}
