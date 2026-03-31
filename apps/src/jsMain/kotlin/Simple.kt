package com.atmaweapon.composable2dos

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.root

fun main() {
    root(appTheme) {
        app(PageNavigator { AutoRoutes }, PageNavigator { AutoRoutes })
    }
}
