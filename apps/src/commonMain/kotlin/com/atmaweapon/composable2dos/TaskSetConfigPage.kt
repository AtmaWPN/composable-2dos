package com.atmaweapon.composable2dos

import com.atmaweapon.composable2dos.sdk.currentSession
import com.atmaweapon.composable2dos.sdk.currentSessionNotNull
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Draft
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.extensions.withWrite
import com.lightningkite.services.database.modification
import kotlin.uuid.Uuid

@Routable("/task-set/{id}/config")
class TaskSetConfigPage(val id: Uuid) : Page {
    val taskSet = remember {
        currentSessionNotNull().taskSets[id]()
            ?: throw Exception()
    }

    override val title: Reactive<String> get() = Constant("Task Set Settings")

    override fun ViewWriter.render() {
        reactive {
            if (currentSession() == null)
                pageNavigator.reset(LandingPage())
        }

        col {
            card.col {
                h6("Color")

                val defaultColor = OkhsvColor(0.6f, 0.8f, 0.85f).toRGB()
                val savedColor = remember {
                    taskSet().color?.let { Color.fromHexString(it) } ?: defaultColor
                }.withWrite { newColor ->
                    currentSessionNotNull().taskSets[id].modify(modification { it.color assign newColor.toAlphalessWeb() })
                }
                val colorDraft = Draft(savedColor)

                okhsvColorPicker(colorDraft)

                important.button {
                    centered.text("Save color")
                    onClick { colorDraft.publish() }
                }

                danger.button {
                    centered.text("Remove color")
                    ::enabled { taskSet().color != null }
                    onClick {
                        currentSessionNotNull().taskSets[id].modify(
                            modification { it.color assign null }
                        )
                        pageNavigator.goBack()
                    }
                }
            }
        }
    }
}
