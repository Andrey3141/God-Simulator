package com.printer.godsimulator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

class Animation(
    private val frames: List<Bitmap>,
    private val frameDuration: Long = 150
) {
    private var currentFrame = 0
    private var lastTime = System.currentTimeMillis()
    private var isPlaying = true

    fun update() {
        if (!isPlaying || frames.isEmpty()) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > frameDuration) {
            currentFrame = (currentFrame + 1) % frames.size
            lastTime = currentTime
        }
    }

    fun draw(canvas: Canvas, x: Float, y: Float, size: Float) {
        if (frames.isEmpty()) return

        val frame = frames[currentFrame]
        val destRect = Rect(
            (x - size/2).toInt(),
            (y - size/2).toInt(),
            (x + size/2).toInt(),
            (y + size/2).toInt()
        )

        canvas.drawBitmap(frame, null, destRect, null)
    }

    fun play() { isPlaying = true }
    fun pause() { isPlaying = false }
    fun reset() { currentFrame = 0 }
}