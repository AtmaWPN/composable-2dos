package com.atmaweapon.composable2dos

import com.atmaweapon.composable2dos.extensions.enablePointerCapture
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.lens
import kotlin.math.*

// ─── OKHSV color math ────────────────────────────────────────────────────────
// Based on Björn Ottosson's OKHSV (https://bottosson.github.io/posts/colorpicker/)

//Copyright (c) 2021 Björn Ottosson
//
//Permission is hereby granted, free of charge, to any person obtaining a copy of
//this software and associated documentation files (the "Software"), to deal in
//the Software without restriction, including without limitation the rights to
//use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
//of the Software, and to permit persons to whom the Software is furnished to do
//so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

private object OKHSV {
    fun srgbTransferFunction(x: Float): Float
    {
        if (x >= 0.0031308f)
            return (1.055f) * x.pow(1.0f/2.4f) - 0.055f
        else
        return 12.92f * x
    }

    fun srgbTransferFunctionInverse(x: Float): Float
    {
        if (x >= 0.04045f)
            return ((x + 0.055f)/(1f + 0.055f)).pow(2.4f)
        else
        return x / 12.92f
    }

    fun linearSrgbToOklab(c: Triple<Float, Float, Float>): Triple<Float, Float, Float>
    {
        val l = 0.41222146f * c.first + 0.53633255f * c.second + 0.051445995f * c.third
        val m = 0.2119035f * c.first + 0.6806995f * c.second + 0.10739696f * c.third
        val s = 0.08830246f * c.first + 0.28171885f * c.second + 0.6299787f * c.third

        val l_ = cbrt(l)
        val m_ = cbrt(m)
        val s_ = cbrt(s)

        return Triple(
            0.21045426f * l_ + 0.7936178f * m_ - 0.004072047f * s_,
            1.9779985f * l_ - 2.4285922f * m_ + 0.4505937f * s_,
            0.025904037f * l_ + 0.78277177f * m_ - 0.80867577f * s_,
        )
    }

    fun oklabToLinearSrgb(c: Triple<Float, Float, Float>): Triple<Float, Float, Float>
    {
        val l_ = c.first + 0.39633778f * c.second + 0.21580376f * c.third
        val m_ = c.first - 0.105561346f * c.second - 0.06385417f * c.third
        val s_ = c.first - 0.08948418f * c.second - 1.2914855f * c.third

        val l = l_ * l_ * l_
        val m = m_ * m_ * m_
        val s = s_ * s_ * s_

        return Triple(
            +4.0767417f * l - 3.3077116f * m + 0.23096994f * s,
            -1.268438f * l + 2.6097574f * m - 0.34131938f * s,
            -0.0041960864f * l - 0.7034186f * m + 1.7076147f * s,
        )
    }

    fun computeMaxSaturation(a: Float, b: Float): Float {

        // Max saturation will be when one of r, g or b goes below zero.

        // Select different coefficients depending on which component goes below zero first
        var k0: Float
        var k1: Float
        var k2: Float
        var k3: Float
        var k4: Float
        var wl: Float
        var wm: Float
        var ws: Float

        if (-1.8817033f * a - 0.8093649f * b > 1)
        {
            // Red component
            k0 = 1.1908628f; k1 = 1.7657673f; k2 = 0.5966264f; k3 = 0.755152f; k4 = 0.5677124f
            wl = 4.0767417f; wm = -3.3077116f; ws = 0.23096994f
        }
        else if (1.8144411f * a - 1.1944528f * b > 1)
        {
            // Green component
            k0 = 0.73956513f; k1 = -0.45954403f; k2 = 0.08285427f; k3 = 0.12541070f; k4 = 0.14503203f
            wl = -1.268438f; wm = 2.6097574f; ws = -0.34131938f
        }
        else
        {
            // Blue component
            k0 = 1.3573365f; k1 = -0.00915799f; k2 = -1.15130210f; k3 = -0.50559604f; k4 = 0.00692167f
            wl = -0.0041960864f; wm = -0.7034186f; ws = 1.7076147f
        }

        // Approximate max saturation using a polynomial:
        var S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b

        // Do one step Halley's method to get closer
        // this gives an error less than 10e6, except for some blue hues where the dS/dh is close to infinite
        // this should be sufficient for most applications, otherwise do two/three steps

        val k_l = +0.39633778f * a + 0.21580376f * b
        val k_m = -0.105561346f * a - 0.06385417f * b
        val k_s = -0.08948418f * a - 1.2914855f * b

        val l_ = 1f + S * k_l
        val m_ = 1f + S * k_m
        val s_ = 1f + S * k_s

        val l = l_ * l_ * l_
        val m = m_ * m_ * m_
        val s = s_ * s_ * s_

        val l_dS = 3f * k_l * l_ * l_
        val m_dS = 3f * k_m * m_ * m_
        val s_dS = 3f * k_s * s_ * s_

        val l_dS2 = 6f * k_l * k_l * l_
        val m_dS2 = 6f * k_m * k_m * m_
        val s_dS2 = 6f * k_s * k_s * s_

        val f  = wl * l     + wm * m     + ws * s
        val f1 = wl * l_dS  + wm * m_dS  + ws * s_dS
        val f2 = wl * l_dS2 + wm * m_dS2 + ws * s_dS2

        S -= f * f1 / (f1*f1 - 0.5f * f * f2)

        return S
    }

