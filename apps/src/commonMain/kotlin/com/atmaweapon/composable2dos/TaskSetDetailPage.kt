package com.atmaweapon.composable2dos

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h2
import com.lightningkite.kiteui.views.direct.recyclerView
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.atmaweapon.composable2dos.sdk.currentSessionNotNull
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.remember
import com.lightningkite.serialization.lensPath
import com.lightningkite.services.database.Query
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import kotlin.collections.get
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
                            text { ::content { it().title } }

                        }
                    }
                )
            }


        }
    }
}