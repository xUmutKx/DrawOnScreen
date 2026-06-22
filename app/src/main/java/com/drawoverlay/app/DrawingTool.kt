package com.drawoverlay.app

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF

enum class DrawingTool {
    PEN,
    PENCIL,
    FOUNTAIN,
    BRUSH,
    CALLIGRAPHY,
    MARKER,
    CRAYON,
    GLOW,
    AIRBRUSH,
    CHARCOAL,
    ERASER,
    LINE,
    RECTANGLE,
    CIRCLE,
    ARROW,
    LASER
}

data class DrawingPath(
    val path: Path = Path(),
    val paint: Paint = Paint(),
    val tool: DrawingTool = DrawingTool.PEN,
    var startPoint: PointF? = null,
    var endPoint: PointF? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isShape() = tool in listOf(
        DrawingTool.LINE, DrawingTool.RECTANGLE,
        DrawingTool.CIRCLE, DrawingTool.ARROW
    )

    fun isFreehand() = tool in listOf(
        DrawingTool.PEN, DrawingTool.PENCIL, DrawingTool.FOUNTAIN,
        DrawingTool.BRUSH, DrawingTool.CALLIGRAPHY, DrawingTool.MARKER,
        DrawingTool.CRAYON, DrawingTool.GLOW, DrawingTool.AIRBRUSH,
        DrawingTool.CHARCOAL, DrawingTool.ERASER, DrawingTool.LASER
    )
}
