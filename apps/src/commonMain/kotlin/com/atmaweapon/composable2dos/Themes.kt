package com.atmaweapon.composable2dos

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.reactive.core.Signal
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
        background = Color.fromHexString("#BBBBBB"),
        foreground = Color.fromHexString("#333333"),
        semanticOverrides = SemanticOverrides(
            // Danger - red styling
            DangerSemantic.override {
                it.withBack(
                    foreground = Color.fromHexString("#BBBBBB"),
                    background = Color.fromHexString("#550505"),
                )
            },
            HeaderSemantic.override {
                it.withoutBack(padding = Edges(1.rem))
            },
            NavSemantic.override {
                it.withBack(
                    foreground = Color.fromHexString("#BBBBBB"),
                    background = Color.fromHexString("#333333"),
                    cornerRadii = CornerRadii.AdaptiveToSpacing(0.px),
//                    cascading = false,
//                    padding = Edges(0.px),
                    gap = 0.px
                )
            },
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
}

@Serializable
enum class ThemePreferenceOption(val make: (ThemePreference) -> Theme) {
    AtmaWeapon(make = { edgyTheme() }),
    Flat2(make = { Theme.flat2("flat2", 0.6.turns) }),
    ShadLight(make = { Theme.shadCnLike("shad-light", Color.gray(0.95f)) }),
    ShadDark(make = { Theme.shadCnLike("shad-dark", Color.gray(0.05f)) }),
    Clean(make = { Theme.clean(Color.fromHexString("#27b392")) }),

}

val appThemePreference = PersistentProperty("theme", ThemePreference())
val appTheme = remember { appThemePreference().make() }