    fun findCusp(a: Float, b: Float): Pair<Float, Float> {
        // First, find the maximum saturation (saturation S = C/L)
        val S_cusp = computeMaxSaturation(a, b)

        // Convert to linear sRGB to find the first point where at least one of r,g or b >= 1:
        val rgb_at_max: Triple<Float, Float, Float> = oklabToLinearSrgb(Triple(1f, S_cusp * a, S_cusp * b))
        val L_cusp = cbrt(1 / max(max(rgb_at_max.first, rgb_at_max.second), rgb_at_max.third))
        val C_cusp = L_cusp * S_cusp

        return L_cusp to C_cusp
    }

    fun toe(x: Float): Float {
        val k_1 = 0.206f
        val k_2 = 0.03f
        val k_3 = (1 + k_1) / (1 + k_2)
        return 0.5f * (k_3 * x - k_1 + sqrt((k_3 * x - k_1) * (k_3 * x - k_1) + 4 * k_2 * k_3 * x))
    }

    fun inverseToe(x: Float): Float {
        val k_1 = 0.206f
        val k_2 = 0.03f
        val k_3 = (1 + k_1) / (1 + k_2)
        return (x * x + k_1 * x) / (k_3 * (x + k_2))
    }

    fun toST(cusp: Pair<Float, Float>): Pair<Float, Float> {
        val L = cusp.first
        val C = cusp.second
        return C / L to C / (1 - L)
    }
}

data class OkhsvColor(
    val hue: Float,        // 0..1
    val saturation: Float, // 0..1
    val value: Float,      // 0..1
) {
    fun toRGB(): Color {
        val h = hue
        val s = saturation
        val v = value

        val a_ = cos(2 * PI.toFloat() * h)
        val b_ = sin(2 * PI.toFloat() * h)

        val cusp: Pair<Float, Float> = OKHSV.findCusp(a_, b_)
        val ST_max: Pair<Float, Float> = OKHSV.toST(cusp)
        val S_max = ST_max.first
        val T_max = ST_max.second
        val S_0 = 0.5f
        val k = 1 - S_0 / S_max

        // first we compute L and V as if the gamut is a perfect triangle:

        // L, C when v==1:
        val L_v = 1 - s * S_0 / (S_0 + T_max - T_max * k * s)
        val C_v = s * T_max * S_0 / (S_0 + T_max - T_max * k * s)

        var L = v * L_v
        var C = v * C_v

        // then we compensate for both toe and the curved top part of the triangle:
        val L_vt = OKHSV.inverseToe(L_v)
        val C_vt = C_v * L_vt / L_v

        val L_new = OKHSV.inverseToe(L)
        C = C * L_new / L
        L = L_new

        val rgb_scale: Triple<Float, Float, Float> = OKHSV.oklabToLinearSrgb(Triple(L_vt, a_ * C_vt, b_ * C_vt))
        val scale_L = cbrt(1 / max(max(rgb_scale.first, rgb_scale.second), max(rgb_scale.third, 0f)))

        L *= scale_L
        C *= scale_L

        val rgb: Triple<Float, Float, Float> = OKHSV.oklabToLinearSrgb(Triple(L, C * a_, C * b_))
        return Color(
            1f,
            OKHSV.srgbTransferFunction(rgb.first),
            OKHSV.srgbTransferFunction(rgb.second),
            OKHSV.srgbTransferFunction(rgb.third),
        )
    }

    companion object {
        fun fromRGB(color: Color): OkhsvColor {

            val lab: Triple<Float, Float, Float> = OKHSV.linearSrgbToOklab(Triple(
                OKHSV.srgbTransferFunctionInverse(color.red),
                OKHSV.srgbTransferFunctionInverse(color.green),
                OKHSV.srgbTransferFunctionInverse(color.blue),
            ))

            var C = sqrt(lab.second * lab.second + lab.third * lab.third)
            val a_ = lab.second / C
            val b_ = lab.third / C

            var L = lab.first
            val h = 0.5f + 0.5f * atan2(-lab.third, -lab.second) / PI.toFloat()

            val cusp: Pair<Float, Float> = OKHSV.findCusp(a_, b_)
            val ST_max: Pair<Float, Float> = OKHSV.toST(cusp)
            val S_max = ST_max.first
            val T_max = ST_max.second
            val S_0 = 0.5f
            val k = 1 - S_0 / S_max

            // first we find L_v, C_v, L_vt and C_vt

            val t = T_max / (C + L * T_max)
            val L_v = t * L
            val C_v = t * C

            val L_vt = OKHSV.inverseToe(L_v)
            val C_vt = C_v * L_vt / L_v

            // we can then use these to invert the step that compensates for the toe and the curved top part of the triangle:
            val rgb_scale: Triple<Float, Float, Float> = OKHSV.oklabToLinearSrgb(Triple(L_vt, a_ * C_vt, b_ * C_vt))
            val scale_L = cbrt(1 / max(max(rgb_scale.first, rgb_scale.second), max(rgb_scale.third, 0f)))

            L /= scale_L
            C /= scale_L

            C = C * OKHSV.toe(L) / L
            L = OKHSV.toe(L)

            // we can now compute v and s:

            val v = L / L_v
            val s = (S_0 + T_max) * C_v / ((T_max * S_0) + T_max * k * C_v)

            return OkhsvColor(h, s, v)
        }
    }
}

