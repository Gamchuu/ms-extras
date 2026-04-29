/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023-2025, odtheking
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Portions of this file are derived from OdinFabric
 * Copyright (c) odtheking
 * Licensed under BSD-3-Clause
 *
 * Modifications and additions:
 * Licensed under GPL-3.0
 */

package gamchu.render.util

import com.mojang.blaze3d.opengl.GlDevice
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.systems.RenderSystem
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import net.minecraft.client.Minecraft
import gamchu.render.util.helper.Font
import gamchu.render.util.helper.Gradient
import gamchu.render.util.helper.Image
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG.*
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3.*
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33C
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree

@Suppress("unused")
object NVGRenderer {

  private val mc: Minecraft =
    Minecraft.getInstance()

  private val nvgPaint = NVGPaint.malloc()
  private val nvgColor = NVGColor.malloc()
  private val nvgColor2 = NVGColor.malloc()

  val interFont = Font("Roboto", "/assets/gamchu/fonts/Roboto-Regular.ttf")

  private val fontMap = HashMap<Font, NVGFont>()
  private val fontBounds = FloatArray(4)
  private val images = HashMap<Image, NVGImage>()


  private var scissor: Scissor? = null
  private var drawing: Boolean = false
  private var vg = -1L
  
  private var prevSampler = 0
  private var prevVAO = 0
  private var prevProgram = 0

  init {
    vg = nvgCreate(NVG_ANTIALIAS or NVG_STENCIL_STROKES)
    require(vg != -1L) { "Failed to initialize NanoVG" }
  }

  fun beginFrame(width: Float, height: Float) {
    check(!drawing) { "[NVGRenderer] Already drawing, but called beginFrame" }

    val framebuffer = mc.mainRenderTarget
    val glFramebuffer = (framebuffer.colorTexture as GlTexture).getFbo(
      (RenderSystem.getDevice() as GlDevice).directStateAccess(),
      null
    )

    GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, glFramebuffer)
    GlStateManager._viewport(0, 0, framebuffer.width, framebuffer.height)
    GlStateManager._activeTexture(GL30.GL_TEXTURE0)

