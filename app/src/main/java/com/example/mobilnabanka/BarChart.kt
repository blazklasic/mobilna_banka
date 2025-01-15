package com.example.mobilnabanka

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

data class BarData(val category: String, val value: Double)

@Composable
fun BarChart(depositSum: Double, withdrawalSum: Double, modifier: Modifier = Modifier) {
    val data = listOf(
        BarData("Pologi", depositSum),
        BarData("Dvigi", withdrawalSum)
    )

    Canvas(modifier = modifier) {
        val maxValue = data.maxOfOrNull { it.value } ?: 1.0
        val barWidth = size.width / (data.size * 2)
        val spaceBetweenBars = barWidth

        data.forEachIndexed { index, item ->
            val barHeight = size.height * (item.value / maxValue)
            val barLeft = index * (barWidth * 2)
            val barTop = size.height - barHeight

            drawRect(
                color = if (item.category == "Pologi") Color.Green else Color.Red,
                topLeft = Offset(barLeft, barTop.toFloat()),
                size = Size(barWidth, barHeight.toFloat())
            )

            drawIntoCanvas { canvas ->
                val paint = android.text.TextPaint().apply {
                    color = Color.Black.toArgb()
                    textSize = 40f
                }

                canvas.nativeCanvas.drawText(
                    item.category,
                    barLeft + barWidth / 2,
                    size.height - 10f,
                    paint
                )
            }
        }
    }
}