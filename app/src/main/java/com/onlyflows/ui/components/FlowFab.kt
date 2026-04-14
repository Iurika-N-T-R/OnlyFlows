package com.onlyflows.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.onlyflows.ui.theme.FabBlue
import com.onlyflows.ui.theme.OnlyFlowsTheme

@Composable
fun FlowFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick          = onClick,
        containerColor   = FabBlue,
        contentColor     = Color.White,
        modifier         = modifier,
    ) {
        Icon(
            imageVector      = Icons.Rounded.Add,
            contentDescription = "Add task",
        )
    }
}

@Preview
@Composable
private fun FlowFabPreview() {
    OnlyFlowsTheme {
        FlowFab(onClick = {})
    }
}
