package com.onlyflows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlyflows.data.mock.MockData
import com.onlyflows.domain.model.AgendaEvent
import com.onlyflows.ui.theme.OnlyFlowsTheme
import com.onlyflows.ui.theme.flowColors

@Composable
fun AgendaCard(
    events: List<AgendaEvent>,
    modifier: Modifier = Modifier,
) {
    val fc = MaterialTheme.flowColors

    Surface(
        color  = fc.agendaCardBg,
        shape  = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            events.forEach { event ->
                AgendaEventRow(event = event)
            }
        }
    }
}

@Composable
private fun AgendaEventRow(event: AgendaEvent) {
    val isDark = MaterialTheme.flowColors.isDark
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp),
    ) {
        // Color indicator bar
        val indicatorColor = try {
            Color(android.graphics.Color.parseColor(event.colorHex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
        // In dark mode, lighten the time text so dark calendar colors remain readable
        val timeTextColor = if (isDark) lerp(indicatorColor, Color.White, 0.45f) else indicatorColor

        if (!event.isAllDay) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text  = event.time ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = timeTextColor,
                modifier = Modifier.widthIn(min = 52.dp),
            )
            Spacer(Modifier.width(4.dp))
        } else {
            // All-day: solid dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(indicatorColor),
            )
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text  = event.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AgendaCardPreview() {
    OnlyFlowsTheme {
        AgendaCard(
            events = MockData.todayAgendaEvents,
            modifier = Modifier.padding(16.dp),
        )
    }
}