    prevSampler = org.lwjgl.opengl.GL11.glGetInteger(GL33C.GL_SAMPLER_BINDING)
    prevVAO = org.lwjgl.opengl.GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)
    prevProgram = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM)

    GL33C.glBindSampler(0, 0)
    
    val scale = mc.window.guiScale.toFloat()
    nvgBeginFrame(vg, width, height, scale)
    nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
    drawing = true
  }

  fun endFrame() {
    check(drawing) { "[NVGRenderer] Not drawing, but called endFrame" }

    nvgEndFrame(vg)
    GlStateManager._disableCull()
    GlStateManager._disableDepthTest()
    GlStateManager._enableBlend()
    GlStateManager._blendFuncSeparate(770, 771, 1, 0)

    GL33C.glBindSampler(0, prevSampler)
    GL30.glBindVertexArray(prevVAO)
    GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
    org.lwjgl.opengl.GL20.glUseProgram(prevProgram)

    if (TextureTracker.prevActiveTexture != -1) {
      GlStateManager._activeTexture(TextureTracker.prevActiveTexture)
      if (TextureTracker.prevBoundTexture != -1) GlStateManager._bindTexture(TextureTracker.prevBoundTexture)
    }

    GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
    
    // Restore pixel store alignments so we don't break Minecraft's font glyph uploads
    GL30.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, 4)
    GL30.glPixelStorei(GL30.GL_UNPACK_ROW_LENGTH, 0)
    GL30.glPixelStorei(GL30.GL_UNPACK_SKIP_PIXELS, 0)
    GL30.glPixelStorei(GL30.GL_UNPACK_SKIP_ROWS, 0)
    
    drawing = false
  }

  @JvmStatic
  fun push() = nvgSave(vg)

  @JvmStatic
  fun pop() = nvgRestore(vg)

  @JvmStatic
  fun scale(x: Float, y: Float) = nvgScale(vg, x, y)

  @JvmStatic
  fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

  @JvmStatic
  fun rotate(amount: Float) = nvgRotate(vg, amount)

  @JvmStatic
  fun globalAlpha(amount: Float) = nvgGlobalAlpha(vg, amount.coerceIn(0f, 1f))

  @JvmStatic
  fun pushScissor(x: Float, y: Float, w: Float, h: Float) {
    scissor = Scissor(scissor, x, y, w + x, h + y)
    scissor?.applyScissor()
  }

  @JvmStatic
  fun popScissor() {
    nvgResetScissor(vg)
    scissor = scissor?.previous
    scissor?.applyScissor()
  }

  @JvmStatic
  fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
    nvgBeginPath(vg)
    nvgMoveTo(vg, x1, y1)
    nvgLineTo(vg, x2, y2)
    nvgStrokeWidth(vg, thickness)
    color(color)
    nvgStrokeColor(vg, nvgColor)
    nvgStroke(vg)
  }

  @JvmStatic
  fun drawHalfRoundedRect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float, roundTop: Boolean) {
    nvgBeginPath(vg)

    if (roundTop) {
      nvgMoveTo(vg, x, y + h)
      nvgLineTo(vg, x + w, y + h)
      nvgLineTo(vg, x + w, y + radius)
      nvgArcTo(vg, x + w, y, x + w - radius, y, radius)
      nvgLineTo(vg, x + radius, y)
      nvgArcTo(vg, x, y, x, y + radius, radius)
      nvgLineTo(vg, x, y + h)
    } else {
      nvgMoveTo(vg, x, y)
      nvgLineTo(vg, x + w, y)
      nvgLineTo(vg, x + w, y + h - radius)
      nvgArcTo(vg, x + w, y + h, x + w - radius, y + h, radius)
      nvgLineTo(vg, x + radius, y + h)
      nvgArcTo(vg, x, y + h, x, y + h - radius, radius)
      nvgLineTo(vg, x, y)
    }

    nvgClosePath(vg)
    color(color)
    nvgFillColor(vg, nvgColor)
    nvgFill(vg)
  }

  @JvmStatic
  fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) {
    nvgBeginPath(vg)
    nvgRoundedRect(vg, x, y, w, h + .5f, radius)
    color(color)
    nvgFillColor(vg, nvgColor)
    nvgFill(vg)
  }

  @JvmStatic
  fun rect(x: Float, y: Float, w: Float, h: Float, color: Int) {
    nvgBeginPath(vg)
    nvgRect(vg, x, y, w, h + .5f)
    color(color)
    nvgFillColor(vg, nvgColor)
    nvgFill(vg)
  }

  @JvmStatic
  fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float) {
    nvgBeginPath(vg)
    nvgRoundedRect(vg, x, y, w, h, radius)
    nvgStrokeWidth(vg, thickness)
    nvgPathWinding(vg, NVG_HOLE)
    color(color)
    nvgStrokeColor(vg, nvgColor)
    nvgStroke(vg)
  }

  @JvmStatic
  fun hollowGradientRect(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    thickness: Float,
    color1: Int,
    color2: Int,
    gradient: Gradient,
    radius: Float,
  ) {
    nvgBeginPath(vg)
    nvgRoundedRect(vg, x, y, w, h, radius)
    nvgStrokeWidth(vg, thickness)
    gradient(color1, color2, x, y, w, h, gradient)
    nvgStrokePaint(vg, nvgPaint)
    nvgStroke(vg)
  }

  @JvmStatic
  fun gradientRect(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    color1: Int,
    color2: Int,
    gradient: Gradient,
    radius: Float,
  ) {
    nvgBeginPath(vg)
    nvgRoundedRect(vg, x, y, w, h, radius)
    gradient(color1, color2, x, y, w, h, gradient)
    nvgFillPaint(vg, nvgPaint)
    nvgFill(vg)
  }

  @JvmStatic
  fun circle(x: Float, y: Float, radius: Float, color: Int) {
    nvgBeginPath(vg)
    nvgCircle(vg, x, y, radius)
    color(color)
    nvgFillColor(vg, nvgColor)
    nvgFill(vg)
  }

  @JvmStatic
  fun text(text: String, x: Float, y: Float, size: Float, color: Int, font: Font = interFont) {
    nvgFontSize(vg, size)
    nvgFontFaceId(vg, getFontID(font))
    color(color)
    nvgFillColor(vg, nvgColor)
    nvgText(vg, x, y + .5f, text)
  }

  @JvmStatic
  fun textShadow(text: String, x: Float, y: Float, size: Float, color: Int, font: Font = interFont) {
    nvgFontFaceId(vg, getFontID(font))
    nvgFontSize(vg, size)
    color(-16777216)
    nvgFillColor(vg, nvgColor)
    nvgText(vg, round(x + 3f), round(y + 3f), text)

    color(color)
    nvgFillColor(vg, nvgColor)
    nvgText(vg, round(x), round(y), text)
  }

  @JvmStatic
  fun textWidth(text: String, size: Float, font: Font = interFont): Float {
    nvgFontSize(vg, size)
    nvgFontFaceId(vg, getFontID(font))
    return nvgTextBounds(vg, 0f, 0f, text, fontBounds)
  }

  @JvmStatic
  fun drawWrappedString(
    text: String,
    x: Float,
    y: Float,
    w: Float,
    size: Float,
    color: Int,
    font: Font = interFont,
    lineHeight: Float = 1f,
  ) {
    nvgFontSize(vg, size)
    nvgFontFaceId(vg, getFontID(font))
    nvgTextLineHeight(vg, lineHeight)
    color(color)
    nvgFillColor(vg, nvgColor)
    nvgTextBox(vg, x, y, w, text)
  }

  @JvmStatic
  fun getWrappedStringHeight(text: String, maxWidth: Float, fontSize: Float, font: Font = interFont): Float {
    nvgFontSize(vg, fontSize)
    nvgFontFaceId(vg, getFontID(font))

    val bounds = FloatArray(4)
    nvgTextBoxBounds(vg, 0f, 0f, maxWidth, text, bounds)

    return bounds[3] - bounds[1]
  }

  @JvmStatic
  fun wrappedTextBounds(
    text: String,
    w: Float,
    size: Float,
    font: Font,
    lineHeight: Float = 1f,
  ): FloatArray {
    val bounds = FloatArray(4)
    nvgFontSize(vg, size)
    nvgFontFaceId(vg, getFontID(font))
    nvgTextLineHeight(vg, lineHeight)
    nvgTextBoxBounds(vg, 0f, 0f, w, text, bounds)
    return bounds
  }

  @JvmStatic
  fun createNVGImage(textureId: Int, textureWidth: Int, textureHeight: Int): Int =
    nvglCreateImageFromHandle(
      vg,
      textureId,
      textureWidth,
      textureHeight,
      NVG_IMAGE_NEAREST or NVG_IMAGE_NODELETE
    )

  @JvmStatic
  fun image(image: Image, x: Float, y: Float, w: Float, h: Float, radius: Float = 0F, colorMask: Int = 0) {
    nvgImagePattern(vg, x, y, w, h, 0f, getImage(image), 1f, nvgPaint)

    if (colorMask != 0) {
      nvgRGBA(
        colorMask.red.toByte(),
        colorMask.green.toByte(),
        colorMask.blue.toByte(),
        colorMask.alpha.toByte(),
        nvgPaint.innerColor()
      )
    }

    nvgBeginPath(vg)

    if (radius == 0F)
      nvgRect(vg, x, y, w, h + .5f)
    else
      nvgRoundedRect(vg, x, y, w, h + .5f, radius)

    nvgFillPaint(vg, nvgPaint)
    nvgFill(vg)
  }

  @JvmStatic
  fun createImage(resourcePath: String): Image {
    val image = images.keys.find { it.identifier == resourcePath } ?: Image(resourcePath)
    if (image.isSVG) images.getOrPut(image) { NVGImage(0, loadSVG(image)) }.count++
    else images.getOrPut(image) { NVGImage(0, loadImage(image)) }.count++
    return image
  }

  @JvmStatic
  fun deleteImage(image: Image) {
    val nvgImage = images[image] ?: return
    nvgImage.count--
    if (nvgImage.count == 0) {
      nvgDeleteImage(vg, nvgImage.nvg)
      images.remove(image)
    }
  }

  private fun getImage(image: Image): Int {
    return images[image]?.nvg ?: throw IllegalStateException("Image (${image.identifier}) doesn't exist")
  }

  private fun loadImage(image: Image): Int {
    val w = IntArray(1)
    val h = IntArray(1)
    val channels = IntArray(1)
    val buffer = stbi_load_from_memory(image.buffer(), w, h, channels, 4)
      ?: throw NullPointerException("Failed to load image: ${image.identifier}")
    try {
      return nvgCreateImageRGBA(vg, w[0], h[0], 0, buffer)
    } finally {
      stbi_image_free(buffer)
    }
  }

  private fun loadSVG(image: Image): Int {
    val vec = image.stream.use { it.bufferedReader().readText() }
    val svg = nsvgParse(vec, "px", 96f) ?: throw IllegalStateException("Failed to parse ${image.identifier}")

    val width = svg.width().toInt()
    val height = svg.height().toInt()
    val buffer = memAlloc(width * height * 4)

    try {
      val rasterizer = nsvgCreateRasterizer()
      nsvgRasterize(rasterizer, svg, 0f, 0f, 1f, buffer, width, height, width * 4)
      val nvgImage = nvgCreateImageRGBA(vg, width, height, 0, buffer)
      nsvgDeleteRasterizer(rasterizer)
      return nvgImage
    } finally {
      nsvgDelete(svg)
      memFree(buffer)
    }
  }

  private fun color(color: Int) {
    nvgRGBA(color.red.toByte(), color.green.toByte(), color.blue.toByte(), color.alpha.toByte(), nvgColor)
  }

  private fun color(color1: Int, color2: Int) {
    nvgRGBA(
      color1.red.toByte(),
      color1.green.toByte(),
      color1.blue.toByte(),
      color1.alpha.toByte(),
      nvgColor
    )
    nvgRGBA(
      color2.red.toByte(),
      color2.green.toByte(),
      color2.blue.toByte(),
      color2.alpha.toByte(),
      nvgColor2
    )
  }

  private fun gradient(color1: Int, color2: Int, x: Float, y: Float, w: Float, h: Float, direction: Gradient) {
    color(color1, color2)
    when (direction) {
      Gradient.LeftToRight -> nvgLinearGradient(vg, x, y, x + w, y, nvgColor, nvgColor2, nvgPaint)
      Gradient.TopToBottom -> nvgLinearGradient(vg, x, y, x, y + h, nvgColor, nvgColor2, nvgPaint)
      Gradient.TopLeftToBottomRight -> nvgLinearGradient(vg, x, y, x + w, y + h, nvgColor, nvgColor2, nvgPaint)
    }
  }

  private fun getFontID(font: Font): Int {
    return fontMap.getOrPut(font) {
      val buffer = font.buffer()
      NVGFont(
        nvgCreateFontMem(
          vg,
          font.name,
          buffer,
          false
        ), buffer
      )
    }.id
  }

  private class Scissor(val previous: Scissor?, val x: Float, val y: Float, val maxX: Float, val maxY: Float) {
    fun applyScissor() {
      if (previous == null) nvgScissor(vg, x, y, maxX - x, maxY - y)
      else {
        val x = max(x, previous.x)
        val y = max(y, previous.y)
        val width = max(0f, (min(maxX, previous.maxX) - x))
        val height = max(0f, (min(maxY, previous.maxY) - y))
        nvgScissor(vg, x, y, width, height)
      }
    }
  }

  private data class NVGImage(var count: Int, val nvg: Int)
  private data class NVGFont(val id: Int, val buffer: ByteBuffer)

  inline val Int.red get() = this shr 16 and 0xFF
  inline val Int.green get() = this shr 8 and 0xFF
  inline val Int.blue get() = this and 0xFF
  inline val Int.alpha get() = this shr 24 and 0xFF

}
