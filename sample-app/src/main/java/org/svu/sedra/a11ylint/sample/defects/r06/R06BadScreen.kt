package org.svu.sedra.a11ylint.sample.defects.r06

import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * R-06 bad examples: composables that take input through a custom gesture and set no semantics,
 * so TalkBack has no action to offer. Every reported line carries an EXPECT marker, on the
 * function declaration.
 */

// A colour swatch that only responds to a raw tap.
@Composable
fun ColourSwatch(onPick: () -> Unit) { // EXPECT: ComposeComposableWithoutSemantics
    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onPick() }) },
    )
}

// A custom slider that can only be dragged.
@Composable
fun VolumeSlider(state: DraggableState) { // EXPECT: ComposeComposableWithoutSemantics
    Box(
        modifier = Modifier
            .size(48.dp)
            .draggable(state, Orientation.Horizontal),
    )
}
