package com.versity.launcher.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** On other home screens the spinner's hub turns it one step per tap, and the turn is kept. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SpinnerTurnTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun hubTapTurnsThePlacedWidgetOneStepAndWrapsRound() {
        val store = WidgetStore(context)
        // A real placed widget, so the redraw after each tap goes through the widget manager.
        val id = org.robolectric.Shadows.shadowOf(AppWidgetManager.getInstance(context))
            .createWidget(RadialLauncherWidget::class.java, com.versity.launcher.R.layout.widget_radial)
        store.saveSystem(id, WidgetSpec.default(WidgetType.LAUNCHER).copy(style = WidgetStyle.RADIAL_SPINNER, turn = 6))
        val tap = Intent(context, RadialLauncherWidget::class.java)
            .setAction(Widgets.ACTION_TURN)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        RadialLauncherWidget().onReceive(context, tap)
        assertEquals(7, store.system(id, WidgetType.LAUNCHER).turn)
        RadialLauncherWidget().onReceive(context, tap)
        assertEquals(0, store.system(id, WidgetType.LAUNCHER).turn)
        // The rest of the look is untouched.
        assertEquals(WidgetStyle.RADIAL_SPINNER, store.system(id, WidgetType.LAUNCHER).style)
    }

    @Test fun everySpinnerTurnBuildsItsWidget() {
        for (turn in 0 until CollectionArt.SPIN_SLOTS) {
            val spec = WidgetSpec.default(WidgetType.LAUNCHER).copy(style = WidgetStyle.RADIAL_SPINNER, turn = turn)
            val parent = android.widget.FrameLayout(context)
            WidgetRenderer.build(context, spec, 220, 220, appWidgetId = 7).apply(context, parent)
        }
    }
}
