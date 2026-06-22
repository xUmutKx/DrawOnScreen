package com.drawoverlay.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import com.google.android.material.slider.Slider

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
    private var isExpanded = false

    val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = if (prefs.toolbarSide) Gravity.TOP or Gravity.END else Gravity.TOP or Gravity.START
        x = 0; y = 250
    }

    private var dragStartX = 0f; private var dragStartY = 0f
    private var winStartX = 0; private var winStartY = 0

    init {
        setupDrag()
        setupButtons()
        setupToolsGrid()
        setupColorPalette()
        applyPrefs()
    }

    private fun applyPrefs() {
        val defaultTool = try { DrawingTool.valueOf(prefs.defaultTool) } catch (_: Exception) { DrawingTool.PEN }
        canvas.currentTool = defaultTool
        canvas.strokeWidth = prefs.defaultStroke
        canvas.currentColor = prefs.defaultColor
        updateToolHighlight(getToolButtonId(defaultTool))
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
                MotionEvent.ACTION_UP -> v.performClick()
            }
            true
        }
    }

    private fun setupButtons() {
        root.findViewById<ImageButton>(R.id.btnToolSelect)?.setOnClickListener {
            toggleMenu()
        }

        root.findViewById<ImageButton>(R.id.btnPen)?.setOnClickListener { 
            selectTool(DrawingTool.PEN, R.id.btnPen)
        }

        root.findViewById<ImageButton>(R.id.btnEraser)?.setOnClickListener { 
            selectTool(DrawingTool.ERASER, R.id.btnEraser)
        }

        root.findViewById<ImageButton>(R.id.btnUndo)?.setOnClickListener { canvas.undo() }
        root.findViewById<ImageButton>(R.id.btnClear)?.setOnClickListener { canvas.clearAll() }
        root.findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener { onClose() }

        root.findViewById<Slider>(R.id.strokeSlider)?.apply {
            value = prefs.defaultStroke.coerceIn(2f, 80f)
            addOnChangeListener { _, v, _ -> canvas.strokeWidth = v }
        }
    }

    private fun toggleMenu() {
        isExpanded = !isExpanded
        root.findViewById<View>(R.id.expandableMenu).visibility = if (isExpanded) View.VISIBLE else View.GONE
        root.findViewById<ImageButton>(R.id.btnToolSelect).setImageResource(
            if (isExpanded) R.drawable.ic_collapse else R.drawable.ic_expand
        )
        // Re-update layout to wrap content
        try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
    }

    private fun setupToolsGrid() {
        val grid = root.findViewById<GridLayout>(R.id.toolsGrid)
        grid.removeAllViews()
        
        DrawingTool.entries.forEach { tool ->
            val btn = ImageButton(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = (48 * context.resources.displayMetrics.density).toInt()
                    height = width
                }
                setBackgroundResource(R.drawable.bg_tool_button)
                setImageResource(getIconForTool(tool))
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(12, 12, 12, 12)
                setColorFilter(Color.WHITE)
                setOnClickListener { 
                    selectTool(tool, 0)
                    toggleMenu() 
                }
            }
            grid.addView(btn)
        }
    }

    private fun setupColorPalette() {
        val container = root.findViewById<LinearLayout>(R.id.colorContainer) ?: return
        val colors = listOf(Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        container.removeAllViews()
        colors.forEach { color ->
            val btn = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (32 * context.resources.displayMetrics.density).toInt(),
                    (32 * context.resources.displayMetrics.density).toInt()
                ).apply { setMargins(4, 0, 4, 0) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(2, Color.LTGRAY)
                }
                setOnClickListener { 
                    canvas.currentColor = color
                    if (isExpanded) toggleMenu()
                }
            }
            container.addView(btn)
        }
    }

    private fun selectTool(tool: DrawingTool, btnId: Int) {
        canvas.currentTool = tool
        updateToolHighlight(btnId)
    }

    private fun updateToolHighlight(selectedId: Int) {
        val mainIds = listOf(R.id.btnPen, R.id.btnEraser)
        mainIds.forEach { id -> 
            root.findViewById<ImageButton>(id)?.alpha = if (id == selectedId) 1f else 0.4f 
        }
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

    private fun getToolButtonId(tool: DrawingTool): Int {
        return when (tool) {
            DrawingTool.PEN -> R.id.btnPen
            DrawingTool.ERASER -> R.id.btnEraser
            else -> 0
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
