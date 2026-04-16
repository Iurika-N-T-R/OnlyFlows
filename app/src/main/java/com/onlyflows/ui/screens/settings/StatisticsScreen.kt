package com.onlyflows.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.DailyCount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit = {},
    viewModel: StatisticsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Statistiques",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                SectionLabel("Vue d'ensemble")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = "Projets",
                        value = state.projectCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Tâches actives",
                        value = state.activeTaskCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = "Complétées",
                        value = state.completedTaskCount.toString(),
                        tint  = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Abandonnées",
                        value = state.abandonedTaskCount.toString(),
                        tint  = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Tâches complétées")
                Spacer(Modifier.height(8.dp))

                // Period selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7 to "7 j", 30 to "30 j", 90 to "90 j").forEach { (days, label) ->
                        FilterChip(
                            selected = state.selectedPeriodDays == days,
                            onClick  = { viewModel.selectPeriod(days) },
                            label    = { Text(label) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                CompletionBarChart(
                    data       = state.dailyCompletions,
                    periodDays = state.selectedPeriodDays,
                    barColor   = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Stockage")
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment   = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Base de données",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text  = "%.2f Mo".format(state.dbSizeMb),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    tint: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val resolvedTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text  = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = resolvedTint,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompletionBarChart(
    data: List<DailyCount>,
    periodDays: Int,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        Box(
            modifier         = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Aucune donnée pour cette période",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Build a full date range filled with zeros
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val today     = LocalDate.now()
    val fullRange = (0 until periodDays).map { offset ->
        today.minusDays((periodDays - 1 - offset).toLong()).format(formatter)
    }
    val dataMap  = data.associate { it.date to it.count }
    val counts   = fullRange.map { date -> dataMap[date] ?: 0 }
    val maxCount = counts.maxOrNull()?.takeIf { it > 0 } ?: 1

    Canvas(modifier = modifier) {
        val barCount   = counts.size
        val totalWidth = size.width
        val totalHeight = size.height
        val barSpacing = 2.dp.toPx()
        val barWidth   = (totalWidth - barSpacing * (barCount - 1)) / barCount

        counts.forEachIndexed { index, count ->
            val barHeight = (count.toFloat() / maxCount) * totalHeight
            val x = index * (barWidth + barSpacing)
            val y = totalHeight - barHeight
            if (barHeight > 0f) {
                drawRect(
                    color   = barColor,
                    topLeft = Offset(x, y),
                    size    = Size(barWidth, barHeight),
                )
            }
        }
    }
}
