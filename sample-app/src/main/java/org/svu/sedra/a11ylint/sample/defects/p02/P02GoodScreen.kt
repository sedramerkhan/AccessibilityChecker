package org.svu.sedra.a11ylint.sample.defects.p02

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.svu.sedra.a11ylint.sample.R

@Composable
fun P02GoodScreen() {
    val icon = painterResource(R.drawable.ic_launcher_foreground)
    Icon(painter = icon, contentDescription = "Trash can illustration", modifier = Modifier)
    Text("Delete draft")
    Image(painter = icon, contentDescription = null, modifier = Modifier)
}
