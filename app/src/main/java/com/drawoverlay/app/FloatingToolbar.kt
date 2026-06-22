package com.drawoverlay.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import com.google.android.material.slider.Slider
import androidx.appcompat.view.ContextThemeWrapper

class FloatingToolbar(
    private val context: Context,
    private val wm: WindowManager,
    private val canvas: DrawingCanvas,
    private val prefs: AppPrefs,
    private val onClose: () -> Unit,
    private val onTogglePassThrough: (Boolean) -> Unit,
    private val onScreenshot: () -> Unit
) {
    private val root: View = LayoutInflater.from(context).inflate(R.layout.layout_toolbar, null)
    private var isPassThrough = prefs.autoPassthrough
    private var isExpanded = true
    private var isSpotlight = false
    private var spotlightView: View? = null

    private val spotlightParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = if (prefs.toolbarSide) Gravity.TOP or Gravity.END else Gravity.TOP or Gravity.START
        x = 0; y = 200
    }

    private var dragStartX = 0f; private var dragStartY = 0f
    private var winStartX = 0; private var winStartY = 0

    private val colorPalette = listOf(
        Color.WHITE, Color.BLACK,
        0xFFFF5252.toInt(), 0xFF69F0AE.toInt(), 0xFF448AFF.toInt(),
        0xFFFFFF00.toInt(), 0xFFFF9100.toInt(), 0xFFEA80FC.toInt(),
        0xFF18FFFF.toInt(), 0xFF9E9E9E.toInt(),
        0xFFFF6D00.toInt(), 0xFF00BFA5.toInt(),
        0xFFD500F9.toInt(), 0xFFFFD740.toInt()
    )

    init {
        setupDrag()
        setupButtons()
        applyPrefs()
        if (isPassThrough) {
            onTogglePassThrough(true)
            root.findViewById<ImageButton>(R.id.btnPassThrough)?.setColorFilter(0xFFFFEB3B.toInt())
        }
    }

    private fun applyPrefs() {
        fun vis(id: Int, show: Boolean) {
            root.findViewById<View>(id)?.visibility = if (show) View.VISIBLE else View.GONE
        }
        vis(R.id.btnUndo, prefs.showUndo)
        vis(R.id.btnRedo, prefs.showRedo)
        vis(R.id.btnEraser, prefs.showEraser)
        vis(R.id.shapesDivider, prefs.showShapes)
        vis(R.id.btnLine, prefs.showShapes)
        vis(R.id.btnRect, prefs.showShapes)
        vis(R.id.btnCircle, prefs.showShapes)
        vis(R.id.btnArrow, prefs.showShapes)
        vis(R.id.btnLaser, prefs.showLaser)
        vis(R.id.btnSpotlight, prefs.showSpotlight)
        vis(R.id.btnScreenshot, prefs.showScreenshot)
        vis(R.id.btnPassThrough, prefs.showPassthrough)
        vis(R.id.btnRuler, prefs.showRuler)

        vis(R.id.btnFountain, prefs.showFountain)
        vis(R.id.btnCalligraphy, prefs.showCalligraphy)
        vis(R.id.btnPencil, prefs.showPencil)
        vis(R.id.btnBrush, prefs.showBrush)
        vis(R.id.btnMarker, prefs.showMarker)

        val defaultTool = try { DrawingTool.valueOf(prefs.defaultTool) } catch (_: Exception) { DrawingTool.PEN }
        canvas.currentTool = defaultTool
        canvas.strokeWidth = prefs.defaultStroke
        
        val id = getToolButtonId(defaultTool)
        if (id != 0) updateToolHighlight(id)
    }

    private fun getToolButtonId(tool: DrawingTool): Int {
        return when (tool) {
            DrawingTool.PEN -> R.id.btnPen
            DrawingTool.PENCIL -> R.id.btnPencil
            DrawingTool.FOUNTAIN -> R.id.btnFountain
            DrawingTool.BRUSH -> R.id.btnBrush
            DrawingTool.CALLIGRAPHY -> R.id.btnCalligraphy
            DrawingTool.MARKER -> R.id.btnMarker
            DrawingTool.ERASER -> R.id.btnEraser
            DrawingTool.LINE -> R.id.btnLine
            DrawingTool.RECTANGLE -> R.id.btnRect
            DrawingTool.CIRCLE -> R.id.btnCircle
            DrawingTool.ARROW -> R.id.btnArrow
            DrawingTool.LASER -> R.id.btnLaser
            else -> 0
        }
    }

    private fun setupDrag() {
        root.findViewById<View>(R.id.dragHandle)?.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { 
                    dragStartX = ev.rawX; dragStartY = ev.rawY
                    winStartX = params.x; winStartY = params.y 
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (winStartX + (ev.rawX - dragStartX)).toInt()
                    params.y = (winStartY + (ev.rawY - dragStartY)).toInt()
                    try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
                }
                MotionEvent.ACTION_UP -> { v.performClick() }
            }
            true
        }
    }

    private fun setupButtons() {
        root.findViewById<ImageButton>(R.id.btnCollapse)?.setOnClickListener {
            isExpanded = !isExpanded
            root.findViewById<LinearLayout>(R.id.toolsContainer)?.visibility =
                if (isExpanded) View.VISIBLE else View.GONE
            root.findViewById<ImageButton>(R.id.btnCollapse)
                ?.setImageResource(if (isExpanded) R.drawable.ic_collapse else R.drawable.ic_expand)
        }

        val toolMap = mapOf(
            R.id.btnPen to DrawingTool.PEN,
            R.id.btnPencil to DrawingTool.PENCIL,
            R.id.btnFountain to DrawingTool.FOUNTAIN,
            R.id.btnBrush to DrawingTool.BRUSH,
            R.id.btnCalligraphy to DrawingTool.CALLIGRAPHY,
            R.id.btnMarker to DrawingTool.MARKER,
            R.id.btnEraser to DrawingTool.ERASER,
            R.id.btnLine to DrawingTool.LINE,
            R.id.btnRect to DrawingTool.RECTANGLE,
            R.id.btnCircle to DrawingTool.CIRCLE,
            R.id.btnArrow to DrawingTool.ARROW,
            R.id.btnLaser to DrawingTool.LASER
        )

        toolMap.forEach { (id, tool) ->
            root.findViewById<ImageButton>(id)?.setOnClickListener {
                selectTool(tool, id)
            }
        }

        root.findViewById<ImageButton>(R.id.btnUndo)?.setOnClickListener { canvas.undo() }
        root.findViewById<ImageButton>(R.id.btnRedo)?.setOnClickListener { canvas.redo() }
        root.findViewById<ImageButton>(R.id.btnClear)?.setOnClickListener { canvas.clearAll() }

        root.findViewById<ImageButton>(R.id.btnPassThrough)?.setOnClickListener {
            isPassThrough = !isPassThrough
            onTogglePassThrough(isPassThrough)
            val btn = root.findViewById<ImageButton>(R.id.btnPassThrough)
            if (isPassThrough) btn?.setColorFilter(0xFFFFEB3B.toInt()) else btn?.clearColorFilter()
        }

        root.findViewById<ImageButton>(R.id.btnScreenshot)?.setOnClickListener { onScreenshot() }
        root.findViewById<ImageButton>(R.id.btnSpotlight)?.setOnClickListener { toggleSpotlight() }
        root.findViewById<ImageButton>(R.id.btnRuler)?.setOnClickListener { 
            prefs.showRuler = !prefs.showRuler
            updateRulerButton()
            canvas.invalidate()
        }
        root.findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener { onClose() }

        root.findViewById<Slider>(R.id.strokeSlider)?.apply {
            value = prefs.defaultStroke.coerceIn(2f, 60f)
            addOnChangeListener { _, v, _ -> canvas.strokeWidth = v }
        }
        root.findViewById<Slider>(R.id.opacitySlider)?.addOnChangeListener { _, v, _ -> canvas.opacity = v.toInt() }

        setupColorPalette()
        updateRulerButton()

        root.findViewById<ImageButton>(R.id.btnToolSelect)?.setOnClickListener { btn ->
            val themedContext = ContextThemeWrapper(context, R.style.Theme_DrawOnScreen)
            val popup = PopupMenu(themedContext, btn)
            val tools = DrawingTool.entries
            tools.forEachIndexed { index, tool ->
                popup.menu.add(0, index, 0, tool.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
            }
            popup.setOnMenuItemClickListener { item ->
                val tool = tools[item.itemId]
                val btnId = getToolButtonId(tool)
                selectTool(tool, if (btnId != 0) btnId else R.id.btnToolSelect)
                true
            }
            popup.show()
        }

        root.findViewById<ImageButton>(R.id.btnStylusAction)?.setOnClickListener { btn ->
            val themedContext = ContextThemeWrapper(context, R.style.Theme_DrawOnScreen)
            val popup = PopupMenu(themedContext, btn)
            val actions = listOf("Eraser", "Laser", "Marker")
            actions.forEachIndexed { index, action -> popup.menu.add(0, index, 0, action) }
            popup.setOnMenuItemClickListener { item ->
                val action = when (item.itemId) {
                    1 -> StylusButtonAction.LASER
                    2 -> StylusButtonAction.MARKER
                    else -> StylusButtonAction.ERASER
                }
                canvas.stylusButtonAction = action
                prefs.stylusButtonAction = action.name
                true
            }
            popup.show()
        }
    }

    private fun selectTool(tool: DrawingTool, btnId: Int) {
        canvas.currentTool = tool
        if (isPassThrough) {
            isPassThrough = false
            onTogglePassThrough(false)
            root.findViewById<ImageButton>(R.id.btnPassThrough)?.clearColorFilter()
        }
        updateToolHighlight(btnId)
    }

    private fun setupColorPalette() {
        val container = root.findViewById<LinearLayout>(R.id.colorContainer) ?: return
        container.removeAllViews()
        colorPalette.forEach { color ->
            val sz = (28 * context.resources.displayMetrics.density).toInt()
            val btn = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(sz, sz).apply { setMargins(3, 3, 3, 3) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(2, if (color == Color.BLACK) 0x66FFFFFF else 0x33000000)
                }
                setOnClickListener {
                    canvas.currentColor = color
                    if (canvas.currentTool == DrawingTool.ERASER) canvas.currentTool = DrawingTool.PEN
                }
            }
            container.addView(btn)
        }
    }

    private fun updateRulerButton() {
        root.findViewById<ImageButton>(R.id.btnRuler)?.let {
            if (prefs.showRuler) it.setColorFilter(0xFFFFEB3B.toInt()) else it.clearColorFilter()
        }
    }

    private fun updateToolHighlight(selectedId: Int) {
        val toolIds = listOf(R.id.btnPen, R.id.btnPencil, R.id.btnFountain, R.id.btnBrush, R.id.btnCalligraphy,
            R.id.btnMarker, R.id.btnEraser, R.id.btnLine, R.id.btnRect, R.id.btnCircle, R.id.btnArrow, R.id.btnLaser, R.id.btnToolSelect)
        
        toolIds.forEach { id -> 
            root.findViewById<ImageButton>(id)?.alpha = if (id == selectedId) 1f else 0.55f 
        }
    }

    private fun toggleSpotlight() {
        if (isSpotlight) {
            spotlightView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
            spotlightView = null; isSpotlight = false
        } else {
            val sv = SpotlightView(context)
            spotlightView = sv
            try { wm.addView(sv, spotlightParams) } catch (_: Exception) {}
            isSpotlight = true
        }
        root.findViewById<ImageButton>(R.id.btnSpotlight)?.let {
            if (isSpotlight) it.setColorFilter(0xFFFFEB3B.toInt()) else it.clearColorFilter()
        }
    }

    fun startScreenshotCrop(bmp: Bitmap?) { bmp?.let { canvas.overlayBitmap(it) } }

    fun refreshUi() {
        applyPrefs()
    }

    fun refreshPosition() {
        params.gravity = if (prefs.toolbarSide) Gravity.TOP or Gravity.END else Gravity.TOP or Gravity.START
        try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
    }

    fun getView(): View = root
}

class SpotlightView(context: Context) : View(context) {
    private var cx = 0f; private var cy = 0f; private val radius = 200f
    init { setOnTouchListener { _, e -> cx = e.x; cy = e.y; invalidate(); true } }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { cx = w / 2f; cy = h / 2f }
    override fun onDraw(c: Canvas) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bc = Canvas(bmp)
        bc.drawColor(0xCC000000.toInt())
        val ep = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL) }
        bc.drawCircle(cx, cy, radius, ep)
        c.drawBitmap(bmp, 0f, 0f, null)
        bmp.recycle()
    }
}