// ─── Color slider component ─────────────────────────────────────────────────

private fun ViewWriter.colorSlider(
    value: MutableReactive<Double>,
    gradientStops: Reactive<List<GradientStop>>,
) {
    row {
        sizeConstraints(height = 1.5.rem).expanding.canvas {
            var currentValue by Signal(0.0)
            var currentStops by Signal(listOf<GradientStop>())

            val delegate = object : CanvasDelegate() {
                private var isDragging = false

                private fun update(x: Double, width: Double) {
                    val newValue = (x / width).coerceIn(0.0, 1.0)
                    reactiveSuspending {
                        value.set(newValue)
                        invalidate()
                    }
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

                    // Draw gradient
                    context.fillPaint = LinearGradient(
                        stops = currentStops,
                        x0 = 0.0, y0 = 0.0, x1 = w, y1 = 0.0,
                    )
                    context.fillRect(0.0, 0.0, w, h)

                    // Indicator line
                    val indicatorX = currentValue * w
                    context.strokePaint = Color.white
                    context.lineWidth = 2.0
                    context.beginPath()
                    context.moveTo(indicatorX, 0.0)
                    context.lineTo(indicatorX, h)
                    context.stroke()
                    context.strokePaint = Color.black
                    context.lineWidth = 1.0
                    context.beginPath()
                    context.moveTo(indicatorX - 1.0, 0.0)
                    context.lineTo(indicatorX - 1.0, h)
                    context.stroke()
                }
            }
            this.delegate = delegate
            delegate.invalidate = { this.delegate = delegate }
            reactive {
                currentValue = value()
                currentStops = gradientStops()
                delegate.invalidate()
            }
            enablePointerCapture()
        }
        sizeConstraints(width = 4.rem).numberInput {
            keyboardHints = KeyboardHints.decimal
            content bind value.lens(get = { it }, set = { it ?: 0.0})
        }
    }
}

// ─── Color picker component ───────────────────────────────────────────────────

fun ViewWriter.okhsvColorPicker(color: MutableReactive<Color>) {
    col {
        val okhsv = MutableRemember { OkhsvColor.fromRGB(color()) }

        color bind okhsv.lens { it.toRGB() }.withWrite { OkhsvColor.fromRGB(it) }

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

        val gradientGrit = 8

        // Hue slider
        colorSlider(
            value = okhsv.lens(get = { it.hue.toDouble() }, set = { okhsv.state.raw.copy(hue = it.toFloat()) }),
            gradientStops = remember {
                (0..gradientGrit).map { i ->
                    val t = i / gradientGrit.toFloat()
                    GradientStop(t, okhsv().copy(hue = t).toRGB())
                }
            },
        )

        // Saturation slider
        colorSlider(
            value = okhsv.lens(get = { it.saturation.toDouble() }, set = { okhsv.state.raw.copy(saturation = it.toFloat()) }),
            gradientStops = remember {
                (0..gradientGrit).map { i ->
                    val t = i / gradientGrit.toFloat()
                    GradientStop(t, okhsv().copy(saturation = t).toRGB())
                }
            },
        )

        // Value slider
        colorSlider(
            value = okhsv.lens(get = { it.value.toDouble() }, set = { okhsv.state.raw.copy(value = it.toFloat()) }),
            gradientStops = remember {
                (0..gradientGrit).map { i ->
                    val t = i / gradientGrit.toFloat()
                    GradientStop(t, okhsv().copy(value = t).toRGB())
                }
            },
        )

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
