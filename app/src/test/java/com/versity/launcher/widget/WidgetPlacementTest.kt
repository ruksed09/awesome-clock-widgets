package com.versity.launcher.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.versity.launcher.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Which widgets count as on the home screen, and how a widget sent from the app gets its look. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class WidgetPlacementTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = AppWidgetManager.getInstance(context)
    private val store = WidgetStore(context)

    private fun bind(): Int = shadowOf(manager).createWidget(AnalogueClockWidget::class.java, R.layout.widget_analog_classic_dark)

    private fun shown(id: Int) = manager.updateAppWidgetOptions(id, Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 120)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 120)
    })

    /** A widget the setup screen can read its family from, as a home screen would have bound it. */
    private fun bindWithInfo(id: Int): Int {
        val info = android.appwidget.AppWidgetProviderInfo().apply {
            provider = android.content.ComponentName(context, AnalogueClockWidget::class.java)
        }
        shadowOf(manager).addBoundWidget(id, info)
        return id
    }

    @Test fun onlyWidgetsTheHomeScreenShowsOrStyledAreListed() {
        val reserved = bind()
        val laidOut = bind().also(::shown)
        val styled = bind().also { store.saveSystem(it, WidgetSpec.default(WidgetType.ANALOGUE)) }
        val ids = Widgets.placed(context).map { it.first }
        assertFalse(reserved in ids)
        assertTrue(laidOut in ids && styled in ids)
        assertTrue(Widgets.isPlaced(context, laidOut))
        assertFalse(Widgets.isPlaced(context, reserved))
    }

    @Test fun removingAWidgetForgetsItsLookAndSignalsTheChange() {
        val id = bind().also(::shown)
        store.saveSystem(id, WidgetSpec.default(WidgetType.ANALOGUE))
        val before = Widgets.changes.intValue
        AnalogueClockWidget().onDeleted(context, intArrayOf(id))
        assertFalse(store.hasSystem(id))
        assertTrue(Widgets.changes.intValue > before)
    }

    @Test fun aPinIsDoneOnceTheHomeScreenTakesTheLook() {
        val pin = Widgets.PendingPin("t1", WidgetType.ANALOGUE, emptySet())
        store.saveDraft("t1", WidgetSpec.default(WidgetType.ANALOGUE))
        assertFalse(Widgets.wasPinned(context, pin))
        val id = bind()
        // The home screen's confirmation carries the new widget's id.
        WidgetPinnedReceiver().onReceive(
            context,
            Intent().putExtra(Widgets.EXTRA_DRAFT, "t1").putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
        )
        assertTrue(Widgets.wasPinned(context, pin))
        assertTrue(store.hasSystem(id))
    }

    @Test fun aNewWidgetOfTheFamilyAlsoCountsAsPinned() {
        val existing = bind()
        val pin = Widgets.PendingPin("t2", WidgetType.ANALOGUE, setOf(existing))
        store.saveDraft("t2", WidgetSpec.default(WidgetType.ANALOGUE))
        assertFalse(Widgets.wasPinned(context, pin))
        bind()
        assertTrue(Widgets.wasPinned(context, pin))
    }

    @Test fun anUnansweredRequestKeepsNoDraftWhenTheHomeScreenCannotPin() {
        shadowOf(manager).setRequestPinAppWidgetSupported(false)
        assertNull(Widgets.requestPin(context, WidgetSpec.default(WidgetType.ANALOGUE)))
        assertNull(store.recentDraft(WidgetType.ANALOGUE))
    }

    @Test fun aPinRequestKeepsTheDesignUntilAnswered() {
        shadowOf(manager).setRequestPinAppWidgetSupported(true)
        val spec = WidgetSpec.default(WidgetType.ANALOGUE).copy(opacity = 42)
        val pin = Widgets.requestPin(context, spec)
        assertNotNull(pin)
        assertEquals(42, store.recentDraft(WidgetType.ANALOGUE)?.opacity)
    }

    @Test fun theSetupScreenAppliesAJustDesignedLookAtOnce() {
        val id = bindWithInfo(501)
        store.saveDraft("t3", WidgetSpec.default(WidgetType.ANALOGUE).copy(opacity = 37))
        val activity = Robolectric.buildActivity(
            WidgetConfigActivity::class.java,
            Intent(context, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
        ).create().get()
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
        assertEquals(37, store.system(id, WidgetType.ANALOGUE).opacity)
        // The design went on this widget, so it is not offered again.
        assertNull(store.recentDraft(WidgetType.ANALOGUE))
    }

    @Test fun theSetupScreenShowsTheStudioWithoutAFreshDesign() {
        val id = bindWithInfo(502)
        val activity = Robolectric.buildActivity(
            WidgetConfigActivity::class.java,
            Intent(context, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
        ).create().get()
        assertFalse(activity.isFinishing)
    }
}
