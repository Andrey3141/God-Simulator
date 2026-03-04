package com.printer.godsimulator

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.*
import kotlin.math.floor

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameLoopThread: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var spriteManager: SpriteManager
    private lateinit var world: World
    private val tileSize = 24

    // Текстуры
    private var grassTexture: Bitmap? = null
    private var grassTexturePaint: Paint? = null

    private var cameraX = 0f
    private var cameraY = 0f
    private var zoom = 1.0f

    private var isSurfaceCreated = false
    private var viewWidth = 0
    private var viewHeight = 0

    // Для управления камерой
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // Для зума
    private var scaleGestureDetector: ScaleGestureDetector

    // Для отладки
    private var lastFpsTime = System.currentTimeMillis()
    private var fps = 0
    private var frameCount = 0

    // Кешированные кисти
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
        textSize = 20f
        isAntiAlias = true
        setShadowLayer(2f, 2f, 2f, Color.BLACK)
    }

    // Кеш для цветов тайлов
    private val tileColorCache = mutableMapOf<TileType, Paint>()

    init {
        holder.addCallback(this)

        // Загружаем текстуры
        loadTextures(context)
        initTileColorCache()

        spriteManager = SpriteManager(context)
        world = World(spriteManager)

        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoom *= detector.scaleFactor
                zoom = zoom.coerceIn(0.5f, 3f)
                return true
            }
        })
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

        // Центрируем камеру
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
    }

    private fun startGameLoop() {
        gameLoopThread = scope.launch {
            while (isActive && isSurfaceCreated) {
                val startTime = System.currentTimeMillis()

                update()
                draw()

                // Целимся в 60 FPS
                val frameTime = System.currentTimeMillis() - startTime
                if (frameTime < 16) {
                    delay(16 - frameTime)
                }
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopThread?.cancel()
        gameLoopThread = null
    }

    private fun update() {
        // Получаем видимые чанки
        val visibleChunks = getVisibleChunks()

        // Обновляем мир
        world.updateVisibleChunks(visibleChunks)

        // Обновляем всех существ в видимых чанках
        for (chicken in world.getChickensInChunks(visibleChunks)) {
            chicken.update()
        }

        for (creature in world.getCreaturesInChunks(visibleChunks)) {
            creature.update()
        }

        // Расчет FPS
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

        // Вычисляем видимые тайлы
        val startTileX = floor((cameraX - tileSize) / scaledTileSize).toInt() - 1
        val startTileY = floor((cameraY - tileSize) / scaledTileSize).toInt() - 1
        val endTileX = floor((cameraX + viewWidth + tileSize) / scaledTileSize).toInt() + 1
        val endTileY = floor((cameraY + viewHeight + tileSize) / scaledTileSize).toInt() + 1

        val chunkSize = 32
        val visibleChunks = mutableSetOf<Pair<Int, Int>>()

        for (x in startTileX..endTileX) {
            for (y in startTileY..endTileY) {
                val chunkX = floor(x.toDouble() / chunkSize).toInt()
                val chunkY = floor(y.toDouble() / chunkSize).toInt()
                visibleChunks.add(Pair(chunkX, chunkY))
            }
        }

        return visibleChunks
    }

    private fun draw() {
        if (!isSurfaceCreated || viewWidth == 0 || viewHeight == 0) {
            return
        }

        val canvas = holder.lockCanvas() ?: return
        try {
            // Небо
            canvas.drawColor(Color.rgb(135, 206, 235))

            // Рисуем мир
            drawWorld(canvas)

            // Рисуем существ
            val visibleChunks = getVisibleChunks()
            drawChickens(canvas, visibleChunks)
            drawCreatures(canvas, visibleChunks)

            // Отладка
            drawDebugInfo(canvas, visibleChunks)

        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawWorld(canvas: Canvas) {
        val scaledTileSize = (tileSize * zoom).toInt()
        if (scaledTileSize <= 0) return

        // Вычисляем видимые тайлы
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

                // Проверяем видимость
                if (screenX + size < 0 || screenX > viewWidth ||
                    screenY + size < 0 || screenY > viewHeight) {
                    continue
                }

                // Рисуем тайл
                if (tile.type == TileType.GRASS && grassTexture != null) {
                    val dstRect = Rect(
                        screenX.toInt(),
                        screenY.toInt(),
                        (screenX + size).toInt(),
                        (screenY + size).toInt()
                    )
                    canvas.drawBitmap(grassTexture!!, null, dstRect, grassTexturePaint)
                } else {
                    canvas.drawRect(screenX, screenY, screenX + size, screenY + size,
                        tileColorCache[tile.type] ?: tileColorCache[TileType.GRASS]!!)
                }

                // Рисуем деревья
                if (tile.treeCount > 0 && zoom > 1.0f) {
                    drawTrees(canvas, screenX, screenY, size, tile.treeCount)
                }
            }
        }
    }

    private fun drawTrees(canvas: Canvas, x: Float, y: Float, size: Float, count: Int) {
        for (i in 0 until count.coerceAtMost(3)) {
            val treeX = x + size * (0.3f + i * 0.2f)
            val treeY = y + size * 0.5f

            // Ствол
            canvas.drawRect(treeX - size/12, treeY, treeX + size/12, treeY + size/4, treeTrunkPaint)

            // Крона
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

            if (screenX < -50 || screenX > viewWidth + 50 ||
                screenY < -50 || screenY > viewHeight + 50) {
                continue
            }

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

            if (screenX < -50 || screenX > viewWidth + 50 ||
                screenY < -50 || screenY > viewHeight + 50) {
                continue
            }

            canvas.save()
            canvas.translate(screenX, screenY)
            canvas.scale(zoom, zoom)
            creature.draw(canvas)
            canvas.restore()
        }
    }

    private fun drawDebugInfo(canvas: Canvas, visibleChunks: Set<Pair<Int, Int>>) {
        canvas.drawText("FPS: $fps", 50f, 50f, textPaint)
        canvas.drawText("Зум: ${"%.1f".format(zoom)}x", 50f, 80f, textPaint)
        canvas.drawText("Кур: ${world.allChickens.size}", 50f, 110f, textPaint)
        canvas.drawText("Существ: ${world.allCreatures.size}", 50f, 140f, textPaint)

        val hintPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 16f
        }
        canvas.drawText("👆 Тащи", viewWidth - 50f, 50f, hintPaint)
        canvas.drawText("🤏 Зум", viewWidth - 50f, 80f, hintPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    cameraX -= dx
                    cameraY -= dy

                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
    }
}