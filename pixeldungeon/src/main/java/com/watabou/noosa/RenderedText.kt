package com.watabou.noosa

import android.graphics.*
import com.watabou.gltextures.SmartTexture
import com.watabou.glwrap.Matrix
import com.watabou.glwrap.Texture
import java.util.*
import kotlin.math.roundToInt

/**
 * Created by Void on 2019/9/24 17:41
 *
 */
class RenderedText : Image {
    private var size = 0
    private var text: String
    private var cache = CachedText()
    private var needsRender = false

    constructor() : this(0) {
        text = ""
    }

    constructor(size: Int) : this("", size) {
        text = ""
        this.size = size
    }

    constructor(text: String, size: Int) : super() {
        this.text = text
        this.size = size
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
        super.destroy()
        cache.activeTexts.remove(this)
        super.destroy()
    }

    @Synchronized
    private fun render(r: RenderedText) {
        r.needsRender = false
        r.cache.activeTexts.remove(r)
        val key = "text:${r.size} ${r.text}"
        if (textCache.containsKey(key)) {
            r.cache = textCache[key]!!
            r.texture = r.cache.texture
            r.frame = r.cache.rect
            r.cache.activeTexts.add(r)
        } else {
            measure(r)
            if (r.width == 0f || r.height == 0f) return
            /*
            * bitmap has to be in a power of 2 for some devices
            * (as we're using openGL methods to render to texture)
            * */
            val bitmap = Bitmap.createBitmap(
                Integer.highestOneBit(r.width.toInt()) * 2,
                Integer.highestOneBit(r.height.toInt()) * 2,
                Bitmap.Config.ARGB_4444
            )
            bitmap.eraseColor(Color.BLACK)
            canvas.setBitmap(bitmap)
            canvas.drawText(r.text, r.size / 10f, r.size.toFloat(), painter)
            //paint inner text
            painter.setARGB(255, 255, 255, 255)
            painter.style = Paint.Style.FILL
            canvas.drawText(r.text, r.size / 10f, r.size.toFloat(), painter)
            r.texture = SmartTexture(bitmap, Texture.NEAREST, Texture.CLAMP, true)
            val rect = r.texture.uvRect(0, 0, r.width.toInt(), r.height.toInt())
            r.frame(rect)
            r.cache.rect = rect
            r.cache.texture = r.texture
            r.cache.length = r.text.length
            r.cache.activeTexts.add(r)
            textCache["text:${r.size} ${r.text}"] = r.cache
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

    private class CachedText {
        var texture: SmartTexture? = null
        var rect: RectF? = null
        var length: Int? = null
        var activeTexts = HashSet<RenderedText>()
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
            r.width = (painter.measureText(r.text) + r.size / 5f)
            r.height = (-painter.ascent() + painter.descent() + r.size / 5f)
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
            if (asset == null) font = null
            else font = Typeface.createFromAsset(Game.instance.assets, asset)
            clearCache()
        }

        fun getFont(): Typeface? = font

    }
}