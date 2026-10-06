package org.svu.sedra.a11ylint.sample.defects.p05

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import org.svu.sedra.a11ylint.sample.R

/**
 * P-05 good examples: the text is protected from the picture, or it is not over it at all.
 * P-05 must report nothing here.
 */
@Composable
fun P05GoodScreen() {
    val photo = painterResource(R.drawable.ic_launcher_foreground)

    Column {
        // A scrim between the picture and the caption.
        Box {
            Image(photo, contentDescription = null)
            Box(modifier = Modifier.fillMaxSize().background(Color(0x99000000)))
            Text("Summer sale")
        }

        // The caption carries its own background.
        Box {
            Image(photo, contentDescription = null)
            Text("Summer sale", modifier = Modifier.background(Color(0xCC000000)))
        }

        // The caption is under the picture, not on it.
        Column {
            Image(photo, contentDescription = null)
            Text("Summer sale")
        }
    }
}
