package com.atmaweapon.composable2dos

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.reactive.core.remember
import kotlinx.serialization.Serializable

fun edgyTheme(): Theme {
    return Theme(
        id = "base",
        elevation = 0.dp,
        gap = 0.5.rem,
        padding = Edges(0.5.rem),
        outlineWidth = 0.px,
        cornerRadii = CornerRadii.Fixed(0.rem),
        background = Color.fromHexString("#b9b9b9"),
        foreground = Color.fromHexString("#3a3a3a"),
        semanticOverrides = SemanticOverrides(
            // Danger - red styling
            DangerSemantic.override {
                it.withBack(
                background = Color.fromHexString("#E53935"),
                    foreground = Color.white,
                )
            },
            HeaderSemantic.override {
                it.withoutBack(padding = Edges(1.rem))
            },
            NavSemantic.override {
                it.withBack(
                    foreground = it.background,
                    background = it.foreground,
                    cornerRadii = CornerRadii.AdaptiveToSpacing(0.px),
//                    cascading = false,
//                    padding = Edges(0.px),
                    gap = 0.px
                )
            },
            CardSemantic.override {
                it.withBack(
                    background = it.background.darken(0.1f),
                    cascading = true,
                )
            }
        ),
    )
}

fun happyTheme(): Theme {
    return Theme(
        id = "happy",
        elevation = 0.dp,
        gap = 0.5.rem,
        padding = Edges(0.5.rem),
        outlineWidth = 0.px,
        cornerRadii = CornerRadii.Fixed(0.rem),
        background = Color.fromHexString("#a5c7a2"),
        foreground = Color.fromHexString("#0b3009"),
        semanticOverrides = SemanticOverrides(
            // Danger - red styling
            DangerSemantic.override {
                it.withBack(
                    background = Color.fromHexString("#E53935"),
                    foreground = Color.white,
                )
            },
            HeaderSemantic.override {
                it.withoutBack(padding = Edges(1.rem))
            },
            NavSemantic.override {
                it.withBack(
                    background = it.foreground,
                    foreground = it.background,
                    cornerRadii = CornerRadii.AdaptiveToSpacing(0.px),
//                    cascading = false,
//                    padding = Edges(0.px),
                    gap = 0.px
                )
            },
            CardSemantic.override {
                it.withBack(
                    background = it.background,
                    outline = it.foreground,
                    cascading = true,
                )
            }
        ),
    )
}

@Serializable
data class ThemePreference(
    val option: ThemePreferenceOption = ThemePreferenceOption.AtmaWeapon,
    val font: ThemePreferenceFont = ThemePreferenceFont.System,
) {
    fun make() = option.make(this).copy(
        id = font.name, font = font.font()
    )
}

@Serializable
enum class ThemePreferenceFont(val font: () -> FontAndStyle) {
    System(font = {
        FontAndStyle(
            systemDefaultFixedWidthFont,
            allCaps = true,
        )
    }),
//    Fancy(font = {
//        FontAndStyle(
//            font = ,
//            allCaps = true,
//        )
//    }),
}

@Serializable
enum class ThemePreferenceOption(val make: (ThemePreference) -> Theme) {
    AtmaWeapon(make = { edgyTheme() }),
    Happy(make = { happyTheme() }),
    Flat2(make = { Theme.flat2("flat2", 0.6.turns) }),
    ShadLight(make = { Theme.shadCnLike("shad-light", Color.gray(0.95f)) }),
    ShadDark(make = { Theme.shadCnLike("shad-dark", Color.gray(0.05f)) }),
    Clean(make = { Theme.clean(Color.fromHexString("#27b392")) }),

}

val appThemePreference = PersistentProperty("theme", ThemePreference())
val appTheme = remember { appThemePreference().make() }
