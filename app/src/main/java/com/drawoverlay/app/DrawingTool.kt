package com.drawoverlay.app

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF

enum class DrawingTool {
    // Pen types
    PEN,          // Ballpoint - thin, smooth
    PENCIL,       // Pencil - textured, pressure-sensitive
    FOUNTAIN,     // Fountain pen - variable width
    BRUSH,        // Brush - tapered strokes
    CALLIGRAPHY,  // Calligraphy - angled flat nib
    MARKER,       // Highlighter marker - thick, semi-transparent
    // Shape tools
    ERASER,
    LINE,
    RECTANGLE,
    CIRCLE,
    ARROW,
    // Special tools
    LASER,        // Laser pointer - fades out
    SCREENSHOT_CROP // Crop & overlay
}

data class DrawingPath(
    val path: Path = Path(),
    val paint: Paint = Paint(),
    val tool: DrawingTool = DrawingTool.PEN,
    var startPoint: PointF? = null,
    var endPoint: PointF? = null,
    val timestamp: Long = System.currentTimeMillis(), // for laser fade
    var alpha: Int = 255
) {
    fun isShape() = tool in listOf(
        DrawingTool.LINE, DrawingTool.RECTANGLE,
        DrawingTool.CIRCLE, DrawingTool.ARROW
    )
    fun isFreehand() = tool in listOf(
        DrawingTool.PEN, DrawingTool.PENCIL, DrawingTool.FOUNTAIN,
        DrawingTool.BRUSH, DrawingTool.CALLIGRAPHY, DrawingTool.MARKER,
        DrawingTool.ERASER, DrawingTool.LASER
    )
}
