package com.drawoverlay.app

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF

enum class DrawingTool {
    // Pen types
    PEN,          // Ballpoint - thin, smooth
    PENCIL,       // Pencil - textured, pressure-sensitive
    FOUNTAIN,     // Fountain pen (Dolma Kalem) - variable width based on velocity/pressure
    BRUSH,        // Brush - tapered strokes
    CALLIGRAPHY,  // Calligraphy (Hat Kalemi) - angled flat nib
    MARKER,       // Highlighter marker - thick, semi-transparent
    CRAYON,       // Pastel - textured edge
    GLOW,         // Neon/Glow - inner white, outer color
    AIRBRUSH,     // Spray - scattered dots
    CHARCOAL,     // Kömür - soft, wide, grainy

    // Shape tools
    ERASER,
    LINE,
    RECTANGLE,
    CIRCLE,
    ARROW,

    // Smart Tools
    RULER,        // Cetvel - Helper line
    PROTRACTOR,   // İletki - Angle/Circle helper

    // Special tools
    LASER,        // Laser pointer - fades out
    SCREENSHOT_CROP
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
        DrawingTool.CRAYON, DrawingTool.GLOW, DrawingTool.AIRBRUSH,
        DrawingTool.CHARCOAL, DrawingTool.ERASER, DrawingTool.LASER
    )
}
