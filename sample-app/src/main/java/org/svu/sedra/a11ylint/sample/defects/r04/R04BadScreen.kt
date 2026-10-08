package org.svu.sedra.a11ylint.sample.defects.r04

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * R-04 bad examples: lists built by hand with a loop, so nothing tells accessibility services
 * how many items there are. Every reported line carries an EXPECT marker.
 */
@Composable
fun R04BadScreen(orders: List<String>) {
    Column {
        // A scrolling list that looks like a LazyColumn but announces no count or position.
        Column( // EXPECT: ComposeMissingCollectionInfo
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            orders.forEach { order ->
                Text(order)
            }
        }

        // The same defect written with a for loop.
        Row { // EXPECT: ComposeMissingCollectionInfo
            for (order in orders) {
                Text(order)
            }
        }
    }
}
