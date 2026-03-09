package com.lightningkite.lskiteuistarter

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.lskiteuistarter.sdk.currentSession
import com.lightningkite.lskiteuistarter.sdk.sessionToken
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.mutableRemember
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import kotlin.collections.emptyList
import kotlin.time.Clock.System.now

@Routable("/dashboard")
class HomePage : Page {
    override val title: Reactive<String> get() = Constant("Dashboard")
    override fun ViewWriter.render() {

        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        val taskSets = remember {
            currentSession()?.let { session ->
                session.taskSets.query(Query(condition { it.user eq session.userId }))()
            } ?: emptyList()
        }

        col {
            expanding.recyclerView {
                placer = RecyclerViewPlacerVerticalGrid(1)

                children(
                    items = taskSets,
                    id = { it._id },
                    render = {
                        card.button {
                            text { ::content { it().title } }
                            ::action { Action(it().title) { pageNavigator.navigate(TaskSetDetailPage(it()._id)) } }
                        }
                    }
                )
            }

//            important.buttonTheme.button {
//                centered.text("Test Notifications")
//                ::enabled { fcmToken() != null }
//                onClick {
//                    currentSession()?.api?.fcmToken?.testInAppNotifications(fcmToken()!!)
//                }
//            }

            important.button {
                centered.text("Create New Task Set")
                ::action { Action("Create New Task Set") { openPopover(PopoverPreferredDirection.aboveCenter) {
                    val taskSetName = mutableRemember { "" }
                    col {
                        textInput {
                            content bind taskSetName
                            hint = "Title"
                        }
                        row {
                            button {
                                centered.text("Cancel")
                                ::action { Action("Cancel") {
                                    closeThisPopover()
                                } }
                            }
                            important.button {
                                centered.text("Save")
                                ::action { Action("Save") {
                                    currentSession()?.let { session ->
                                        session.taskSets.add(TaskSet(
                                            title = taskSetName(),
                                            user = session.userId,
                                            createdAt = now()
                                        ))
                                    }
                                } }
                            }
                        }
                    }
                } } }
            }

            important.buttonTheme.button {
                centered.text("Logout")
                onClick {
                    try {
                        currentSession()?.api?.userAuth?.terminateSession()
                    } catch (e: Exception) {

                    } finally {
                        sessionToken set null
                        pageNavigator.reset(LoginPage())
                    }
                }
            }
        }
    }
}
