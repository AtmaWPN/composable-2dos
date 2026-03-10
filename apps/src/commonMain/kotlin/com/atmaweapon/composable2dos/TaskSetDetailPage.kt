package com.atmaweapon.composable2dos

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.atmaweapon.composable2dos.sdk.currentSessionNotNull
import com.atmaweapon.composable2dos.taskSet
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.WarningSemantic.invoke
import com.lightningkite.kiteui.models.flat2
import com.lightningkite.kiteui.models.systemDefaultFont
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.mutableRemember
import com.lightningkite.reactive.core.mutableRememberSuspending
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.rememberSuspending
import com.lightningkite.serialization.lensPath
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.modification
import com.lightningkite.services.database.neq
import com.lightningkite.services.database.notNull
import kotlin.time.Clock.System.now
import kotlin.uuid.Uuid


@Routable("/task-set/{id}")
class TaskSetDetailPage(val id: Uuid) : Page {
    val taskSet = remember {
        currentSessionNotNull().taskSets[id]()
            ?: throw Exception()
    }

    override val title: Reactive<String> get() = taskSet.lensPath { it.title }

    val tasks = remember {
        currentSessionNotNull().tasks.query(Query(condition { it.taskSet eq id }))()
    }

    override fun ViewWriter.render() {
        col {
            expanding.recyclerView {
                placer = RecyclerViewPlacerVerticalGrid(1)

                children(
                    items = tasks,
                    id = { it._id },
                    render = {
                        card.row {
                            val completed = mutableRemember {
                                it().completedAt != null
                            }

                            checkbox {
                                checked bind completed
                            }

                            reactiveSuspending {
                                val newCompleted = completed()
                                val task = it()
                                if ((task.completedAt != null) != newCompleted) {
                                    currentSessionNotNull().tasks[task._id].modify(
                                        if (newCompleted) modification { it.completedAt assign now() }
                                        else modification { it.completedAt assign null }
                                    )
                                }
                            }

                            shownWhen { completed() }.expanding.strikethrough.text {
                                ::content { it().title }
                            }
                            shownWhen { !completed() }.expanding.text {
                                ::content { it().title }
                            }
                        }
                    }
                )
            }

            important.button {
                centered.text("Create New Task")
                ::action {
                    Action("Create New Task") {
                        openPopover(PopoverPreferredDirection.aboveCenter) {
                            val taskName = mutableRemember { "" }
                            col {
                                textInput {
                                    content bind taskName
                                    hint = "Task title"
                                }
                                row {
                                    button {
                                        centered.text("Cancel")
                                        ::action { Action("Cancel") { closeThisPopover() } }
                                    }
                                    important.button {
                                        centered.text("Save")
                                        ::action {
                                            Action("Save") {
                                                val session = currentSessionNotNull()
                                                session.tasks.add(
                                                    Task(
                                                        title = taskName(),
                                                        user = session.userId,
                                                        taskSet = id,
                                                        createdAt = now()
                                                    )
                                                )
                                                closeThisPopover()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
