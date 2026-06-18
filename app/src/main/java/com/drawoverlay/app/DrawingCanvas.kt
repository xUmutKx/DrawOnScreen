package com.drawoverlay.app

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class DrawingCanvas(context: Context) : View(context) {

    // ── State ────────────────────────────────────────────────────────────────
    private val finishedPaths = mutableListOf<DrawingPath>()
    private val undoStack     = mutableListOf<DrawingPath>()
    private var currentPath: DrawingPath? = null

    var currentTool   : DrawingTool = DrawingTool.PEN
    var currentColor  : Int = Color.WHITE
    var strokeWidth   : Float = 8f
    var opacity       : Int = 255

    // Stylus button behavior setting
    var stylusButtonAction: StylusButtonAction = StylusButtonAction.ERASER

    // Shape preview
    private var previewStart = PointF()
    private var previewEnd   = PointF()
    private var isDrawingShape = false

    // Laser paths with fade handler
    private val laserPaths = mutableListOf<DrawingPath>()
    private val laserHandler = Handler(Looper.getMainLooper())
    private val LASER_FADE_MS = 1500L

    // Previous tool (for stylus button toggle)
    private var previousTool: DrawingTool = DrawingTool.PEN
    private var stylusButtonHeld = false

    // Bitmap layer
    private var canvasBitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    // Pencil texture noise
    private val random = java.util.Random(42)

    private val previewPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmapCanvas = Canvas(canvasBitmap!!)
        redrawAll()
    }

    // ── Paint factory ────────────────────────────────────────────────────────
    private fun buildPaint(tool: DrawingTool = currentTool, pressureScale: Float = 1f): Paint {
        val p = Paint().apply {
            isAntiAlias = true
            strokeCap   = Paint.Cap.ROUND
            strokeJoin  = Paint.Join.ROUND
            color       = currentColor
            alpha       = opacity
        }
        when (tool) {
            DrawingTool.PEN -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * pressureScale
            }
            DrawingTool.PENCIL -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * pressureScale * 0.8f
                p.alpha       = (opacity * 0.85f * pressureScale).toInt().coerceIn(30, 255)
                p.maskFilter  = BlurMaskFilter(1.5f, BlurMaskFilter.Blur.NORMAL)
            }
            DrawingTool.FOUNTAIN -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * pressureScale * 1.5f
                p.strokeCap   = Paint.Cap.BUTT
            }
            DrawingTool.BRUSH -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * pressureScale * 2.5f
                p.alpha       = (opacity * 0.8f).toInt()
                p.maskFilter  = BlurMaskFilter(strokeWidth * 0.4f, BlurMaskFilter.Blur.NORMAL)
            }
            DrawingTool.CALLIGRAPHY -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * pressureScale
                p.strokeCap   = Paint.Cap.SQUARE
                p.strokeJoin  = Paint.Join.MITER
            }
            DrawingTool.MARKER -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * 4f
                p.alpha       = (opacity * 0.45f).toInt()
                p.strokeCap   = Paint.Cap.SQUARE
            }
            DrawingTool.ERASER -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * 4f
                p.xfermode    = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            DrawingTool.LASER -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * 2f
                p.color       = Color.RED
                p.alpha       = 220
                p.maskFilter  = BlurMaskFilter(strokeWidth * 1.5f, BlurMaskFilter.Blur.NORMAL)
            }
            DrawingTool.LINE, DrawingTool.RECTANGLE,
            DrawingTool.CIRCLE, DrawingTool.ARROW -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth
            }
            else -> {
                p.style       = Paint.Style.STROKE
                p.strokeWidth = strokeWidth
            }
        }
        return p
    }

    // ── Touch handling ───────────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.getX(0)
        val y = event.getY(0)

        // Pressure for stylus
        val pressure = if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS)
            event.getPressure(0).coerceIn(0.1f, 1.0f) else 1f

        val isShape = currentTool in listOf(
            DrawingTool.LINE, DrawingTool.RECTANGLE,
            DrawingTool.CIRCLE, DrawingTool.ARROW
        )

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isShape) {
                    previewStart.set(x, y)
                    previewEnd.set(x, y)
                    isDrawingShape = true
                    previewPaint.set(buildPaint())
                } else {
                    val dp = DrawingPath(tool = currentTool)
                    dp.paint.set(buildPaint(pressureScale = pressure))
                    dp.path.moveTo(x, y)
                    currentPath = dp
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isShape && isDrawingShape) {
                    previewEnd.set(x, y)
                    invalidate()
                } else {
                    currentPath?.let { dp ->
                        val histSize = event.historySize
                        for (i in 0 until histSize) {
                            val hx = event.getHistoricalX(0, i)
                            val hy = event.getHistoricalY(0, i)
                            // Pencil texture: random micro offsets
                            if (dp.tool == DrawingTool.PENCIL) {
                                addPencilTexture(bitmapCanvas!!, hx, hy, dp.paint)
                            }
                            dp.path.lineTo(hx, hy)
                        }
                        if (dp.tool == DrawingTool.PENCIL) {
                            addPencilTexture(bitmapCanvas!!, x, y, dp.paint)
                        }
                        dp.path.lineTo(x, y)
                        bitmapCanvas?.drawPath(dp.path, dp.paint)
                        dp.path.reset()
                        dp.path.moveTo(x, y)
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isShape && isDrawingShape) {
                    isDrawingShape = false
                    val dp = DrawingPath(
                        tool = currentTool,
                        startPoint = PointF(previewStart.x, previewStart.y),
                        endPoint   = PointF(previewEnd.x, previewEnd.y)
                    )
                    dp.paint.set(buildPaint())
                    drawShapeOnto(bitmapCanvas!!, dp)
                    finishedPaths.add(dp)
                    undoStack.clear()
                    invalidate()
                } else {
                    currentPath?.let { dp ->
                        if (dp.tool == DrawingTool.LASER) {
                            laserPaths.add(dp)
                            scheduleLaserFade(dp)
                        } else {
                            finishedPaths.add(dp)
                            undoStack.clear()
                        }
                        currentPath = null
                        invalidate()
                    }
                }
            }

            // Stylus button
            MotionEvent.ACTION_BUTTON_PRESS -> {
                val btn = event.buttonState
                if (btn and MotionEvent.BUTTON_STYLUS_PRIMARY != 0 && !stylusButtonHeld) {
                    stylusButtonHeld = true
                    previousTool = currentTool
                    currentTool = when (stylusButtonAction) {
                        StylusButtonAction.ERASER -> DrawingTool.ERASER
                        StylusButtonAction.LASER  -> DrawingTool.LASER
                        StylusButtonAction.MARKER -> DrawingTool.MARKER
                    }
                }
            }
            MotionEvent.ACTION_BUTTON_RELEASE -> {
                if (stylusButtonHeld) {
                    stylusButtonHeld = false
                    currentTool = previousTool
                }
            }
        }
        return true
    }

    private fun addPencilTexture(c: Canvas, x: Float, y: Float, basePaint: Paint) {
        val count = 3
        val noiseRange = strokeWidth * 0.6f
        val dotPaint = Paint(basePaint).apply {
            strokeWidth = 1f
            alpha = (basePaint.alpha * 0.4f).toInt()
        }
        repeat(count) {
            val nx = x + (random.nextFloat() - 0.5f) * noiseRange
            val ny = y + (random.nextFloat() - 0.5f) * noiseRange
            c.drawPoint(nx, ny, dotPaint)
        }
    }

    // ── Laser fade ───────────────────────────────────────────────────────────
    private fun scheduleLaserFade(dp: DrawingPath) {
        laserHandler.postDelayed({
            laserPaths.remove(dp)
            invalidate()
        }, LASER_FADE_MS)
    }

    // ── Draw ─────────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Draw laser paths on top (not in bitmap)
        for (lp in laserPaths) {
            val age = System.currentTimeMillis() - lp.timestamp
            val fadeAlpha = ((1f - age.toFloat() / LASER_FADE_MS) * 220f).toInt().coerceIn(0, 220)
            val fadePaint = Paint(lp.paint).apply { alpha = fadeAlpha }
            canvas.drawPath(lp.path, fadePaint)
        }

        // Shape preview
        if (isDrawingShape) {
            val tempDp = DrawingPath(
                tool = currentTool,
                startPoint = PointF(previewStart.x, previewStart.y),
                endPoint   = PointF(previewEnd.x, previewEnd.y)
            )
            tempDp.paint.set(previewPaint)
            drawShapeOnto(canvas, tempDp)
        }
    }

    private fun redrawAll() {
        bitmapCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        for (dp in finishedPaths) {
            if (dp.isShape()) drawShapeOnto(bitmapCanvas!!, dp)
            else bitmapCanvas?.drawPath(dp.path, dp.paint)
        }
    }

    private fun drawShapeOnto(c: Canvas, dp: DrawingPath) {
        val s = dp.startPoint ?: return
        val e = dp.endPoint ?: return
        when (dp.tool) {
            DrawingTool.LINE      -> c.drawLine(s.x, s.y, e.x, e.y, dp.paint)
            DrawingTool.RECTANGLE -> c.drawRect(s.x, s.y, e.x, e.y, dp.paint)
            DrawingTool.CIRCLE    -> {
                val cx = (s.x + e.x) / 2f; val cy = (s.y + e.y) / 2f
                val rx = abs(e.x - s.x) / 2f; val ry = abs(e.y - s.y) / 2f
                c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, dp.paint)
            }
            DrawingTool.ARROW     -> drawArrow(c, s.x, s.y, e.x, e.y, dp.paint)
            else -> {}
        }
    }

    private fun drawArrow(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, p: Paint) {
        c.drawLine(x1, y1, x2, y2, p)
        val angle  = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        val len    = 40f
        val spread = Math.PI / 6
        c.drawLine(x2, y2, (x2 - len * cos(angle - spread)).toFloat(), (y2 - len * sin(angle - spread)).toFloat(), p)
        c.drawLine(x2, y2, (x2 - len * cos(angle + spread)).toFloat(), (y2 - len * sin(angle + spread)).toFloat(), p)
    }

    // ── Public API ───────────────────────────────────────────────────────────
    fun undo() {
        if (finishedPaths.isNotEmpty()) {
            undoStack.add(finishedPaths.removeLast())
            redrawAll()
            invalidate()
        }
    }

    fun redo() {
        if (undoStack.isNotEmpty()) {
            finishedPaths.add(undoStack.removeLast())
            redrawAll()
            invalidate()
        }
    }

    fun clearAll() {
        finishedPaths.clear()
        undoStack.clear()
        laserPaths.clear()
        bitmapCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun getBitmap(): Bitmap? = canvasBitmap
    fun hasContent(): Boolean = finishedPaths.isNotEmpty()

    fun overlayBitmap(bmp: Bitmap) {
        bitmapCanvas?.drawBitmap(bmp, 0f, 0f, null)
        invalidate()
    }
}

enum class StylusButtonAction { ERASER, LASER, MARKER }
