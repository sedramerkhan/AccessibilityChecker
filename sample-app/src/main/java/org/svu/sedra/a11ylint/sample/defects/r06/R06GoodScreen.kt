package org.svu.sedra.a11ylint.sample.defects.r06

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * R-06 good examples: the gesture stays, and the semantics say what it does.
 * R-06 must report nothing here.
 */

// The gesture is kept for pointer users; semantics describe it for everyone else.
@Composable
fun GoodColourSwatch(onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onPick() }) }
            .semantics { contentDescription = "Pick the colour red" }, // EXPECT: ComposeHardcodedA11yText
    )
}

// A drag gesture with the current value described in words.
@Composable
fun GoodVolumeSlider(state: DraggableState, volume: Int) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .draggable(state, Orientation.Horizontal)
            .semantics {
                contentDescription = "Volume" // EXPECT: ComposeHardcodedA11yText
                stateDescription = "$volume percent"
            },
    )
}

// A plain tap is all this does, so a click modifier is the simpler answer.
@Composable
fun GoodTapTarget(onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClickLabel = "Pick the colour red", role = Role.Button) { onPick() } // EXPECT: ComposeHardcodedA11yText
            .semantics { contentDescription = "Pick the colour red" }, // EXPECT: ComposeHardcodedA11yText
    )
}
