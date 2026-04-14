package com.atmaweapon.composable2dos

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
        val l = 0.4122214708f * c.first + 0.5363325363f * c.second + 0.0514459929f * c.third
        val m = 0.2119034982f * c.first + 0.6806995451f * c.second + 0.1073969566f * c.third
        val s = 0.0883024619f * c.first + 0.2817188376f * c.second + 0.6299787005f * c.third

        val l_ = cbrt(l);
        val m_ = cbrt(m);
        val s_ = cbrt(s);

        return Triple(
            0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_,
            1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
            0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_,
        )
    }

    fun oklabToLinearSrgb(c: Triple<Float, Float, Float>): Triple<Float, Float, Float>
    {
        val l_ = c.first + 0.3963377774f * c.second + 0.2158037573f * c.third
        val m_ = c.first - 0.1055613458f * c.second - 0.0638541728f * c.third
        val s_ = c.first - 0.0894841775f * c.second - 1.2914855480f * c.third

        val l = l_ * l_ * l_;
        val m = m_ * m_ * m_;
        val s = s_ * s_ * s_;

        return Triple(
            +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
            -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
            -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
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

        if (-1.88170328f * a - 0.80936493f * b > 1)
        {
            // Red component
            k0 = +1.19086277f; k1 = +1.76576728f; k2 = +0.59662641f; k3 = +0.75515197f; k4 = +0.56771245f;
            wl = +4.0767416621f; wm = -3.3077115913f; ws = +0.2309699292f;
        }
        else if (1.81444104f * a - 1.19445276f * b > 1)
        {
            // Green component
            k0 = +0.73956515f; k1 = -0.45954404f; k2 = +0.08285427f; k3 = +0.12541070f; k4 = +0.14503204f;
            wl = -1.2684380046f; wm = +2.6097574011f; ws = -0.3413193965f;
        }
        else
        {
            // Blue component
            k0 = +1.35733652f; k1 = -0.00915799f; k2 = -1.15130210f; k3 = -0.50559606f; k4 = +0.00692167f;
            wl = -0.0041960863f; wm = -0.7034186147f; ws = +1.7076147010f;
        }

        // Approximate max saturation using a polynomial:
        var S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b;

        // Do one step Halley's method to get closer
        // this gives an error less than 10e6, except for some blue hues where the dS/dh is close to infinite
        // this should be sufficient for most applications, otherwise do two/three steps

        val k_l = +0.3963377774f * a + 0.2158037573f * b;
        val k_m = -0.1055613458f * a - 0.0638541728f * b;
        val k_s = -0.0894841775f * a - 1.2914855480f * b;

        val l_ = 1f + S * k_l;
        val m_ = 1f + S * k_m;
        val s_ = 1f + S * k_s;

        val l = l_ * l_ * l_;
        val m = m_ * m_ * m_;
        val s = s_ * s_ * s_;

        val l_dS = 3f * k_l * l_ * l_;
        val m_dS = 3f * k_m * m_ * m_;
        val s_dS = 3f * k_s * s_ * s_;

        val l_dS2 = 6f * k_l * k_l * l_;
        val m_dS2 = 6f * k_m * k_m * m_;
        val s_dS2 = 6f * k_s * k_s * s_;

        val f  = wl * l     + wm * m     + ws * s;
        val f1 = wl * l_dS  + wm * m_dS  + ws * s_dS;
        val f2 = wl * l_dS2 + wm * m_dS2 + ws * s_dS2;

        S = S - f * f1 / (f1*f1 - 0.5f * f * f2);

        return S;
    }

    fun findCusp(a: Float, b: Float): Pair<Float, Float> {
        // First, find the maximum saturation (saturation S = C/L)
        val S_cusp = computeMaxSaturation(a, b);

        // Convert to linear sRGB to find the first point where at least one of r,g or b >= 1:
        val rgb_at_max: Triple<Float, Float, Float> = oklabToLinearSrgb(Triple(1f, S_cusp * a, S_cusp * b));
        val L_cusp = cbrt(1 / max(max(rgb_at_max.first, rgb_at_max.second), rgb_at_max.third));
        val C_cusp = L_cusp * S_cusp;

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

        L = L * scale_L
        C = C * scale_L

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

            L = L / scale_L
            C = C / scale_L

            C = C * OKHSV.toe(L) / L
            L = OKHSV.toe(L)

            // we can now compute v and s:

            val v = L / L_v
            val s = (S_0 + T_max) * C_v / ((T_max * S_0) + T_max * k * C_v)

            return OkhsvColor(h, s, v)
        }
    }
}

// ─── Color picker component ───────────────────────────────────────────────────

fun ViewWriter.okhsvColorPicker(color: MutableReactive<Color>) {
    col {
        val debounced = color.debounce(200)
        val okhsv = remember { OkhsvColor.fromRGB(debounced()) }

        // Sync: internal okhsv → external color
        reactiveSuspending {
            color.set(okhsv().toRGB())
        }

        // 2D saturation/value panel
        sizeConstraints(minWidth = 16.rem, minHeight = 12.rem).canvas {
            val delegate = object : CanvasDelegate() {
                private fun update(x: Double, y: Double, w: Double, h: Double) {
                    val s = (x / w).toFloat().coerceIn(0f, 1f)
                    val v = (1.0 - y / h).toFloat().coerceIn(0f, 1f)
                    suspend { color.set(okhsv.state.raw.copy(saturation = s, value = v).toRGB()) }
                    invalidate()
                }

                override fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    update(x, y, width, height)
                    return true
                }
                override fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    update(x, y, width, height)
                    return true
                }

                override fun draw(context: DrawingContext2D) {
                    val w = context.width
                    val h = context.height
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
                            angle = Angle.zero,
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

                override fun sizeThatFitsHeight(width: Double, height: Double): Double = 192.0
            }
            delegate.invalidate = { this.delegate = delegate }
        }

        // Hue slider
        sizeConstraints(minHeight = 1.5.rem).canvas {
            val hueDelegate = object : CanvasDelegate() {
                private fun update(x: Double, width: Double) {
                    val h = (x / width).toFloat().coerceIn(0f, 1f)
                    suspend { color.set(okhsv.state.raw.copy(hue = h).toRGB()) }
                    invalidate()
                }

                override fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    update(x, width)
                    return true
                }
                override fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                    update(x, width)
                    return true
                }

                override fun draw(context: DrawingContext2D) {
                    val w = context.width
                    val h = context.height
                    // Hue gradient — 13 stops across full hue spectrum
                    val stops = (0..12).map { i ->
                        val t = i / 12f
                        GradientStop(t, OkhsvColor(t, 1f, 1f).toRGB())
                    }
                    context.fillPaint = LinearGradient(
                        stops = stops, angle = Angle.zero,
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

                override fun sizeThatFitsHeight(width: Double, height: Double): Double = 24.0
            }
            hueDelegate.invalidate = { this.delegate = hueDelegate }
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
