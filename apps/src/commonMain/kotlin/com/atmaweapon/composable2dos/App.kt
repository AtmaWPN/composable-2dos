package com.atmaweapon.composable2dos

import com.atmaweapon.composable2dos.extensions.toAppPlatform
import com.atmaweapon.composable2dos.sdk.*
import com.atmaweapon.composable2dos.utils.*
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.ExceptionToMessages
import com.lightningkite.kiteui.exceptions.installLsError
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.confirmDanger
import com.lightningkite.kiteui.views.l2.appNav
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.*
import kotlinx.coroutines.launch


fun baseTheme(): Theme {
    return Theme(
        id = "base",
        font = FontAndStyle(
            systemDefaultFixedWidthFont,
            allCaps = true,
        ),
        elevation = 0.dp,
        cornerRadii = CornerRadii.Fixed(0.rem),
        background = Color.fromHexString("#BBBBBB"),
        foreground = Color.fromHexString("#333333"),
        semanticOverrides = SemanticOverrides(
            // Danger - red styling
            DangerSemantic.override {
                it.withBack(
                    foreground = Color.fromHexString("#BBBBBB"),
                    background = Color.fromHexString("#550505"),
                )
            },
        ),
    )
}

//val defaultTheme = Theme.flat2("default", Angle(0.33f))
val defaultTheme = baseTheme()
val appTheme = Signal(defaultTheme)

// Notification Items
val fcmToken: Signal<String?> = Signal(null)
val setFcmToken =
    { token: String -> fcmToken.value = token } //This is for iOS. It is used in the iOS app. Do not remove.

var appUpdateChecked = false

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator) {
    ExceptionToMessages.root.installLsError()
    ExceptionToMessages.root.installLoggedOutErrors()

    AppScope.reactiveSuspending {
        if (currentSession() == null) return@reactiveSuspending
        val permission = notificationPermissions()
        when (permission) {
            false -> {}

            true -> {
                fcmSetup()
            }

            null -> {
                confirmDanger(
                    "Send notifications?",
                    "Composable 2DOs would like to send you notifications.",
                    "Allow"
                ) {
                    requestNotificationPermissions()
                }
            }
        }
    }


    if (Platform.current != Platform.Web && !appUpdateChecked) {
        appUpdateChecked = true
        AppScope.launch {
            val currentBuild = Build.version
            val releases = try {
                selectedApi.await().api.appRelease.query(
                    Query(
                        condition { it.platform.eq(Platform.current.toAppPlatform()) }
                    ))
            } catch (_: Exception) {
                return@launch
            }

            val currentRelease = releases.find { it.version == currentBuild } ?: return@launch
            val latestRelease = releases.maxByOrNull { it.releaseDate } ?: return@launch
            if (latestRelease._id != currentRelease._id) {
                dialogPageNavigator.navigate(
                    UpdateDialog(
                        newVersion = latestRelease.version,
                        forceUpdate = releases.any { it.requiredUpdate && it.releaseDate > currentRelease.releaseDate }
                    )
                )
            }
        }
    }

    navigator.navigate(LandingPage())
    return appNav(navigator, dialog) {
        appName = "KiteUI Sample App"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
//                NavLink(title = { "Internal" }, icon = { Icon.home }) { { RootPage } },
//                NavLink(title = { "Documentation" }, icon = { Icon.list }) { { DocSearchPage } },
            )
        }

        ::exists {
            navigator.currentPage() !is UseFullPage
        }
    }
}

interface UseFullPage


