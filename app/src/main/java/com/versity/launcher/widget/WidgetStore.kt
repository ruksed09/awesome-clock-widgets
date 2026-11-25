package com.versity.launcher.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * Where widget looks are kept. Widget providers read this synchronously while the home screen waits,
 * so it is plain SharedPreferences rather than DataStore. Keys:
 * - "w_<appWidgetId>": a widget on the phone's home screen (any launcher)
 * - "h_<id>": a widget on Versity's own home screen
 * - "draft_<token>": a look waiting for the home screen to confirm a pinned widget
 */
class WidgetStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("widget_specs", Context.MODE_PRIVATE)

    fun system(appWidgetId: Int, type: WidgetType): WidgetSpec = WidgetSpec.decode(prefs.getString(systemKey(appWidgetId), null), type)

    fun hasSystem(appWidgetId: Int): Boolean = prefs.contains(systemKey(appWidgetId))

    fun saveSystem(appWidgetId: Int, spec: WidgetSpec) = prefs.edit().putString(systemKey(appWidgetId), spec.encode()).apply()

    fun removeSystem(appWidgetId: Int) = prefs.edit().remove(systemKey(appWidgetId)).apply()

    /** Type is stored alongside so a home widget can be drawn without knowing its provider. */
    fun home(id: Int): WidgetSpec? {
        val raw = prefs.getString(homeKey(id), null) ?: return null
        val type = prefs.getString(homeKey(id) + "_type", null)?.let { n -> WidgetType.entries.firstOrNull { it.name == n } } ?: return null
        return WidgetSpec.decode(raw, type)
    }

    fun saveHome(id: Int, spec: WidgetSpec) = prefs.edit()
        .putString(homeKey(id), spec.encode())
        .putString(homeKey(id) + "_type", spec.type.name)
        .apply()

    fun removeHome(id: Int) = prefs.edit().remove(homeKey(id)).remove(homeKey(id) + "_type").apply()

    /** Ids for new home widgets only ever go up, so a removed widget's look is never reused by accident. */
    fun nextHomeId(): Int {
        val next = prefs.getInt(NEXT_HOME_ID, 1)
        prefs.edit().putInt(NEXT_HOME_ID, next + 1).apply()
        return next
    }

    fun saveDraft(token: String, spec: WidgetSpec) = prefs.edit()
        .putString(draftKey(token), spec.encode())
        .putString(draftKey(token) + "_type", spec.type.name)
        .putLong(draftKey(token) + "_at", System.currentTimeMillis())
        .apply()

    fun hasDraft(token: String): Boolean = prefs.contains(draftKey(token))

    /** Takes the draft out of the store: a draft is used once. */
    fun takeDraft(token: String): WidgetSpec? {
        val raw = prefs.getString(draftKey(token), null) ?: return null
        val type = prefs.getString(draftKey(token) + "_type", null)?.let { n -> WidgetType.entries.firstOrNull { it.name == n } } ?: return null
        prefs.edit().remove(draftKey(token)).remove(draftKey(token) + "_type").remove(draftKey(token) + "_at").apply()
        return WidgetSpec.decode(raw, type)
    }

    /**
     * The newest draft of [type] made in the last half hour, if any. Some home screens open the
     * configuration screen for a pinned widget before confirming the pin, and when a home screen will
     * not pin at all the user adds the widget from its widget list instead; either way the studio
     * starts from the look the user just designed instead of the defaults.
     */
    fun recentDraft(type: WidgetType, withinMillis: Long = 30 * 60_000L): WidgetSpec? {
        val now = System.currentTimeMillis()
        return prefs.all.keys.asSequence()
            .filter { it.startsWith("draft_") && it.endsWith("_type") && prefs.getString(it, null) == type.name }
            .map { it.removeSuffix("_type") }
            .map { key -> key to prefs.getLong(key + "_at", 0L) }
            .filter { (_, at) -> now - at in 0..withinMillis }
            .maxByOrNull { (_, at) -> at }
            ?.let { (key, _) -> WidgetSpec.decode(prefs.getString(key, null), type) }
    }

    /** Removes every draft of [type]: its look has found its widget. */
    fun dropDrafts(type: WidgetType) {
        val keys = prefs.all.keys.filter { it.startsWith("draft_") && it.endsWith("_type") && prefs.getString(it, null) == type.name }
            .map { it.removeSuffix("_type") }
        if (keys.isEmpty()) return
        prefs.edit().apply { keys.forEach { remove(it).remove(it + "_type").remove(it + "_at") } }.apply()
    }

    private fun systemKey(id: Int) = "w_$id"
    private fun homeKey(id: Int) = "h_$id"
    private fun draftKey(token: String) = "draft_$token"

    private companion object {
        const val NEXT_HOME_ID = "next_home_id"
    }
}
