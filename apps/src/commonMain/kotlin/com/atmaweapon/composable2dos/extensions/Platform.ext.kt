package com.atmaweapon.composable2dos.extensions

import com.lightningkite.kiteui.Platform
import com.atmaweapon.composable2dos.AppPlatform


fun Platform.toAppPlatform(): AppPlatform = when (this) {
    Platform.iOS -> AppPlatform.iOS
    Platform.Android -> AppPlatform.Android
    Platform.Web -> AppPlatform.Web
    Platform.Desktop -> AppPlatform.Desktop
}

