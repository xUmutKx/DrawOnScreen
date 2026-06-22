package com.drawoverlay.app

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class DrawingCanvas(context: Context, private val prefs: AppPrefs) : View(context) {

    private val finishedPaths = mutableListOf<DrawingPath>()
    private val undoStack     = mutableListOf<DrawingPath>()
    private var currentPath: DrawingPath? = null

    var currentTool   : DrawingTool = try { DrawingTool.valueOf(prefs.defaultTool) } catch (_: Exception) { DrawingTool.PEN }
    var currentColor  : Int  = prefs.defaultColor
    var strokeWidth   : Float = prefs.defaultStroke
    var opacity       : Int  = 255
    var stylusButtonAction: StylusButtonAction = StylusButtonAction.ERASER

    private var previewStart = PointF(); private var previewEnd = PointF()
    private var isDrawingShape = false

    private val laserPaths   = mutableListOf<DrawingPath>()
    private val laserHandler = Handler(Looper.getMainLooper())
    private val LASER_FADE_MS = 1500L

    private var previousTool: DrawingTool = DrawingTool.PEN
    private var stylusButtonHeld = false

    // Ruler/Smart tool state
    private var rulerMatrix = Matrix()
    private var rulerBounds = RectF(0f, 0f, 800f, 150f)
    private var rulerAngle = 0f
    private var isDraggingRuler = false

    private var canvasBitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    private val random = java.util.Random(42)
    private val previewPaint = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }

    // Velocity tracking for Fountain Pen
    private var lastX = 0f
    private var lastY = 0f
    private var lastVelocity = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(canvasBitmap!!)
            redrawAll()
        }
    }

    private fun buildPaint(tool: DrawingTool = currentTool, pressureScale: Float = 1f, velocityScale: Float = 1f, dx: Float = 0f, dy: Float = 0f): Paint {
        val p = Paint().apply { isAntiAlias = true; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = currentColor; alpha = opacity }
        when (tool) {
            DrawingTool.PEN         -> { p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * pressureScale }
            DrawingTool.PENCIL      -> { 
                p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * pressureScale * 0.8f
                p.alpha = (opacity * 0.85f * pressureScale).toInt().coerceIn(30,255)
                p.maskFilter = BlurMaskFilter(1.2f, BlurMaskFilter.Blur.NORMAL)
            }
            DrawingTool.FOUNTAIN    -> { 
                // Dolma Kalem: Width depends on pressure, velocity and angle
                val angle = atan2(dy.toDouble(), dx.toDouble())
                val angleFactor = (0.7f + 0.6f * abs(sin(angle + Math.PI/4))).toFloat()
                val vFactor = (1.2f - (velocityScale * 0.3f)).coerceIn(0.7f, 1.3f)
                
                p.style = Paint.Style.STROKE
                p.strokeWidth = strokeWidth * pressureScale * vFactor * angleFactor
                p.strokeCap = Paint.Cap.BUTT
                if (prefs.inkBleeding) p.maskFilter = BlurMaskFilter(1.2f, BlurMaskFilter.Blur.NORMAL)
            }
            DrawingTool.BRUSH       -> { 
                p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * pressureScale * 2.5f
                p.alpha = (opacity * 0.8f).toInt()
                p.maskFilter = BlurMaskFilter(strokeWidth * 0.3f, BlurMaskFilter.Blur.NORMAL)
            }
            DrawingTool.CALLIGRAPHY -> { 
                p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * pressureScale
                p.strokeCap = Paint.Cap.SQUARE; p.strokeJoin = Paint.Join.MITER
                // Angled nib effect is usually done by drawing a path with a specific orientation
            }
            DrawingTool.MARKER      -> { p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * 4f; p.alpha = (opacity * 0.45f).toInt(); p.strokeCap = Paint.Cap.SQUARE }
            DrawingTool.CRAYON      -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * 2f
                p.alpha = (opacity * 0.9f).toInt()
                p.pathEffect = DiscretePathEffect(10f, 4f)
            }
            DrawingTool.GLOW        -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * 1.5f
                p.setShadowLayer(strokeWidth, 0f, 0f, currentColor)
            }
            DrawingTool.AIRBRUSH    -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * 5f
                p.alpha = (opacity * 0.3f).toInt()
                p.maskFilter = BlurMaskFilter(strokeWidth * 1.5f, BlurMaskFilter.Blur.NORMAL)
            }
            DrawingTool.CHARCOAL    -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * 3f
                p.alpha = (opacity * 0.6f).toInt()
                p.maskFilter = BlurMaskFilter(strokeWidth * 0.8f, BlurMaskFilter.Blur.NORMAL)
                p.pathEffect = DiscretePathEffect(5f, 5f)
            }
            DrawingTool.ERASER      -> { p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * 4f; p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
            DrawingTool.LASER       -> { p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth * 2f; p.color = Color.RED; p.alpha = 220; p.maskFilter = BlurMaskFilter(strokeWidth * 1.5f, BlurMaskFilter.Blur.NORMAL) }
            else -> { p.style = Paint.Style.STROKE; p.strokeWidth = strokeWidth }
        }
        return p
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)
        if (prefs.stylusOnly && toolType != MotionEvent.TOOL_TYPE_STYLUS) return false
        if (!prefs.fingerDraw && toolType == MotionEvent.TOOL_TYPE_FINGER) return false

        val x = event.getX(0); val y = event.getY(0)
        val pressure = if (toolType == MotionEvent.TOOL_TYPE_STYLUS && prefs.pressureSensitive) event.getPressure(0).coerceIn(0.1f, 1.0f) else 1f
        
        // Velocity calculation
        val dx = x - lastX; val dy = y - lastY
        val dist = sqrt(dx*dx + dy*dy)
        lastVelocity = (dist * 0.3f + lastVelocity * 0.7f).coerceIn(0f, 50f)
        val vScale = (lastVelocity / 50f)

        val isShape = currentTool in listOf(DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.CIRCLE, DrawingTool.ARROW)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x; lastY = y
                if (isShape) { 
                    previewStart.set(x,y); previewEnd.set(x,y); isDrawingShape=true
                    previewPaint.set(buildPaint(pressureScale=pressure)) 
                } else { 
                    val dp = DrawingPath(tool=currentTool)
                    dp.paint.set(buildPaint(pressureScale=pressure, velocityScale=vScale, dx=0f, dy=0f))
                    dp.path.moveTo(x,y); currentPath=dp 
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isShape && isDrawingShape) { previewEnd.set(x,y); invalidate() }
                else { currentPath?.let { dp ->
                    for (i in 0 until event.historySize) {
                        val hx=event.getHistoricalX(0,i); val hy=event.getHistoricalY(0,i)
                        val hdx = hx - lastX; val hdy = hy - lastY
                        if (dp.tool==DrawingTool.PENCIL) addPencilTexture(bitmapCanvas!!,hx,hy,dp.paint)
                        if (dp.tool==DrawingTool.AIRBRUSH) addAirbrushTexture(bitmapCanvas!!,hx,hy,dp.paint)
                        
                        // Dynamic width for fountain pen
                        if (dp.tool == DrawingTool.FOUNTAIN) {
                            val p = buildPaint(dp.tool, pressure, vScale, hdx, hdy)
                            bitmapCanvas?.drawLine(lastX, lastY, hx, hy, p)
                        } else {
                            dp.path.lineTo(hx,hy)
                        }
                        lastX = hx; lastY = hy
                    }
                    
                    val curDx = x - lastX; val curDy = y - lastY
                    if (dp.tool==DrawingTool.PENCIL) addPencilTexture(bitmapCanvas!!,x,y,dp.paint)
                    if (dp.tool==DrawingTool.AIRBRUSH) addAirbrushTexture(bitmapCanvas!!,x,y,dp.paint)
                    
                    if (dp.tool == DrawingTool.FOUNTAIN) {
                        val p = buildPaint(dp.tool, pressure, vScale, curDx, curDy)
                        bitmapCanvas?.drawLine(lastX, lastY, x, y, p)
                    } else {
                        dp.path.lineTo(x,y)
                        bitmapCanvas?.drawPath(dp.path,dp.paint)
                        dp.path.reset(); dp.path.moveTo(x,y)
                    }
                    invalidate()
                }}
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isShape && isDrawingShape) {
                    isDrawingShape=false
                    val dp = DrawingPath(tool=currentTool, startPoint=PointF(previewStart.x,previewStart.y), endPoint=PointF(previewEnd.x,previewEnd.y))
                    dp.paint.set(buildPaint()); drawShapeOnto(bitmapCanvas!!,dp); finishedPaths.add(dp); undoStack.clear(); invalidate()
                } else {
                    currentPath?.let { dp ->
                        if (dp.tool==DrawingTool.LASER) { laserPaths.add(dp); scheduleLaserFade(dp) }
                        else { finishedPaths.add(dp); undoStack.clear() }
                        currentPath=null; invalidate()
                    }
                }
            }
            MotionEvent.ACTION_BUTTON_PRESS -> {
                if (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0 && !stylusButtonHeld) {
                    stylusButtonHeld=true; previousTool=currentTool
                    currentTool = when(stylusButtonAction) { StylusButtonAction.LASER->DrawingTool.LASER; StylusButtonAction.MARKER->DrawingTool.MARKER; else->DrawingTool.ERASER }
                }
            }
            MotionEvent.ACTION_BUTTON_RELEASE -> { if (stylusButtonHeld) { stylusButtonHeld=false; currentTool=previousTool } }
        }
        return true
    }

    private fun addPencilTexture(c: Canvas, x: Float, y: Float, basePaint: Paint) {
        val dotPaint = Paint(basePaint).apply { strokeWidth=1f; alpha=(basePaint.alpha*0.4f).toInt() }
        repeat(3) { c.drawPoint(x+(random.nextFloat()-0.5f)*strokeWidth*0.6f, y+(random.nextFloat()-0.5f)*strokeWidth*0.6f, dotPaint) }
    }

    private fun addAirbrushTexture(c: Canvas, x: Float, y: Float, basePaint: Paint) {
        val dotPaint = Paint(basePaint).apply { strokeWidth=1f; alpha=(basePaint.alpha*0.2f).toInt() }
        repeat(15) {
            val r = random.nextFloat() * strokeWidth * 2f
            val a = random.nextFloat() * 2 * Math.PI
            c.drawPoint(x + r * cos(a).toFloat(), y + r * sin(a).toFloat(), dotPaint)
        }
    }

    private fun scheduleLaserFade(dp: DrawingPath) { laserHandler.postDelayed({ laserPaths.remove(dp); invalidate() }, LASER_FADE_MS) }

    override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        
        if (prefs.showRuler) {
            drawRuler(canvas)
        }

        for (lp in laserPaths) {
            val age = System.currentTimeMillis() - lp.timestamp
            val a = ((1f - age.toFloat()/LASER_FADE_MS)*220f).toInt().coerceIn(0,220)
            canvas.drawPath(lp.path, Paint(lp.paint).apply { alpha=a })
        }
        if (isDrawingShape) {
            val tmp = DrawingPath(tool=currentTool, startPoint=PointF(previewStart.x,previewStart.y), endPoint=PointF(previewEnd.x,previewEnd.y))
            tmp.paint.set(previewPaint); drawShapeOnto(canvas,tmp)
        }
        currentPath?.let { dp ->
            if (!dp.path.isEmpty) canvas.drawPath(dp.path, dp.paint)
        }
    }

    private fun drawRuler(canvas: Canvas) {
        canvas.save()
        canvas.translate(prefs.rulerX, prefs.rulerY)
        canvas.rotate(prefs.rulerRotation)
        
        // Ruler body with texture (gradient/wood-like)
        val bodyPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, 150f, 0xFFE0C090.toInt(), 0xFFC0A070.toInt(), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
            alpha = 200
        }
        canvas.drawRoundRect(0f, 0f, 800f, 150f, 10f, 10f, bodyPaint)
        
        // Edge highlights
        val edgePaint = Paint().apply {
            color = 0xFF504030.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(0f, 0f, 800f, 150f, 10f, 10f, edgePaint)
        
        // Markings (cm/mm)
        val markPaint = Paint().apply { color = Color.BLACK; strokeWidth = 1f; textSize = 20sp }
        for (i in 0..80) {
            val x = i * 10f
            val h = if (i % 10 == 0) 40f else if (i % 5 == 0) 25f else 15f
            canvas.drawLine(x, 0f, x, h, markPaint)
            if (i % 10 == 0) canvas.drawText("${i/10}", x + 2, 60f, markPaint)
        }
        
        canvas.restore()
    }

    // Call this from floating toolbar or touch logic to handle ruler dragging
    private fun handleRulerTouch(x: Float, y: Float, action: Int): Boolean {
        // Basic hit test and drag logic for ruler could go here
        // For brevity in this turn, I'll focus on the drawing tools
        return false
    }

    private fun redrawAll() {
        bitmapCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        for (dp in finishedPaths) { if (dp.isShape()) drawShapeOnto(bitmapCanvas!!,dp) else bitmapCanvas?.drawPath(dp.path,dp.paint) }
    }

    private fun drawShapeOnto(c: Canvas, dp: DrawingPath) {
        val s=dp.startPoint?:return; val e=dp.endPoint?:return
        when (dp.tool) {
            DrawingTool.LINE      -> c.drawLine(s.x,s.y,e.x,e.y,dp.paint)
            DrawingTool.RECTANGLE -> c.drawRect(s.x,s.y,e.x,e.y,dp.paint)
            DrawingTool.CIRCLE    -> { val cx=(s.x+e.x)/2f; val cy=(s.y+e.y)/2f; val rx=abs(e.x-s.x)/2f; val ry=abs(e.y-s.y)/2f; c.drawOval(cx-rx,cy-ry,cx+rx,cy+ry,dp.paint) }
            DrawingTool.ARROW     -> drawArrow(c,s.x,s.y,e.x,e.y,dp.paint)
            else -> {}
        }
    }

    private fun drawArrow(c: Canvas, x1:Float, y1:Float, x2:Float, y2:Float, p:Paint) {
        c.drawLine(x1,y1,x2,y2,p)
        val angle=atan2((y2-y1).toDouble(),(x2-x1).toDouble()); val len=40f; val spread=Math.PI/6
        c.drawLine(x2,y2,(x2-len*cos(angle-spread)).toFloat(),(y2-len*sin(angle-spread)).toFloat(),p)
        c.drawLine(x2,y2,(x2-len*cos(angle+spread)).toFloat(),(y2-len*sin(angle+spread)).toFloat(),p)
    }

    fun undo()  { if (finishedPaths.isNotEmpty()) { undoStack.add(finishedPaths.removeLast()); redrawAll(); invalidate() } }
    fun redo()  { if (undoStack.isNotEmpty()) { finishedPaths.add(undoStack.removeLast()); redrawAll(); invalidate() } }
    fun clearAll() { finishedPaths.clear(); undoStack.clear(); laserPaths.clear(); bitmapCanvas?.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR); invalidate() }
    fun getBitmap(): Bitmap? = canvasBitmap
    fun overlayBitmap(bmp: Bitmap) { bitmapCanvas?.drawBitmap(bmp,0f,0f,null); invalidate() }
}

enum class StylusButtonAction { ERASER, LASER, MARKER }
