package com.atmaweapon.composable2dos

import com.atmaweapon.composable2dos.extensions.enablePointerCapture
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlin.math.*

// ─── OKHSV color math ────────────────────────────────────────────────────────
// Based on Björn Ottosson's OKHSV (https://bottosson.github.io/posts/colorpicker/)

private fun srgbToLinear(c: Float): Float =
    if (c >= 0.04045f) ((c + 0.055f) / 1.055f).pow(2.4f) else c / 12.92f

private fun linearToSrgb(c: Float): Float =
    if (c >= 0.0031308f) 1.055f * c.pow(1f / 2.4f) - 0.055f else 12.92f * c

private fun linearRgbToOklab(r: Float, g: Float, b: Float): FloatArray {
    val l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
    val m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
    val s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b
    val l_ = l.pow(1f / 3f)
    val m_ = m.pow(1f / 3f)
    val s_ = s.pow(1f / 3f)
    return floatArrayOf(
        0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_,
        1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
        0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_
    )
}

private fun oklabToLinearRgb(L: Float, a: Float, b: Float): FloatArray {
    val l_ = L + 0.3963377774f * a + 0.2158037573f * b
    val m_ = L - 0.1055613458f * a - 0.0638541728f * b
    val s_ = L - 0.0894841775f * a - 1.2914855480f * b
    val l = l_ * l_ * l_
    val m = m_ * m_ * m_
    val s = s_ * s_ * s_
    return floatArrayOf(
        4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    )
}

private fun computeMaxSaturation(a: Float, b: Float): Float {
    val k0: Float; val k1: Float; val k2: Float; val k3: Float; val k4: Float
    val wl: Float; val wm: Float; val ws: Float
    if (-1.88170328f * a - 0.80936493f * b > 1f) {
        k0 = 1.19086277f; k1 = 1.76576728f; k2 = 0.59662007f; k3 = 0.75515197f; k4 = 0.56771245f
        wl = 4.0767416621f; wm = -3.3077115913f; ws = 0.2309699292f
    } else if (1.81444104f * a - 1.19445276f * b > 1f) {
        k0 = 0.73956515f; k1 = -0.45954494f; k2 = 0.08285427f; k3 = 0.12541070f; k4 = 0.14503204f
        wl = -1.2684380046f; wm = 2.6097574011f; ws = -0.3413193965f
    } else {
        k0 = 1.35733652f; k1 = -0.00915799f; k2 = -1.15130210f; k3 = -0.50559606f; k4 = 0.00692167f
        wl = -0.0041960863f; wm = -0.7034186147f; ws = 1.7076147010f
    }
    var S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b
    repeat(2) {
        val kl = 0.3963377774f * a + 0.2158037573f * b
        val km = -0.1055613458f * a - 0.0638541728f * b
        val ks = -0.0894841775f * a - 1.2914855480f * b
        val l_ = 1f + S * kl; val m_ = 1f + S * km; val s_ = 1f + S * ks
        val l = l_ * l_ * l_; val m = m_ * m_ * m_; val s = s_ * s_ * s_
        val f = wl * l + wm * m + ws * s
        val f1 = wl * 3f * kl * l_ * l_ + wm * 3f * km * m_ * m_ + ws * 3f * ks * s_ * s_
        S -= f / f1
    }
    return S
}

private fun findCusp(a: Float, b: Float): FloatArray {
    val sCusp = computeMaxSaturation(a, b)
    val rgb = oklabToLinearRgb(1f, sCusp * a, sCusp * b)
    val lCusp = (1f / maxOf(maxOf(rgb[0], rgb[1]), rgb[2]).coerceAtLeast(1e-8f)).pow(1f / 3f)
    return floatArrayOf(lCusp, lCusp * sCusp)
}

data class OkhsvColor(
    val hue: Float,        // 0..1
    val saturation: Float, // 0..1
    val value: Float,      // 0..1
    val alpha: Float = 1f
) {
    fun toRGB(): Color {
        if (value < 1e-6f) return Color(alpha, 0f, 0f, 0f)
        val h = hue * 2f * PI.toFloat()
        val a_ = cos(h)
        val b_ = sin(h)
        val (lCusp, cCusp) = findCusp(a_, b_).let { Pair(it[0], it[1]) }
        val Sv = cCusp / lCusp
        val L = value * lCusp
        val C = saturation * Sv * value
        val lab = oklabToLinearRgb(L, C * a_, C * b_)
        return Color(
            alpha,
            linearToSrgb(lab[0].coerceIn(0f, 1f)),
            linearToSrgb(lab[1].coerceIn(0f, 1f)),
            linearToSrgb(lab[2].coerceIn(0f, 1f))
        )
    }

    companion object {
        fun fromRGB(color: Color): OkhsvColor {
            val r = srgbToLinear(color.red.coerceIn(0f, 1f))
            val g = srgbToLinear(color.green.coerceIn(0f, 1f))
            val b = srgbToLinear(color.blue.coerceIn(0f, 1f))
            val lab = linearRgbToOklab(r, g, b)
            val L = lab[0]; val a = lab[1]; val bOk = lab[2]
            val C = sqrt(a * a + bOk * bOk)
            val hue = ((atan2(bOk, a) / (2f * PI.toFloat())) + 1f) % 1f
            if (C < 1e-6f || L < 1e-6f) return OkhsvColor(hue, 0f, L / 1f, color.alpha)
            val a_ = a / C; val b_ = bOk / C
            val (lCusp, cCusp) = findCusp(a_, b_).let { Pair(it[0], it[1]) }
            val Sv = if (lCusp > 1e-6f) cCusp / lCusp else 0f
            val value = if (lCusp > 1e-6f) (L / lCusp).coerceIn(0f, 1f) else 0f
            val saturation = if (value > 1e-6f && Sv > 1e-6f) (C / (Sv * value)).coerceIn(0f, 1f) else 0f
            return OkhsvColor(hue, saturation, value, color.alpha)
        }
    }
}

