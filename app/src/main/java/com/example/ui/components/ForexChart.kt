package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.forex.PricePoint
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BearishRed
import com.example.ui.theme.GridLineSlate
import com.example.ui.theme.TextGrey
import com.example.ui.theme.ElectricBlue
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ForexChart(
    history: List<PricePoint>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No historical price points", color = TextGrey)
        }
        return
    }

    var chartType by remember { mutableStateOf("Candlestick & Line") } // "Line Only", "Candlestick Only", "Candlestick & Line"
    var timeframe by remember { mutableStateOf("30 Days") } // "7 Days", "15 Days", "30 Days"

    val filteredHistory = remember(history, timeframe) {
        val count = when (timeframe) {
            "7 Days" -> 7
            "15 Days" -> 15
            else -> 30
        }
        history.takeLast(count)
    }

    val gridColor = GridLineSlate
    val accentColor = ElectricBlue

    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = TextGrey, fontSize = 10.sp)

    // Touch/Drag state for interactive tooltip
    var dragX by remember { mutableStateOf<Float?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val prices = filteredHistory.map { it.close }
    val maxPrice = filteredHistory.maxOf { it.high }
    val minPrice = filteredHistory.minOf { it.low }
    val priceRange = (maxPrice - minPrice).let { if (it == 0.0) 1.0 else it }

    val paddingPercent = 0.15

    val simpleDateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Column(modifier = modifier) {
        // Controls Row for premium user interaction
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Timeframe Selector (7D, 15D, 30D)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("7 Days", "15 Days", "30 Days").forEach { tf ->
                    val isSel = timeframe == tf
                    TextButton(
                        onClick = {
                            timeframe = tf
                            dragX = null
                            selectedIndex = null
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isSel) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (isSel) accentColor else TextGrey
                        )
                    ) {
                        Text(tf.substringBefore(" ") + "d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Chart Mode Selector
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "Line Only" to "Line",
                    "Candlestick Only" to "Candle",
                    "Candlestick & Line" to "Both"
                ).forEach { (typeKey, typeLabel) ->
                    val isSel = chartType == typeKey
                    TextButton(
                        onClick = { chartType = typeKey },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isSel) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (isSel) accentColor else TextGrey
                        )
                    ) {
                        Text(typeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(filteredHistory) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragX = offset.x
                        },
                        onDragEnd = {
                            dragX = null
                            selectedIndex = null
                        },
                        onDragCancel = {
                            dragX = null
                            selectedIndex = null
                        },
                        onDrag = { change, _ ->
                            dragX = change.position.x
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            val leftPadding = 10.dp.toPx()
            val rightPadding = 60.dp.toPx() // Room for price labels on the right
            val topPadding = (height * paddingPercent).toFloat()
            val bottomPadding = 30.dp.toPx() // Room for date labels at the bottom

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding

            // 1. Draw Grid Lines (Horizontal representing prices)
            val gridCount = 4
            for (i in 0..gridCount) {
                val ratio = i.toDouble() / gridCount
                val y = topPadding + chartHeight * (1f - ratio).toFloat()
                
                // Grid line
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(leftPadding + chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Price text label
                val priceVal = minPrice + (priceRange * ratio)
                val priceLabel = String.format("%.4f", priceVal)
                val textLayoutResult = textMeasurer.measure(priceLabel, style = textStyle)
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(leftPadding + chartWidth + 5.dp.toPx(), y - textLayoutResult.size.height / 2)
                )
            }

            // 2. Plot Candle / Line Data
            val pointsCount = filteredHistory.size
            val stepX = chartWidth / (pointsCount - 1).coerceAtLeast(1)

            val linePath = Path()
            val areaPath = Path()
            
            filteredHistory.forEachIndexed { index, point ->
                val x = leftPadding + (index * stepX)
                
                // Map price to coordinate system
                val yClose = topPadding + chartHeight * (1f - ((point.close - minPrice) / priceRange)).toFloat()
                val yOpen = topPadding + chartHeight * (1f - ((point.open - minPrice) / priceRange)).toFloat()
                val yHigh = topPadding + chartHeight * (1f - ((point.high - minPrice) / priceRange)).toFloat()
                val yLow = topPadding + chartHeight * (1f - ((point.low - minPrice) / priceRange)).toFloat()

                // Draw candlesticks if enabled
                if (chartType.contains("Candlestick")) {
                    val candleColor = if (point.close >= point.open) BullishGreen else BearishRed
                    drawLine(
                        color = candleColor.copy(alpha = 0.5f),
                        start = Offset(x, yHigh),
                        end = Offset(x, yLow),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    val bodyWidth = (stepX * 0.5f).coerceIn(4f, 20f)
                    val topBody = minOf(yOpen, yClose)
                    val bottomBody = maxOf(yOpen, yClose)
                    val bodyHeight = (bottomBody - topBody).coerceAtLeast(1.5f)

                    drawRect(
                        color = candleColor,
                        topLeft = Offset(x - bodyWidth / 2, topBody),
                        size = Size(bodyWidth, bodyHeight)
                    )
                }

                // Add to smooth trendlines
                if (index == 0) {
                    linePath.moveTo(x, yClose)
                    areaPath.moveTo(x, yClose)
                } else {
                    linePath.lineTo(x, yClose)
                    areaPath.lineTo(x, yClose)
                }
            }

            // Close area path to create modern gradient background fill
            if (chartType.contains("Line") && filteredHistory.isNotEmpty()) {
                val lastX = leftPadding + ((filteredHistory.size - 1) * stepX)
                val firstX = leftPadding
                areaPath.lineTo(lastX, topPadding + chartHeight)
                areaPath.lineTo(firstX, topPadding + chartHeight)
                areaPath.close()

                drawPath(
                    path = areaPath,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            accentColor.copy(alpha = 0.00f)
                        ),
                        startY = topPadding,
                        endY = topPadding + chartHeight
                    )
                )

                // Draw crisp high-fidelity trendline
                drawPath(
                    path = linePath,
                    color = accentColor,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Draw X-Axis dates (e.g., start, middle, end)
            val datesToDraw = listOf(0, pointsCount / 2, pointsCount - 1)
            datesToDraw.forEach { idx ->
                if (idx < filteredHistory.size) {
                    val p = filteredHistory[idx]
                    val x = leftPadding + (idx * stepX)
                    val dateLabel = simpleDateFormat.format(Date(p.timestamp))
                    val textLayout = textMeasurer.measure(dateLabel, style = textStyle)
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(x - textLayout.size.width / 2, height - bottomPadding + 5.dp.toPx())
                    )
                }
            }

            // 3. Draw Scrubber Ruler on Drag
            dragX?.let { xVal ->
                val clampedX = xVal.coerceIn(leftPadding, leftPadding + chartWidth)
                val index = (((clampedX - leftPadding) / chartWidth) * (pointsCount - 1)).toInt().coerceIn(0, pointsCount - 1)
                selectedIndex = index

                val finalScrubberX = leftPadding + (index * stepX)
                
                // Draw vertical line with dynamic primary indicator line
                drawLine(
                    color = accentColor.copy(alpha = 0.6f),
                    start = Offset(finalScrubberX, topPadding),
                    end = Offset(finalScrubberX, height - bottomPadding),
                    strokeWidth = 1.dp.toPx()
                )

                // Highlight price dot
                val selPoint = filteredHistory[index]
                val dotY = topPadding + chartHeight * (1f - ((selPoint.close - minPrice) / priceRange)).toFloat()
                
                // Outline
                drawCircle(
                    color = accentColor,
                    radius = 6.dp.toPx(),
                    center = Offset(finalScrubberX, dotY)
                )
                // Innards
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(finalScrubberX, dotY)
                )
            }
        }

        // Selected interactive details text overlay
        val idx = selectedIndex
        if (idx != null && idx < filteredHistory.size) {
            val point = filteredHistory[idx]
            val dateStr = simpleDateFormat.format(Date(point.timestamp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Date: $dateStr", color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "O: ${String.format("%.4f", point.open)}  H: ${String.format("%.4f", point.high)}  L: ${String.format("%.4f", point.low)}  C: ${String.format("%.4f", point.close)}",
                    color = if (point.close >= point.open) BullishGreen else BearishRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Default legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Hold & drag to trace prices", color = TextGrey, fontSize = 10.sp)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = BullishGreen, radius = 3.dp.toPx())
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bullish", color = TextGrey, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(8.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = BearishRed, radius = 3.dp.toPx())
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bearish", color = TextGrey, fontSize = 10.sp)
                }
            }
        }
    }
}
