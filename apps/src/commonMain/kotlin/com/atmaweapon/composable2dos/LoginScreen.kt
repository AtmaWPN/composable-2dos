package com.atmaweapon.composable2dos

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.auth.AuthComponent
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.lightningserver.auth.AuthEndpoints
import com.atmaweapon.composable2dos.sdk.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*

@Routable("/login")
class LoginPage : Page, UseFullPage {
    override val title: Reactive<String> get() = Constant("Home")

    companion object {
        const val SECRET_FOR_API_SELECTOR = "i am a dev"
    }

    val backendSelectorEnabled = PersistentProperty("backendSelectorEnabled", false)

    override fun ViewWriter.render() {

        val authUI = remember {
            val api = selectedApi().api
            AuthComponent(
                endpoints = AuthEndpoints(
                    subjects = mapOf("User" to api.userAuth),
                    emailProof = api.userAuth.email,
                    oneTimePasswordProof = api.userAuth.totp,
                    backupCodeProof = api.userAuth.backupCode,
                    passwordProof = api.userAuth.password,
                ),
                subjectType = "User",
                subject = api.userAuth,
                onAuthentication = { token ->
                    sessionToken set token
                    pageNavigator.reset(HomePage())
                }
            )
        }

        frame {
            reactive {
                if (authUI().rawPrimaryInput() == SECRET_FOR_API_SELECTOR) backendSelectorEnabled.value = true
            }

            centered.sizedBox(SizeConstraints(maxWidth = 40.rem)).scrolling.col {
                centered.h4("Composable 2DOs")
                centered.text("This is a project I'm working on for my own benefit.")
                centered.text("If you'd like to try it out and you know me personally, reach out and I can get you an account.")

                shownWhen { backendSelectorEnabled() }.field("Server") {
                    select {
                        bind(selectedApi, ApiOption.entries.toList().let(::Constant)) { it.apiName }
                    }
                }

                frame {
                    reactive {
                        clearChildren()
                        authUI().render(this@frame)
                    }
                }
            }
        }

    }
}