package com.printer.godsimulator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.abs
import kotlin.random.Random

enum class CreatureType { HUMAN, ANIMAL, MONSTER }
enum class Gender { MALE, FEMALE }

class Creature(var tileX: Int, var tileY: Int, var type: CreatureType = CreatureType.HUMAN, var gender: Gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE, private val pixelSize: Float = 8f, val tileSize: Int = GameConfig.TILE_SIZE) {
    private var x: Float = (tileX * tileSize + tileSize/2).toFloat()
    private var y: Float = (tileY * tileSize + tileSize/2).toFloat()
    var age = Random.nextInt(0, 80)
    var health = 100
    var hunger = Random.nextInt(0, 100)
    var speed = Random.nextFloat() * (GameConfig.CREATURE_SPEED_MAX - GameConfig.CREATURE_SPEED_MIN) + GameConfig.CREATURE_SPEED_MIN
    enum class State { IDLE, WALKING, EATING, SLEEPING, FIGHTING }
    var state = State.IDLE
    var stateTime = 0
    var targetX = tileX
    var targetY = tileY
    var isMoving = false
    private val skinColor = when (type) { CreatureType.HUMAN -> Color.rgb(255, 220, 200); CreatureType.ANIMAL -> Color.rgb(160, 120, 80); CreatureType.MONSTER -> Color.rgb(100, 200, 100) }
    private val clothesColor = when (gender) { Gender.MALE -> Color.rgb(70, 130, 180); Gender.FEMALE -> Color.rgb(255, 100, 150) }
    private val hairColor = Color.rgb(101, 67, 33)

    fun update() {
        stateTime++
        when (state) {
            State.IDLE -> { if (stateTime > Random.nextInt(GameConfig.CREATURE_IDLE_TIME_MIN, GameConfig.CREATURE_IDLE_TIME_MAX)) startRandomWalk() }
            State.WALKING -> { moveToTarget() }
            else -> {}
        }
        hunger = (hunger + 0.1f).toInt().coerceAtMost(100)
        if (hunger > 80) health = (health - 0.5f).toInt().coerceAtLeast(0)
        if (health <= 0 || age > 100) {}
    }

    private fun startRandomWalk() {
        val dx = Random.nextInt(-GameConfig.CREATURE_WALK_DISTANCE, GameConfig.CREATURE_WALK_DISTANCE + 1)
        val dy = Random.nextInt(-GameConfig.CREATURE_WALK_DISTANCE, GameConfig.CREATURE_WALK_DISTANCE + 1)
        targetX = tileX + dx; targetY = tileY + dy
        if (targetX in 0..1000 && targetY in 0..1000) { state = State.WALKING; isMoving = true; stateTime = 0 }
    }

    private fun moveToTarget() {
        val targetPixelX = (targetX * tileSize + tileSize/2).toFloat()
        val targetPixelY = (targetY * tileSize + tileSize/2).toFloat()
        val dx = targetPixelX - x; val dy = targetPixelY - y
        if (abs(dx) < speed && abs(dy) < speed) { x = targetPixelX; y = targetPixelY; tileX = targetX; tileY = targetY; state = State.IDLE; isMoving = false; stateTime = 0 }
        else { x += dx.coerceIn(-speed, speed); y += dy.coerceIn(-speed, speed) }
    }

    fun draw(canvas: Canvas) {
        when (type) {
            CreatureType.HUMAN -> drawHuman(canvas)
            CreatureType.ANIMAL -> drawAnimal(canvas)
            CreatureType.MONSTER -> drawMonster(canvas)
        }
        if (health < 100) drawHealthBar(canvas)
    }

    private fun drawHuman(canvas: Canvas) {
        canvas.drawCircle(x, y - pixelSize * 2f, pixelSize * 1.5f, Paint().apply { color = skinColor; isAntiAlias = true })
        val eyePaint = Paint().apply { color = Color.BLACK }
        canvas.drawCircle(x - pixelSize/2f, y - pixelSize * 2.5f, pixelSize/3f, eyePaint)
        canvas.drawCircle(x + pixelSize/2f, y - pixelSize * 2.5f, pixelSize/3f, eyePaint)
        canvas.drawRect(x - pixelSize, y - pixelSize, x + pixelSize, y + pixelSize, Paint().apply { color = clothesColor })
        canvas.drawLine(x - pixelSize * 1.5f, y - pixelSize/2f, x - pixelSize, y, Paint().apply { color = clothesColor; strokeWidth = pixelSize/2f })
        canvas.drawLine(x + pixelSize * 1.5f, y - pixelSize/2f, x + pixelSize, y, Paint().apply { color = clothesColor; strokeWidth = pixelSize/2f })
        canvas.drawLine(x - pixelSize/2f, y + pixelSize, x - pixelSize, y + pixelSize * 2f, Paint().apply { color = Color.rgb(80, 50, 30); strokeWidth = pixelSize/2f })
        canvas.drawLine(x + pixelSize/2f, y + pixelSize, x + pixelSize, y + pixelSize * 2f, Paint().apply { color = Color.rgb(80, 50, 30); strokeWidth = pixelSize/2f })
        if (gender == Gender.MALE) canvas.drawCircle(x, y - pixelSize * 2.5f, pixelSize, Paint().apply { color = hairColor })
        else { canvas.drawCircle(x - pixelSize/2f, y - pixelSize * 3f, pixelSize/2f, Paint().apply { color = hairColor }); canvas.drawCircle(x + pixelSize/2f, y - pixelSize * 3f, pixelSize/2f, Paint().apply { color = hairColor }) }
    }

    private fun drawAnimal(canvas: Canvas) {
        canvas.drawCircle(x, y, pixelSize * 2f, Paint().apply { color = Color.rgb(255, 200, 200); isAntiAlias = true })
        canvas.drawCircle(x - pixelSize, y - pixelSize, pixelSize, Paint().apply { color = Color.rgb(200, 150, 150); isAntiAlias = true })
        canvas.drawCircle(x + pixelSize, y - pixelSize, pixelSize, Paint().apply { color = Color.rgb(200, 150, 150); isAntiAlias = true })
        canvas.drawCircle(x - pixelSize/2f, y - pixelSize, pixelSize/3f, Paint().apply { color = Color.BLACK })
        canvas.drawCircle(x + pixelSize/2f, y - pixelSize, pixelSize/3f, Paint().apply { color = Color.BLACK })
    }

    private fun drawMonster(canvas: Canvas) {
        canvas.drawCircle(x, y, pixelSize * 2f, Paint().apply { color = Color.rgb(100, 200, 100); isAntiAlias = true })
        canvas.drawRect(x - pixelSize, y, x + pixelSize, y + pixelSize * 2f, Paint().apply { color = Color.rgb(50, 150, 50) })
        canvas.drawCircle(x - pixelSize, y - pixelSize/2f, pixelSize/2f, Paint().apply { color = Color.RED })
        canvas.drawCircle(x + pixelSize, y - pixelSize/2f, pixelSize/2f, Paint().apply { color = Color.RED })
    }

    private fun drawHealthBar(canvas: Canvas) {
        val barWidth = pixelSize * 3f; val barHeight = pixelSize / 2f; val healthWidth = barWidth * health / 100f
        canvas.drawRect(x - barWidth/2f, y - pixelSize * 4f, x + barWidth/2f, y - pixelSize * 4f + barHeight, Paint().apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 1f })
        canvas.drawRect(x - barWidth/2f, y - pixelSize * 4f, x - barWidth/2f + healthWidth, y - pixelSize * 4f + barHeight, Paint().apply { color = Color.GREEN })
    }
}