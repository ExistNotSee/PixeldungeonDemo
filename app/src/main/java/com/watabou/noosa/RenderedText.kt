package com.watabou.noosa

import android.graphics.*
import android.util.Log
import com.watabou.gltextures.SmartTexture
import com.watabou.glwrap.Matrix
import com.watabou.glwrap.Texture
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.roundToInt

/**
 * Created by Void on 2019/9/24 17:41
 *
 */
class RenderedText(private var text: String = "", private var size: Int = 0) : Image() {
    private var needsRender = false

    init {
        needsRender = true
        measure(this)
    }

    override fun updateMatrix() {
        super.updateMatrix()
        Matrix.translate(matrix, 0f, -((baseLine() * 0.15f / scale.y).roundToInt().toFloat()))
    }

    override fun draw() {
        if (needsRender) render(this)
        if (texture != null) super.draw()
    }

    override fun destroy() {
        getCache()?.activeTexts?.remove(this)
        super.destroy()
    }

    private fun getCache(): CachedText? = textCache["text:${this.size} ${this.text}"]

    @Synchronized
    private fun render(r: RenderedText) {
        Log.w("-------------", "渲染-----:${r.text}")
        r.needsRender = false
        val rectF: RectF
        val cache = getCache()

        measure(r)
        if (r.width == 0f || r.height == 0f) return
        val bitmap = cache?.bitmap ?: Bitmap.createBitmap(
            Integer.highestOneBit(r.width.toInt()) * 2,
            Integer.highestOneBit(r.height.toInt()) * 2,
            Bitmap.Config.ARGB_8888
        )
        //文字的背景颜色
//        bitmap.eraseColor(Color.BLACK)
        canvas.setBitmap(bitmap)
        //paint inner text
        canvas.drawText(r.text, r.size / 10f, r.size.toFloat(), painter)
        painter.setARGB(255, 255, 255, 255)
        painter.style = Paint.Style.FILL
        canvas.drawText(r.text, r.size / 10f, r.size.toFloat(), painter)
        if (cache == null) {
            r.texture = SmartTexture(bitmap, Texture.NEAREST, Texture.CLAMP, true)
            rectF = r.texture.uvRect(0, 0, r.width.toInt(), r.height.toInt())
        } else {
            r.texture = cache.texture
            rectF = cache.rectF
        }
        r.frame(rectF)

        if (cache != null) return
        textCache["text:${r.size} ${r.text}"] = CachedText(
            bitmap,
            r.texture,
            rectF,
            r.text.length
        ).apply {
            activeTexts.add(r)
        }
    }

    fun text(text: String) {
        this.text = text
        needsRender = true
        measure(this)
    }

    fun getText() = text

    fun size(size: Int) {
        this.size = size
        needsRender = true
        measure(this)
    }

    fun baseLine(): Float = size * scale.y

    private data class CachedText(
        val bitmap: Bitmap,
        val texture: SmartTexture,
        val rectF: RectF,
        val length: Int
    ) {
        val activeTexts = HashSet<RenderedText>()
    }

    companion object {
        val canvas = Canvas()
        val painter = Paint()
        private var font: Typeface? = null
        private val textCache = object : LinkedHashMap<String, CachedText>(700, 0.75f, true) {
            private var cachedChars = 0
            private val MAX_CACHED = 1000

            override fun put(key: String, value: CachedText): CachedText? {
                cachedChars += value.length ?: 0
                val added = super.put(key, value)
                val it = this.entries.iterator()
                while (cachedChars > MAX_CACHED && it.hasNext()) {
                    val cached = it.next().value
                    if (cached.activeTexts.isEmpty()) it.remove()
                }
                return added
            }

            override fun remove(key: String): CachedText? {
                val removed = super.remove(key)
                if (removed != null) {
                    cachedChars -= removed.length ?: 0
                    removed.texture?.delete()
                }
                return removed
            }

            override fun clear() {
                super.clear()
                cachedChars = 0
            }
        }

        @Synchronized
        private fun measure(r: RenderedText) {
            Log.w("-------------", "测量-----:" + r.text)
            if (r.text == "") {
                r.text = ""
                r.width = 0f
                r.height = 0f
                r.visible = false
                return
            }
            r.visible = true
            painter.textSize = r.size.toFloat()
            painter.isAntiAlias = true
            if (font != null) {
                painter.typeface = font
            } else {
                painter.typeface = Typeface.DEFAULT
            }
            //paint outer strokes
            painter.setARGB(255, 0, 0, 0)
            painter.style = Paint.Style.STROKE
            painter.strokeWidth = r.size / 5f
            r.width = painter.measureText(r.text) + r.size / 5f
            r.height = -painter.ascent() + painter.descent() + r.size / 5f
        }

        fun clearCache() {
            for (cached in textCache.values) {
                cached.texture?.delete()
            }
            textCache.clear()
        }

        fun reloadCache() {
            for (text in textCache.values) {
                text.texture?.reload()
            }
        }

        fun setFont(asset: String?) {
            font = if (asset == null) null
            else Typeface.createFromAsset(Game.instance.assets, asset)
            clearCache()
        }

        fun getFont(): Typeface? = font

    }
}