package com.atmaweapon.composable2dos

import com.atmaweapon.composable2dos.sdk.currentSession
import com.atmaweapon.composable2dos.sdk.currentSessionNotNull
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.forms.JsonRenderer.JsonSemantic.withBack
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.mutableRemember
import com.lightningkite.reactive.core.remember
import com.lightningkite.services.database.*
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
                    render = { taskSet ->
                        val previewTasks = remember {
                            currentSession()?.let { session ->
                                session.tasks.query(Query(
                                    condition = condition { (it.taskSet eq taskSet()._id) and (it.completedAt eq null) },
                                    orderBy = sort { it.createdAt.descending() },
                                    limit = 3,
                                ))()
                            } ?: emptyList()
                        }

                        expanding.frame {
                            paddingByEdge = Edges(1.rem, 0.rem, 1.rem, 0.rem)
                            gap = 1.rem
                            card.button {
                                dynamicTheme {
                                    val hexColor = taskSet().color ?: return@dynamicTheme null
                                    ThemeDerivation {
                                        val bg = Color.fromHexString(hexColor)
                                        it.withBack(background = bg, foreground = bg.highlight(1f))
                                    }
                                }
                                column {
                                    row {
                                        expanding.text { ::content { taskSet().title } }

                                    }
                                    column {
                                        shownWhen { previewTasks().isNotEmpty() }.expanding.text {
                                            paddingByEdge = Edges(2.rem, 0.rem, 0.rem, 0.rem)
                                            ::content { previewTasks().getOrNull(0)?.title ?: "No Data" }
                                        }
                                        shownWhen { previewTasks().size >= 2 }.expanding.text {
                                            paddingByEdge = Edges(2.rem, 0.rem, 0.rem, 0.rem)
                                            ::content { previewTasks().getOrNull(1)?.title ?: "No Data" }
                                        }
                                        shownWhen { previewTasks().size >= 3 }.expanding.text {
                                            paddingByEdge = Edges(2.rem, 0.rem, 0.rem, 0.rem)
                                            ::content { previewTasks().getOrNull(2)?.title ?: "No Data" }
                                        }
                                    }
                                }
                                ::action {
                                    Action(taskSet().title) {
                                        pageNavigator.navigate(
                                            TaskSetDetailPage(
                                                taskSet()._id
                                            )
                                        )
                                    }
                                }
                            }
                            atTopEnd.row {
                                button {
                                    icon(Icon.settings, "Configure task set")
                                    onClick {
                                        pageNavigator.navigate(TaskSetConfigPage(taskSet()._id))
                                    }
                                }
                                danger.button {
                                    icon(Icon.delete, "Delete task set")
                                    onClick {
                                        confirmDanger("Delete task set?", "", "Delete") {
                                            currentSessionNotNull().taskSets[taskSet()._id].delete()
                                        }
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
                    val saveAction = Action("Save") {
                        currentSession()?.let { session ->
                            session.taskSets.add(TaskSet(
                                title = taskSetName(),
                                user = session.userId,
                                createdAt = now()
                            ))
                        }
                        close()
                    }
                    sizeConstraints(minWidth = 20.rem).col {
                        textInput {
                            content bind taskSetName
                            hint = "Title"
                            requestFocus()
                            action = saveAction
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
                                action = saveAction
                            }
                        }
                    }
                } } }
            }
        }
    }
}
