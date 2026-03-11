package com.atmaweapon.composable2dos

import com.atmaweapon.composable2dos.sdk.currentSession
import com.atmaweapon.composable2dos.sdk.sessionToken
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.reactive.extensions.equalTo

@Routable("/settings")
class SettingsPage : Page {
    override val title: Reactive<String> get() = Constant("Settings")
    override fun ViewWriter.render() {
        col {
            val user = rememberSuspending { currentSession()?.api?.userAuth?.getSelf() }
            card.col {
                text { ::content { user()?.name ?: "" } }
                text { ::content { user()?.email?.toString() ?: "" } }
            }
            card.col {
                h6("Theme")
                scrollingHorizontally.row {
                    ThemePreferenceOption.entries.forEach { entry ->
                        radioToggleButton {
                            text(entry.name)
                            checked bind (appThemePreference.lens(
                                get = { it.option },
                                modify = { old, it -> old.copy(option = it) }).equalTo(entry))
                        }
                    }
                }
                scrollingHorizontally.row {
                    ThemePreferenceFont.entries.forEach { entry ->
                        radioToggleButton {
                            text(entry.name)
                            checked bind (appThemePreference.lens(
                                get = { it.font },
                                modify = { old, it -> old.copy(font = it) }).equalTo(entry))
                        }
                    }
                }
            }
            danger.button {
                centered.text("Logout")
                onClick {
                    try {
                        currentSession()?.api?.userAuth?.terminateSession()
                    } catch (_: Exception) {
                    } finally {
                        sessionToken set null
                        pageNavigator.reset(LoginPage())
                    }
                }
            }
        }
    }
}
