package com.atmaweapon.composable2dos

import com.atmaweapon.composable2dos.extensions.toAppPlatform
import com.atmaweapon.composable2dos.sdk.currentSession
import com.atmaweapon.composable2dos.sdk.installLoggedOutErrors
import com.atmaweapon.composable2dos.sdk.selectedApi
import com.atmaweapon.composable2dos.utils.fcmSetup
import com.atmaweapon.composable2dos.utils.notificationPermissions
import com.atmaweapon.composable2dos.utils.requestNotificationPermissions
import com.lightningkite.kiteui.Build
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.exceptions.ExceptionToMessages
import com.lightningkite.kiteui.exceptions.installLsError
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Icon.Companion
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.probablyAppleUser
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Signal
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import kotlinx.coroutines.launch


// Notification Items
val fcmToken: Signal<String?> = Signal(null)
val setFcmToken =
    { token: String -> fcmToken.value = token } //This is for iOS. It is used in the iOS app. Do not remove.

var appUpdateChecked = false

fun ViewWriter.appNavBottomTabsIconOnly(setup: AppNav.() -> Unit): Unit {
    val appNav = AppNav.ByProperty()
    OuterSemantic.onNext.col {
        debugName = "outer nav"
        // Nav 3 top and bottom (top)
        bar.row {
            applySafeInsets(bottom = false)
            debugName = "normal app bar"
            showOnPrint = false
            setup(appNav)
            if (Platform.current != Platform.Web) button {
                icon(Icon.arrowBack, "Go Back")
                ::visible { pageNavigator.canGoBack() }
                onClick { pageNavigator.goBack() }
            }
            HeaderSemantic.onNext.centered.expanding.text {
                ::content.invoke { pageNavigator.currentPage()?.title?.let { it() } ?: "" }
                wraps = false
                ellipsis = true
            }
            navGroupActions(appNav.actionsProperty)
            ::shown { appNav.existsProperty() }
        }
        beforeNextElementSetup {
            applySafeInsets(top = false, bottom = false)
        }.expanding.navigatorView(pageNavigator)
        //Nav 3 - top and bottom (bottom/tabs)
        nav.navGroupTabs(appNav.navItemsProperty) {
            padding = 0.rem
            applySafeInsets(top = false)
            debugName = "navGroupTabs"
            showOnPrint = false
            ::shown { appNav.existsProperty() && !AppState.softInputOpen() }
        }
    }
}

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

    appNavFactory.value = ViewWriter::appNavBottomTabsIconOnly
    navigator.navigate(LandingPage())
    return appNav(navigator, dialog) {
        appName = "Composable 2DOs"
        ::navItems {
            listOf(
                NavLink(title = { "Home" }, icon = { Icon.home }) { { HomePage() } },
                NavLink(title = { "Settings" }, icon = { Icon.settings }) { { SettingsPage() } },
            )
        }

        ::exists {
            navigator.currentPage() !is UseFullPage
        }
    }
}

interface UseFullPage


