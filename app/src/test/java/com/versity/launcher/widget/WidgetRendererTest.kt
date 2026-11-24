package com.versity.launcher.widget

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.FrameLayout
import android.widget.TextClock
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.versity.launcher.R
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The renderer's RemoteViews applied for real, the way a home screen would apply them, on Android 8, 12
 * and 15. Applying throws if any call is not allowed on that version, so this guards the version checks.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [26, 31, 35])
class WidgetRendererTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun apply(spec: WidgetSpec, w: Int = 300, h: Int = 150): View {
        val parent = FrameLayout(context)
        return WidgetRenderer.build(context, spec, w, h, Locale.UK).apply(context, parent)
    }

    private fun View.visible(id: Int) = findViewById<View>(id)?.visibility == View.VISIBLE

    @Test fun everyStyleAndTypefaceApplies() {
        for (style in WidgetStyle.entries) {
            for (tf in WidgetTypefaces.all) {
                val spec = WidgetSpec.default(style.type).copy(
                    style = style, typeface = tf.id, background = WidgetBackground.GLASS, border = true, twoTone = true, seconds = true,
                )
                assertNotNull("$style / ${tf.id}", apply(spec))
            }
        }
    }

    @Test fun stylesShowTheirRowsAndOnlyThose() {
        val line = apply(WidgetSpec.default(WidgetType.DIGITAL).copy(style = WidgetStyle.LINE))
        assertTrue(line.visible(R.id.w_time))
        assertTrue(!line.visible(R.id.w_ornament_row) && !line.visible(R.id.w_date_caps) && !line.visible(R.id.w_hour))

        val ornament = apply(WidgetSpec.default(WidgetType.CLOCK_DATE))
        assertTrue(ornament.visible(R.id.w_time) && ornament.visible(R.id.w_ornament_row) && ornament.visible(R.id.w_date_caps))

        val stacked = apply(WidgetSpec.default(WidgetType.DIGITAL).copy(style = WidgetStyle.STACKED))
        assertTrue(stacked.visible(R.id.w_hour) && stacked.visible(R.id.w_minute) && !stacked.visible(R.id.w_time))

        val card = apply(WidgetSpec.default(WidgetType.DATE))
        assertTrue(card.visible(R.id.w_kicker) && card.visible(R.id.w_day) && card.visible(R.id.w_date))
        assertEquals("d", (card.findViewById<TextClock>(R.id.w_day)).format24Hour.toString())

        val month = apply(WidgetSpec.default(WidgetType.DATE).copy(style = WidgetStyle.MONTH))
        assertNotNull(month.findViewById<View>(R.id.w_calendar))
    }

    @Test fun twoToneColoursTheMinutes() {
        val spec = WidgetSpec.default(WidgetType.DIGITAL).copy(style = WidgetStyle.LINE, twoTone = true, accentColor = 0xFFFF8A70, timeFormat = TimeFormat.H24)
        val format = apply(spec).findViewById<TextClock>(R.id.w_time).format24Hour
        assertTrue(format is Spanned)
        val span = (format as Spanned).getSpans(0, format.length, ForegroundColorSpan::class.java).single()
        assertEquals(0xFFFF8A70.toInt(), span.foregroundColor)
        assertEquals("mm", format.subSequence(format.getSpanStart(span), format.getSpanEnd(span)).toString())
    }

    @Test fun timeFormatsFollowTheChoice() {
        val base = WidgetSpec.default(WidgetType.DIGITAL).copy(style = WidgetStyle.LINE)
        val h12 = apply(base.copy(timeFormat = TimeFormat.H12)).findViewById<TextClock>(R.id.w_time)
        assertEquals("h:mm", h12.format24Hour.toString())
        val h24 = apply(base.copy(timeFormat = TimeFormat.H24)).findViewById<TextClock>(R.id.w_time)
        assertEquals("HH:mm", h24.format12Hour.toString())
    }

    @Test fun panelOnlyWhenAsked() {
        val bare = apply(WidgetSpec.default(WidgetType.DIGITAL))
        assertTrue(!bare.visible(R.id.w_bg) && !bare.visible(R.id.w_border))
        val glass = apply(WidgetSpec.default(WidgetType.DIGITAL).copy(background = WidgetBackground.GLASS, border = true))
        assertTrue(glass.visible(R.id.w_bg) && glass.visible(R.id.w_border))
    }

    @Test fun monthViewDrawsTheWholeMonth() {
        val bmp = MonthCalendar.render(context, WidgetSpec.default(WidgetType.DATE).copy(style = WidgetStyle.MONTH), 600, 600, LocalDate.of(2026, 11, 11), Locale.UK)
        assertEquals(600, bmp.width)
        // Today's accent disc is drawn somewhere: count accent-ish pixels.
        var accent = 0
        for (x in 0 until bmp.width step 3) for (y in 0 until bmp.height step 3) {
            val p = bmp.getPixel(x, y)
            if ((p ushr 24) > 200 && ((p shr 16) and 0xFF) > 200 && ((p shr 8) and 0xFF) < 140) accent++
        }
        assertTrue("no accent disc", accent > 20)
    }
}
