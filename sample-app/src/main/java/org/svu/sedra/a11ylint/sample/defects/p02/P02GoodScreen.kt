package org.svu.sedra.a11ylint.sample.defects.p02

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun P02GoodScreen() {
    Icon(contentDescription = "Trash can illustration", modifier = Modifier)
    Text("Delete draft")
    Image(contentDescription = null, modifier = Modifier)
}
