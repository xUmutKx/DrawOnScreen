package com.drawoverlay.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider

class FloatingToolbar(
    private val context: Context,
    private val wm: WindowManager,
    private val canvas: DrawingCanvas,
    private val onClose: () -> Unit,
    private val onTogglePassThrough: (Boolean) -> Unit,
    private val onScreenshot: () -> Unit
) {

    private val root: View = LayoutInflater.from(context)
        .inflate(R.layout.layout_toolbar, null)

    private var isPassThrough = false
    private var isExpanded    = true
    private var isSpotlight   = false
    private var spotlightView: View? = null

    // Spotlight overlay
    private val spotlightParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0; y = 200
    }

    private var dragStartX = 0f; private var dragStartY = 0f
    private var winStartX  = 0 ; private var winStartY  = 0

    // Crop screenshot state
    private var screenshotBmp: Bitmap? = null
    private var cropOverlay: View? = null

    private val colorPalette = listOf(
        Color.WHITE, Color.BLACK,
        0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(),
        0xFFFFFF00.toInt(), 0xFFFF8800.toInt(), 0xFFFF00FF.toInt(),
        0xFF00FFFF.toInt(), 0xFF888888.toInt()
    )

    init {
        setupDrag()
        setupButtons()
    }

    private fun setupDrag() {
        val handle = root.findViewById<View>(R.id.dragHandle)
        handle.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = ev.rawX; dragStartY = ev.rawY
                    winStartX  = params.x; winStartY  = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (winStartX + (ev.rawX - dragStartX)).toInt()
                    params.y = (winStartY + (ev.rawY - dragStartY)).toInt()
                    wm.updateViewLayout(root, params)
                }
            }
            true
        }
    }

    private fun setupButtons() {
        // Collapse/expand
        root.findViewById<ImageButton>(R.id.btnCollapse).setOnClickListener {
            isExpanded = !isExpanded
            root.findViewById<LinearLayout>(R.id.toolsContainer).visibility =
                if (isExpanded) View.VISIBLE else View.GONE
            root.findViewById<ImageButton>(R.id.btnCollapse)
                .setImageResource(if (isExpanded) R.drawable.ic_collapse else R.drawable.ic_expand)
        }

        // Pen type buttons
        val toolMap = mapOf(
            R.id.btnPen         to DrawingTool.PEN,
            R.id.btnPencil      to DrawingTool.PENCIL,
            R.id.btnFountain    to DrawingTool.FOUNTAIN,
            R.id.btnBrush       to DrawingTool.BRUSH,
            R.id.btnCalligraphy to DrawingTool.CALLIGRAPHY,
            R.id.btnMarker      to DrawingTool.MARKER,
            R.id.btnEraser      to DrawingTool.ERASER,
            R.id.btnLine        to DrawingTool.LINE,
            R.id.btnRect        to DrawingTool.RECTANGLE,
            R.id.btnCircle      to DrawingTool.CIRCLE,
            R.id.btnArrow       to DrawingTool.ARROW,
            R.id.btnLaser       to DrawingTool.LASER
        )
        toolMap.forEach { (id, tool) ->
            root.findViewById<ImageButton>(id)?.setOnClickListener {
                canvas.currentTool = tool
                // Exit pass-through when picking a drawing tool
                if (isPassThrough) {
                    isPassThrough = false
                    onTogglePassThrough(false)
                    root.findViewById<ImageButton>(R.id.btnPassThrough)
                        .clearColorFilter()
                }
                updateToolHighlight(id)
            }
        }

        root.findViewById<ImageButton>(R.id.btnUndo).setOnClickListener { canvas.undo() }
        root.findViewById<ImageButton>(R.id.btnRedo).setOnClickListener { canvas.redo() }
        root.findViewById<ImageButton>(R.id.btnClear).setOnClickListener { canvas.clearAll() }

        // Pass-through (interact with screen behind)
        root.findViewById<ImageButton>(R.id.btnPassThrough).setOnClickListener {
            isPassThrough = !isPassThrough
            onTogglePassThrough(isPassThrough)
            val btn = root.findViewById<ImageButton>(R.id.btnPassThrough)
            if (isPassThrough) btn.setColorFilter(Color.WHITE)
            else btn.clearColorFilter()
        }

        // Screenshot crop & overlay
        root.findViewById<ImageButton>(R.id.btnScreenshot).setOnClickListener {
            onScreenshot()
        }

        // Spotlight toggle
        root.findViewById<ImageButton>(R.id.btnSpotlight).setOnClickListener {
            toggleSpotlight()
        }

        root.findViewById<ImageButton>(R.id.btnClose).setOnClickListener { onClose() }

        // Stroke slider
        root.findViewById<Slider>(R.id.strokeSlider).addOnChangeListener { _, value, _ ->
            canvas.strokeWidth = value
        }

        // Opacity slider
        root.findViewById<Slider>(R.id.opacitySlider).addOnChangeListener { _, value, _ ->
            canvas.opacity = value.toInt()
        }

        // Color palette
        setupColorPalette()

        // Settings: stylus button action
        root.findViewById<Spinner>(R.id.spinnerStylusAction)?.let { spinner ->
            val opts = arrayOf("Eraser", "Laser", "Marker")
            spinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, opts)
                .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    canvas.stylusButtonAction = when (pos) {
                        1    -> StylusButtonAction.LASER
                        2    -> StylusButtonAction.MARKER
                        else -> StylusButtonAction.ERASER
                    }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
    }

    private fun setupColorPalette() {
        val container = root.findViewById<LinearLayout>(R.id.colorContainer)
        container.removeAllViews()
        colorPalette.forEach { color ->
            val btn = View(context).apply {
                val size = (32 * context.resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(3, 3, 3, 3)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(2, if (color == Color.BLACK) 0x66FFFFFF else 0x44000000)
                }
                setOnClickListener {
                    canvas.currentColor = color
                    if (canvas.currentTool == DrawingTool.ERASER)
                        canvas.currentTool = DrawingTool.PEN
                }
            }
            container.addView(btn)
        }
    }

    private fun updateToolHighlight(selectedId: Int) {
        val allIds = listOf(R.id.btnPen, R.id.btnPencil, R.id.btnFountain, R.id.btnBrush,
            R.id.btnCalligraphy, R.id.btnMarker, R.id.btnEraser,
            R.id.btnLine, R.id.btnRect, R.id.btnCircle, R.id.btnArrow, R.id.btnLaser)
        allIds.forEach { id ->
            root.findViewById<ImageButton>(id)?.alpha = if (id == selectedId) 1f else 0.4f
        }
    }

    // ── Spotlight ──────────────────────────────────────────────────────────
    private fun toggleSpotlight() {
        if (isSpotlight) {
            spotlightView?.let { wm.removeView(it) }
            spotlightView = null
            isSpotlight = false
        } else {
            val sv = SpotlightView(context)
            spotlightView = sv
            wm.addView(sv, spotlightParams)
            isSpotlight = true
        }
        val btn = root.findViewById<ImageButton>(R.id.btnSpotlight)
        if (isSpotlight) btn.setColorFilter(Color.WHITE) else btn.clearColorFilter()
    }

    // ── Screenshot crop overlay ───────────────────────────────────────────
    fun startScreenshotCrop(bmp: Bitmap?) {
        screenshotBmp = bmp ?: return
        // For now: overlay the current canvas bitmap at full screen
        // Full crop UI would need Activity launch — simplified: paste bitmap to canvas
        canvas.overlayBitmap(screenshotBmp!!)
    }

    fun getView(): View = root
}

// Simple Spotlight overlay: darkens edges, bright oval in center
class SpotlightView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dimPaint = Paint().apply { color = 0xCC000000.toInt() }
    private var cx = 0f; private var cy = 0f
    private val radius = 200f

    init {
        setOnTouchListener { _, e ->
            cx = e.x; cy = e.y; invalidate(); true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        cx = w / 2f; cy = h / 2f
    }

    override fun onDraw(c: Canvas) {
        // Draw dark overlay with hole
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bc  = Canvas(bmp)
        bc.drawColor(0xCC000000.toInt())
        val erasePaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL)
        }
        bc.drawCircle(cx, cy, radius, erasePaint)
        c.drawBitmap(bmp, 0f, 0f, null)
        bmp.recycle()
    }
}