// ─── Helper to compute OKHSV color from hue alone (s=1, v=1) for strips ──────
private fun okhsvHueColor(hue: Float): Color =
    OkhsvColor(hue, 1f, 1f).toRGB()

// ─── Color picker component ───────────────────────────────────────────────────

fun ViewWriter.okhsvColorPicker(color: MutableReactive<Color>) {
    col {
        val okhsv = MutableRemember { OkhsvColor.fromRGB(color()) }

        reactiveSuspending {
            color.set(okhsv().toRGB())
        }
        reactive {
            if (okhsv.state.ready) {
                val parentColor = color()
                val local = okhsv.state.raw.toRGB()
                val tolerance = 0.01f
                val isDifferent =
                    abs(parentColor.red - local.red) > tolerance ||
                        abs(parentColor.green - local.green) > tolerance ||
                        abs(parentColor.blue - local.blue) > tolerance
                if (isDifferent) {
                    okhsv.value = OkhsvColor.fromRGB(parentColor)
                }
            }
        }

        // 2D saturation/value panel
        sizeConstraints(minWidth = 16.rem, aspectRatio = 1.0).canvas {
            val svDelegate = object : CanvasDelegate() {
                private var isDragging = false

                private fun update(x: Double, y: Double, w: Double, h: Double) {
                    val s = (x / w).toFloat().coerceIn(0f, 1f)
                    val v = (1.0 - y / h).toFloat().coerceIn(0f, 1f)
                    okhsv.value = okhsv.state.raw.copy(saturation = s, value = v)
                    invalidate()
                }

                override fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    isDragging = true; update(x, y, width, height); return true
                }
                override fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    if (isDragging) update(x, y, width, height); return true
                }
                override fun onPointerUp(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    isDragging = false; return true
                }

                override fun draw(context: DrawingContext2D) {
                    val w = context.width; val h = context.height
                    val current = okhsv.state.raw
                    val numStrips = 64
                    val stripW = w / numStrips

                    for (i in 0 until numStrips) {
                        val x0 = i * stripW
                        val x1 = x0 + stripW + 1.0
                        val sat = i.toFloat() / numStrips.toFloat()
                        val topColor = OkhsvColor(current.hue, sat, 1f).toRGB()
                        context.fillPaint = LinearGradient(
                            stops = listOf(GradientStop(0f, topColor), GradientStop(1f, Color.black)),
                            x0 = 0.0, y0 = 0.0, x1 = 0.0, y1 = h,
                        )
                        context.fillRect(x0, 0.0, x1 - x0, h)
                    }

                    // Indicator circle
                    val cx = current.saturation * w
                    val cy = (1f - current.value) * h
                    val r = 6.0
                    context.strokePaint = Color.white
                    context.lineWidth = 2.0
                    context.beginPath()
                    context.appendArc(cx, cy, r, Angle.zero, Angle(1f), false)
                    context.stroke()
                    context.strokePaint = Color.black
                    context.lineWidth = 1.0
                    context.beginPath()
                    context.appendArc(cx, cy, r + 1.5, Angle.zero, Angle(1f), false)
                    context.stroke()
                }

            }
            this.delegate = svDelegate
            svDelegate.invalidate = { this.delegate = svDelegate }
            reactive { okhsv(); svDelegate.invalidate() }
            enablePointerCapture()
        }

        // Hue slider
        sizeConstraints(height = 1.5.rem).canvas {
            val hueDelegate = object : CanvasDelegate() {
                private var isDragging = false

                private fun update(x: Double, width: Double) {
                    val h = (x / width).toFloat().coerceIn(0f, 1f)
                    okhsv.value = okhsv.state.raw.copy(hue = h)
                    invalidate()
                }

                override fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    isDragging = true; update(x, width); return true
                }
                override fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    if (isDragging) update(x, width); return true
                }
                override fun onPointerUp(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    isDragging = false; return true
                }

                override fun draw(context: DrawingContext2D) {
                    val w = context.width; val h = context.height
                    val stops = (0..12).map { i ->
                        val t = i / 12f
                        GradientStop(t, okhsvHueColor(t))
                    }
                    context.fillPaint = LinearGradient(
                        stops = stops,
                        x0 = 0.0, y0 = 0.0, x1 = w, y1 = 0.0,
                    )
                    context.fillRect(0.0, 0.0, w, h)

                    // Indicator line
                    val x = okhsv.state.raw.hue * w
                    context.strokePaint = Color.white
                    context.lineWidth = 2.0
                    context.beginPath()
                    context.moveTo(x, 0.0)
                    context.lineTo(x, h)
                    context.stroke()
                    context.strokePaint = Color.black
                    context.lineWidth = 1.0
                    context.beginPath()
                    context.moveTo(x - 1.0, 0.0)
                    context.lineTo(x - 1.0, h)
                    context.stroke()
                }

            }
            this.delegate = hueDelegate
            hueDelegate.invalidate = { this.delegate = hueDelegate }
            reactive { okhsv(); hueDelegate.invalidate() }
            enablePointerCapture()
        }

        // Color swatch + hex display
        row {
            sizeConstraints(width = 3.rem, height = 3.rem).frame {
                dynamicTheme {
                    val c = color()
                    ThemeDerivation {
                        it.copy(id = "swatch_${c.toInt()}", background = c).withBack
                    }
                }
            }
            expanding.centeredVertically.text {
                ::content { color().toAlphalessWeb() }
            }
        }
    }
}
