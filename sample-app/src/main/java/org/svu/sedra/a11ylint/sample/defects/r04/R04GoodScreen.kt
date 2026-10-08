package org.svu.sedra.a11ylint.sample.defects.r04

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics

/**
 * R-04 good examples: either a lazy list, which provides collection semantics itself, or a
 * hand-built list that sets them. R-04 must report nothing here.
 */
@Composable
fun R04GoodScreen(orders: List<String>) {
    Column {
        // A lazy list announces "list, N items" and "item 2 of N" without any extra code.
        LazyColumn {
            items(orders) { order ->
                Text(order)
            }
        }

        // A hand-built list can say the same, but it has to be written out.
        Column(
            modifier = Modifier.semantics {
                collectionInfo = CollectionInfo(rowCount = orders.size, columnCount = 1)
            },
        ) {
            orders.forEachIndexed { index, order ->
                Text(
                    order,
                    modifier = Modifier.semantics {
                        collectionItemInfo = CollectionItemInfo(
                            rowIndex = index,
                            rowSpan = 1,
                            columnIndex = 0,
                            columnSpan = 1,
                        )
                    },
                )
            }
        }
    }
}
