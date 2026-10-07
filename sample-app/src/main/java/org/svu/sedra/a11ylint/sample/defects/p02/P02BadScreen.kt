package org.svu.sedra.a11ylint.sample.defects.p02

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import org.svu.sedra.a11ylint.sample.R

@Composable
fun P02BadScreen() {
    val icon = painterResource(R.drawable.ic_launcher_foreground)
    Icon(painter = icon, contentDescription = "Delete") // EXPECT: ComposeDecorativeImageLabeled, ComposeHardcodedA11yText
    Text("Delete")
}
