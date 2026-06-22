package com.drawoverlay.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import com.google.android.material.slider.Slider
import com.google.android.material.card.MaterialCardView

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
    
    private var toolsMenu: View? = null

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
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = if (prefs.toolbarSide) Gravity.TOP or Gravity.END else Gravity.TOP or Gravity.START
        x = 0; y = 200
    }

    private var dragStartX = 0f; private var dragStartY = 0f
    private var winStartX = 0; private var winStartY = 0

    private val colorPalette = listOf(
        Color.WHITE, Color.BLACK, Color.RED, 0xFF4CAF50.toInt(), 0xFF2196F3.toInt(),
        0xFFFFEB3B.toInt(), 0xFF00BCD4.toInt(), 0xFFE91E63.toInt(), 
        0xFFFF9800.toInt(), 0xFF9C27B0.toInt(),
        0xFF795548.toInt(), 0xFF9E9E9E.toInt()
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
        vis(R.id.btnRuler, true)

        vis(R.id.btnFountain, prefs.showFountain)
        vis(R.id.btnCalligraphy, prefs.showCalligraphy)
        vis(R.id.btnPencil, prefs.showPencil)
        vis(R.id.btnBrush, prefs.showBrush)
        vis(R.id.btnMarker, prefs.showMarker)

        val defaultTool = try { DrawingTool.valueOf(prefs.defaultTool) } catch (_: Exception) { DrawingTool.PEN }
        canvas.currentTool = defaultTool
        canvas.strokeWidth = prefs.defaultStroke
        canvas.currentColor = prefs.defaultColor
        
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
                    hideToolMenu()
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
            hideToolMenu()
        }

        val toolMap = mapOf(
            R.id.btnPen to DrawingTool.PEN,
            R.id.btnPencil to DrawingTool.PENCIL,
            R.id.btnFountain to DrawingTool.FOUNTAIN,
            R.id.btnEraser to DrawingTool.ERASER,
            R.id.btnLine to DrawingTool.LINE,
            R.id.btnRect to DrawingTool.RECTANGLE,
            R.id.btnCircle to DrawingTool.CIRCLE,
            R.id.btnArrow to DrawingTool.ARROW,
            R.id.btnLaser to DrawingTool.LASER
        )

        toolMap.forEach { (id, tool) ->
            root.findViewById<ImageButton>(id)?.setOnClickListener { selectTool(tool, id) }
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

        root.findViewById<ImageButton>(R.id.btnToolSelect)?.setOnClickListener { showToolMenu() }
        
        root.findViewById<ImageButton>(R.id.btnStylusAction)?.setOnClickListener { 
            val next = when(canvas.stylusButtonAction) {
                StylusButtonAction.ERASER -> StylusButtonAction.LASER
                StylusButtonAction.LASER -> StylusButtonAction.MARKER
                else -> StylusButtonAction.ERASER
            }
            canvas.stylusButtonAction = next
            prefs.stylusButtonAction = next.name
            Toast.makeText(context, "Stylus: ${next.name}", Toast.LENGTH_SHORT).show()
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
        hideToolMenu()
    }

    private fun showToolMenu() {
        if (toolsMenu != null) { hideToolMenu(); return }
        
        val menu = LayoutInflater.from(context).inflate(R.layout.layout_tools_menu, null)
        val grid = menu.findViewById<GridLayout>(R.id.toolsGrid)
        
        DrawingTool.entries.forEach { tool ->
            val btn = ImageButton(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = (52 * context.resources.displayMetrics.density).toInt()
                    height = width
                }
                setBackgroundResource(R.drawable.bg_tool_button)
                setImageResource(getIconForTool(tool))
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(14, 14, 14, 14)
                setColorFilter(Color.WHITE)
                alpha = if (canvas.currentTool == tool) 1f else 0.5f
                setOnClickListener { selectTool(tool, 0) }
            }
            grid.addView(btn)
        }
        
        menu.findViewById<View>(R.id.btnClearMenu)?.setOnClickListener { 
            canvas.clearAll()
            hideToolMenu()
        }

        // IMPROVED MENU POSITIONING
        val toolbarPos = IntArray(2)
        root.getLocationOnScreen(toolbarPos)
        
        val menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            
            val menuWidth = (220 * context.resources.displayMetrics.density).toInt()
            if (prefs.toolbarSide) {
                // Toolbar on right, put menu on the left
                x = toolbarPos[0] - menuWidth - (8 * context.resources.displayMetrics.density).toInt()
            } else {
                // Toolbar on left, put menu on the right
                x = toolbarPos[0] + root.width + (8 * context.resources.displayMetrics.density).toInt()
            }
            y = toolbarPos[1]
            
            if (x < 0) x = 8
        }
        
        try {
            wm.addView(menu, menuParams)
            toolsMenu = menu
            
            menu.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    hideToolMenu()
                    true
                } else false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Dropdown Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideToolMenu() {
        toolsMenu?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        toolsMenu = null
    }

    private fun getIconForTool(tool: DrawingTool): Int {
        return when(tool) {
            DrawingTool.PEN -> R.drawable.ic_pen
            DrawingTool.PENCIL -> R.drawable.ic_pencil
            DrawingTool.FOUNTAIN -> R.drawable.ic_fountain
            DrawingTool.BRUSH -> R.drawable.ic_brush
            DrawingTool.CALLIGRAPHY -> R.drawable.ic_calligraphy
            DrawingTool.MARKER -> R.drawable.ic_marker
            DrawingTool.CRAYON -> R.drawable.ic_pen
            DrawingTool.GLOW -> R.drawable.ic_laser
            DrawingTool.AIRBRUSH -> R.drawable.ic_brush
            DrawingTool.CHARCOAL -> R.drawable.ic_pencil
            DrawingTool.ERASER -> R.drawable.ic_eraser
            DrawingTool.LINE -> R.drawable.ic_line
            DrawingTool.RECTANGLE -> R.drawable.ic_rect
            DrawingTool.CIRCLE -> R.drawable.ic_circle
            DrawingTool.ARROW -> R.drawable.ic_arrow
            DrawingTool.LASER -> R.drawable.ic_laser
            else -> R.drawable.ic_expand
        }
    }

    private fun setupColorPalette() {
        val container = root.findViewById<LinearLayout>(R.id.colorContainer) ?: return
        container.removeAllViews()
        colorPalette.forEach { color ->
            val sz = (32 * context.resources.displayMetrics.density).toInt()
            val btn = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(sz, sz).apply { setMargins(4, 4, 4, 4) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(2, if (color == Color.BLACK) 0x66FFFFFF else 0x33000000)
                }
                setOnClickListener {
                    canvas.currentColor = color
                    if (canvas.currentTool == DrawingTool.ERASER) canvas.currentTool = DrawingTool.PEN
                    hideToolMenu()
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
            root.findViewById<ImageButton>(id)?.alpha = if (id == selectedId) 1f else 0.4f 
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
    fun refreshUi() { applyPrefs() }
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
