package com.onlyflows.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.onlyflows.ui.theme.FabBlue
import com.onlyflows.ui.theme.Motion
import com.onlyflows.ui.theme.OnlyFlowsTheme
import com.onlyflows.ui.theme.rememberReducedMotion

@Composable
fun FlowFab(
    onClick: () -> Unit,
    /** Screens hide the FAB in selection mode; passing it here animates that instead of
     *  popping the button out of the layout. */
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val interaction   = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Pressing sinks the button and tips the plus sign — the touch target is large and the
    // ripple alone gave no sense of having actually pushed something.
    val press by animateFloatAsState(
        targetValue   = if (pressed) 0.88f else 1f,
        animationSpec = if (reducedMotion) snap() else tween(Motion.QuickMs),
        label         = "fabPress",
    )
    val iconSpin by animateFloatAsState(
        targetValue   = if (pressed) 45f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(Motion.QuickMs),
        label         = "fabIconSpin",
    )

    AnimatedVisibility(
        visible = visible,
        enter   = if (reducedMotion) fadeIn(snap()) else scaleIn(Motion.entranceSpring()) + fadeIn(tween(Motion.StandardMs)),
        exit    = if (reducedMotion) fadeOut(snap()) else scaleOut(tween(Motion.QuickMs)) + fadeOut(tween(Motion.QuickMs)),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick           = onClick,
            containerColor    = FabBlue,
            contentColor      = Color.White,
            interactionSource = interaction,
            modifier          = Modifier.scale(press),
        ) {
            Icon(
                imageVector        = Icons.Rounded.Add,
                contentDescription = "Add task",
                modifier           = Modifier.rotate(iconSpin),
            )
        }
    }
}

@Preview
@Composable
private fun FlowFabPreview() {
    OnlyFlowsTheme {
        FlowFab(onClick = {})
    }
}
