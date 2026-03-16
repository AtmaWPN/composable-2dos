package com.atmaweapon.composable2dos

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.icon
import com.atmaweapon.composable2dos.sdk.currentSession
import com.atmaweapon.composable2dos.sdk.currentSessionNotNull
import com.atmaweapon.composable2dos.sdk.sessionToken
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.mutableRemember
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.SortPart
import com.lightningkite.services.database.sort
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
                session.taskSets.query(Query(
                    condition = condition { it.user eq session.userId },
                    orderBy = sort { it.title.ascending() },
                ))()
            } ?: emptyList()
        }

        col {
            expanding.recyclerView {
                placer = RecyclerViewPlacerVerticalGrid(1)

                children(
                    items = taskSets,
                    id = { it._id },
                    render = {
                        card.row {
                            expanding.button {
                                text { ::content { it().title } }
                                ::action { Action(it().title) { pageNavigator.navigate(TaskSetDetailPage(it()._id)) } }
                            }
                            button {
                                icon(Icon.delete, "Delete task set")
                                onClick {
                                    confirmDanger("Delete task set?", "", "Delete") {
                                        currentSessionNotNull().taskSets[it()._id].delete()
                                    }
                                }
                            }
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
                ::action { Action("Create New Task Set") { dialog { close ->
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
                                    close()
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
                                    close()
                                } }
                            }
                        }
                    }
                } } }
            }
        }
    }
}
