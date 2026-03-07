package com.printer.godsimulator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.*
import kotlin.math.floor

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var gameLoopThread: Job? = null
    private var scope: CoroutineScope? = null

    private lateinit var spriteManager: SpriteManager
    private lateinit var world: World
    private val tileSize = GameConfig.TILE_SIZE

    private var grassTexture: Bitmap? = null
    private var grassTexturePaint: Paint? = null

    private var cameraX = 0f
    private var cameraY = 0f
    private val zoom = GameConfig.ZOOM_DEFAULT

    private var isSurfaceCreated = false
    private var viewWidth = 0
    private var viewHeight = 0

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private var lastFpsTime = System.currentTimeMillis()
    private var fps = 0
    private var frameCount = 0

    // ✅ Кнопки (справа сверху)
    private var saveButtonRect = RectF()
    private var newWorldButtonRect = RectF()
    private var saveButtonPressed = false
    private var newWorldButtonPressed = false
    private val buttonHeight = 50f
    private val buttonWidth = 140f
    private val buttonMargin = 10f
    private val buttonSpacing = 8f

    private val treeTrunkPaint = Paint().apply {
        color = Color.rgb(80, 50, 30)
        isAntiAlias = false
    }

    private val treeCrownPaint = Paint().apply {
        color = Color.rgb(0, 100, 0)
        isAntiAlias = false
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 18f
        isAntiAlias = true
        setShadowLayer(2f, 2f, 2f, Color.BLACK)
    }

    private val buttonPaint = Paint().apply {
        color = Color.rgb(0, 150, 0)
        isAntiAlias = true
    }

    private val buttonPressedPaint = Paint().apply {
        color = Color.rgb(0, 200, 0)
        isAntiAlias = true
    }

    private val buttonDeletePaint = Paint().apply {
        color = Color.rgb(200, 50, 50)
        isAntiAlias = true
    }

    private val buttonDeletePressedPaint = Paint().apply {
        color = Color.rgb(220, 80, 80)
        isAntiAlias = true
    }

    private val tileColorCache = mutableMapOf<TileType, Paint>()

    init {
        holder.addCallback(this)
        loadTextures(context)
        initTileColorCache()
        spriteManager = SpriteManager(context)
        world = World(spriteManager)

        WorldSave.init(context)
        val loaded = WorldSave.loadWorld(world, spriteManager)
        if (!loaded) {
            world = World(spriteManager)
        }
    }

    private fun loadTextures(context: Context) {
        try {
            grassTexture = BitmapFactory.decodeResource(context.resources, R.drawable.grass)
            grassTexturePaint = Paint().apply {
                isAntiAlias = false
                isFilterBitmap = false
            }
        } catch (e: Exception) {
            grassTexture = null
        }
    }

    private fun initTileColorCache() {
        TileType.values().forEach { type ->
            tileColorCache[type] = Paint().apply {
                color = type.color
                isAntiAlias = false
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        viewWidth = width
        viewHeight = height
        isSurfaceCreated = true
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        cameraX = -viewWidth / 2f + (tileSize * zoom * 16)
        cameraY = -viewHeight / 2f + (tileSize * zoom * 16)
        startGameLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceCreated = false
        stopGameLoop()
        if (WorldSave.isAutoSaveEnabled()) {
            WorldSave.saveWorld(world)
        }
        scope?.cancel()
        scope = null
    }

    private fun startGameLoop() {
        val currentScope = scope ?: return
        gameLoopThread = currentScope.launch {
            while (isActive && isSurfaceCreated) {
                val startTime = System.currentTimeMillis()
                update()
                draw()
                val frameTime = System.currentTimeMillis() - startTime
                if (frameTime < GameConfig.FRAME_TIME_MS) {
                    delay(GameConfig.FRAME_TIME_MS - frameTime)
                }
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopThread?.cancel()
        gameLoopThread = null
    }

    private fun update() {
        val visibleChunks = getVisibleChunks()
        world.updateVisibleChunks(visibleChunks)
        for (chicken in world.getChickensInChunks(visibleChunks)) {
            chicken.update()
        }
        for (creature in world.getCreaturesInChunks(visibleChunks)) {
            creature.update()
        }
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime > 1000) {
            fps = frameCount
            frameCount = 0
            lastFpsTime = now
        }
    }

    private fun getVisibleChunks(): Set<Pair<Int, Int>> {
        val scaledTileSize = (tileSize * zoom).toInt()
        if (scaledTileSize <= 0) return emptySet()
        val startTileX = floor((cameraX - tileSize) / scaledTileSize).toInt() - 1
        val startTileY = floor((cameraY - tileSize) / scaledTileSize).toInt() - 1
        val endTileX = floor((cameraX + viewWidth + tileSize) / scaledTileSize).toInt() + 1
        val endTileY = floor((cameraY + viewHeight + tileSize) / scaledTileSize).toInt() + 1
        val visibleChunks = mutableSetOf<Pair<Int, Int>>()
        for (x in startTileX..endTileX) {
            for (y in startTileY..endTileY) {
                val chunkX = floor(x.toDouble() / GameConfig.CHUNK_SIZE).toInt()
                val chunkY = floor(y.toDouble() / GameConfig.CHUNK_SIZE).toInt()
                visibleChunks.add(Pair(chunkX, chunkY))
            }
        }
        return visibleChunks
    }

    private fun draw() {
        if (!isSurfaceCreated || viewWidth == 0 || viewHeight == 0) return
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.rgb(135, 206, 235))
            drawWorld(canvas)
            val visibleChunks = getVisibleChunks()
            drawChickens(canvas, visibleChunks)
            drawCreatures(canvas, visibleChunks)
            drawButtons(canvas)
            drawStats(canvas)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    // ✅ Кнопки — ПРАВАЯ ВЕРХНЯЯ ЧАСТЬ
    private fun drawButtons(canvas: Canvas) {
        val btnH = buttonHeight
        val btnW = buttonWidth

        // Кнопка "💾 Сохранить" — верхняя
        saveButtonRect = RectF(
            viewWidth - buttonMargin - btnW,
            buttonMargin,
            viewWidth - buttonMargin,
            buttonMargin + btnH
        )
        canvas.drawRoundRect(
            saveButtonRect,
            12f, 12f,
            if (saveButtonPressed) buttonPressedPaint else buttonPaint
        )
        val saveTextPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 16f
        }
        canvas.drawText("💾 Сохранить",
            saveButtonRect.centerX(),
            saveButtonRect.centerY() + 6f,
            saveTextPaint)

        // Кнопка "🔄 Новый мир" — нижняя
        newWorldButtonRect = RectF(
            viewWidth - buttonMargin - btnW,
            buttonMargin + btnH + buttonSpacing,
            viewWidth - buttonMargin,
            buttonMargin + btnH * 2 + buttonSpacing
        )
        canvas.drawRoundRect(
            newWorldButtonRect,
            12f, 12f,
            if (newWorldButtonPressed) buttonDeletePressedPaint else buttonDeletePaint
        )
        val newWorldTextPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 16f
        }
        canvas.drawText("🔄 Новый мир",
            newWorldButtonRect.centerX(),
            newWorldButtonRect.centerY() + 6f,
            newWorldTextPaint)
    }

    // ✅ Статистика — ЛЕВАЯ ВЕРХНЯЯ ЧАСТЬ
    private fun drawStats(canvas: Canvas) {
        val baseX = 15f
        val baseY = 20f
        val stepY = 28f

        val statsPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.LEFT
            textSize = 16f
        }

        canvas.drawText("⚡ FPS: $fps", baseX, baseY, statsPaint)
        canvas.drawText("🐔 Кур: ${world.getDiscoveredChickenCount()}", baseX, baseY + stepY, statsPaint)
        canvas.drawText("🧍 Существ: ${world.allCreatures.size}", baseX, baseY + stepY * 2, statsPaint)

        val hintPaint = Paint(statsPaint).apply {
            textSize = 14f
            alpha = 180
        }
        canvas.drawText("👆 Тащи для перемещения", baseX, baseY + stepY * 3.5f, hintPaint)
    }

    private fun drawWorld(canvas: Canvas) {
        val scaledTileSize = (tileSize * zoom).toInt()
        if (scaledTileSize <= 0) return
        val startX = floor((cameraX - tileSize) / scaledTileSize).toInt()
        val startY = floor((cameraY - tileSize) / scaledTileSize).toInt()
        val endX = floor((cameraX + viewWidth + tileSize) / scaledTileSize).toInt()
        val endY = floor((cameraY + viewHeight + tileSize) / scaledTileSize).toInt()
        for (x in startX..endX) {
            for (y in startY..endY) {
                val tile = world.getTile(x, y)
                val screenX = x * scaledTileSize - cameraX
                val screenY = y * scaledTileSize - cameraY
                val size = scaledTileSize.toFloat()
                if (screenX + size < 0 || screenX > viewWidth || screenY + size < 0 || screenY > viewHeight) continue
                if (tile.type == TileType.GRASS && grassTexture != null) {
                    val dstRect = Rect(screenX.toInt(), screenY.toInt(), (screenX + size).toInt(), (screenY + size).toInt())
                    canvas.drawBitmap(grassTexture!!, null, dstRect, grassTexturePaint)
                } else {
                    canvas.drawRect(screenX, screenY, screenX + size, screenY + size,
                        tileColorCache[tile.type] ?: tileColorCache[TileType.GRASS]!!)
                }
                if (tile.treeCount > 0 && zoom > 1.0f) {
                    drawTrees(canvas, screenX, screenY, size, tile.treeCount)
                }
            }
        }
    }

    private fun drawTrees(canvas: Canvas, x: Float, y: Float, size: Float, count: Int) {
        for (i in 0 until count.coerceAtMost(GameConfig.TREE_MAX_COUNT)) {
            val treeX = x + size * (0.3f + i * 0.2f)
            val treeY = y + size * 0.5f
            canvas.drawRect(treeX - size/12, treeY, treeX + size/12, treeY + size/4, treeTrunkPaint)
            canvas.drawCircle(treeX, treeY - size/6, size/6, treeCrownPaint)
        }
    }

    private fun drawChickens(canvas: Canvas, visibleChunks: Set<Pair<Int, Int>>) {
        val chickens = world.getChickensInChunks(visibleChunks)
        val scaledTileSize = (tileSize * zoom).toFloat()
        for (chicken in chickens) {
            val pixelX = chicken.getPixelX(scaledTileSize)
            val pixelY = chicken.getPixelY(scaledTileSize)
            val screenX = pixelX - cameraX
            val screenY = pixelY - cameraY
            if (screenX < -50 || screenX > viewWidth + 50 || screenY < -50 || screenY > viewHeight + 50) continue
            canvas.save()
            canvas.translate(screenX, screenY)
            canvas.scale(zoom, zoom)
            chicken.draw(canvas, 0f, 0f, 32f)
            canvas.restore()
        }
    }

    private fun drawCreatures(canvas: Canvas, visibleChunks: Set<Pair<Int, Int>>) {
        val creatures = world.getCreaturesInChunks(visibleChunks)
        val scaledTileSize = (tileSize * zoom).toFloat()
        for (creature in creatures) {
            val screenX = creature.tileX * scaledTileSize + scaledTileSize / 2 - cameraX
            val screenY = creature.tileY * scaledTileSize + scaledTileSize / 2 - cameraY
            if (screenX < -50 || screenX > viewWidth + 50 || screenY < -50 || screenY > viewHeight + 50) continue
            canvas.save()
            canvas.translate(screenX, screenY)
            canvas.scale(zoom, zoom)
            creature.draw(canvas)
            canvas.restore()
        }
    }

    // ✅ Обработка нажатий
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y
                isDragging = true

                if (saveButtonRect.contains(x, y)) {
                    saveButtonPressed = true
                    WorldSave.saveWorld(world)
                    WorldSave.setAutoSaveEnabled(true)
                } else if (newWorldButtonRect.contains(x, y)) {
                    newWorldButtonPressed = true
                    WorldSave.clearSave()
                    WorldSave.setAutoSaveEnabled(false)
                    world = World(spriteManager)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && event.pointerCount == 1) {
                    if (!saveButtonPressed && !newWorldButtonPressed) {
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        cameraX -= dx
                        cameraY -= dy
                        lastTouchX = x
                        lastTouchY = y
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                saveButtonPressed = false
                newWorldButtonPressed = false
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        if (isSurfaceCreated) {
            cameraX = -viewWidth / 2f + (tileSize * zoom * 16)
            cameraY = -viewHeight / 2f + (tileSize * zoom * 16)
        }
    }
}