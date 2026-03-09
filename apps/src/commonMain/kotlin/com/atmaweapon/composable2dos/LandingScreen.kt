package com.atmaweapon.composable2dos

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.activityIndicator
import com.atmaweapon.composable2dos.sdk.currentSession
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("/")
class LandingPage : Page, UseFullPage {
    override val title: Reactive<String> get() = Constant("Home")
    override fun ViewWriter.render() {
        launch {
            delay(1)
            if (currentSession.await() != null) {
                println("Haves Session")
                pageNavigator.reset(HomePage())
            } else {
                println("NO Session")
                pageNavigator.reset(LoginPage())
            }
        }
        centered.activityIndicator()
    }
}