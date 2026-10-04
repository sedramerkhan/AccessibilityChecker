package org.svu.sedra.a11ylint.sample.defects.p02

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun P02BadScreen() {
    Icon(contentDescription = "Delete") // EXPECT: ComposeDecorativeImageLabeled
    Text("Delete")
}
