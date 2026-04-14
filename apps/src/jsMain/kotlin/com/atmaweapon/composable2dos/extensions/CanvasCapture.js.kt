package com.atmaweapon.composable2dos.extensions

import com.lightningkite.kiteui.views.direct.Canvas

actual fun Canvas.enablePointerCapture() {
    native.onElement { element ->
        element.addEventListener(
            "pointerdown",
            callback = { event ->
                element.asDynamic().setPointerCapture(event.asDynamic().pointerId)
            }
        )
    }
}
