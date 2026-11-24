package com.versity.launcher.ui

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/** Test tags and semantics shared by UI code and Compose tests. */
object TestTags {
    const val CLOCK_STACKED = "clock_stacked"
    const val CLOCK_SINGLE_LINE = "clock_single_line"
    const val CLOCK_ANALOGUE = "clock_analogue"
    const val CLOCK_ORNAMENT = "clock_ornament"
    fun ornament(name: String) = "ornament_$name"
    const val ORNAMENT_DATE = "ornament_date"
    const val WIDGET_ANALOGUE = "widget_analogue"
    const val BATTERY_BAR = "battery_bar"
    const val DATE_RING = "date_ring"
    const val DOCK_APPS = "dock_apps"
    const val DOCK_PHONE = "dock_phone"
    const val OPEN_CUSTOMISE = "open_customise"
    const val DEFAULT_BANNER = "default_banner"
    const val DEFAULT_BANNER_SET = "default_banner_set"
    const val DEFAULT_BANNER_DISMISS = "default_banner_dismiss"
    fun favourite(key: String) = "favourite_$key"
    const val LOCK_SCREEN = "lock_screen"
    const val LOCK_HANDLE = "lock_handle"
    fun theme(id: String) = "theme_$id"
    const val LOCK_TOGGLE = "lock_toggle"
    fun backdrop(name: String) = "backdrop_$name"
    const val PREVIEW_LOCK = "preview_lock"
    const val SET_LOCK_WALLPAPER = "set_lock_wallpaper"
    const val LOCK_WALLPAPER_CONFIRM = "lock_wallpaper_confirm"
    const val LOCK_WALLPAPER_CANCEL = "lock_wallpaper_cancel"
    const val DIM_OVERLAY = "dim_overlay"
    const val PREVIEW = "preview"
    const val RESET = "reset"
    fun palette(id: String) = "palette_$id"
    fun font(id: String) = "font_$id"
    fun clockStyle(name: String) = "clock_style_$name"
    fun accent(argb: Long?) = "accent_${argb?.toString(16) ?: "auto"}"
    const val PICK_WALLPAPER = "pick_wallpaper"
    const val CLEAR_WALLPAPER = "clear_wallpaper"
    const val DIM_SLIDER = "dim_slider"
    fun tab(name: String) = "tab_$name"
    fun category(name: String) = "category_$name"
    const val APPLY_SHEET = "apply_sheet"
    fun applyTarget(name: String) = "apply_target_$name"
    const val OPT_THEME_BACKGROUND = "opt_theme_background"
    const val OPT_PHONE_WALLPAPER = "opt_phone_wallpaper"
    const val OPT_LOCK_SCREEN = "opt_lock_screen"
    const val OPT_LOCK_WALLPAPER = "opt_lock_wallpaper"
    const val APPLY_CANCEL = "apply_cancel"
    const val APPLY_CONFIRM = "apply_confirm"
    const val BROWSE_THEMES = "browse_themes"
    const val LOCK_FOLLOW_HOME = "lock_follow_home"
    fun drawerStyle(name: String) = "drawer_style_$name"
    fun iconStyle(name: String) = "icon_style_$name"
    fun addWidget(name: String) = "add_widget_$name"
}

val DimAlphaKey = SemanticsPropertyKey<Float>("DimAlpha")
var SemanticsPropertyReceiver.dimAlpha by DimAlphaKey

val PreviewPaletteKey = SemanticsPropertyKey<String>("PreviewPalette")
var SemanticsPropertyReceiver.previewPalette by PreviewPaletteKey
