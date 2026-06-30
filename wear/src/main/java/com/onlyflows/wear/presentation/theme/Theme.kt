package com.onlyflows.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme

private val FlowBlue = Color(0xFF4A90D9)

@Composable
fun OnlyFlowsWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = FlowBlue,
            onPrimary = Color.White,
        ),
        content = content,
    )
}
