package com.atmaweapon.composable2dos.utils


expect fun fcmSetup(): Unit

expect suspend fun requestNotificationPermissions(): Unit

expect suspend fun notificationPermissions(): Boolean?
