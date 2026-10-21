package com.stash.feature.nowplaying.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import com.stash.data.lyrics.parser.TtmlGroup
import com.stash.data.lyrics.parser.TtmlLine
import com.stash.data.lyrics.parser.TtmlLyrics
import com.stash.data.lyrics.parser.TtmlSyllable
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h2400dp-mdpi")
class LyricsSyllableRendererTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var shown by mutableStateOf(lyrics(listOf(syl(WORD))))
    private var shownWidth by mutableStateOf(400.dp)
    private var wordPx: Int = 0

    @Before fun setUp() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                val measurer = rememberTextMeasurer()
                val px = measurer.measure(WORD, look.lineStyle(), softWrap = false, maxLines = 1).size.width
                SideEffect { wordPx = px }
                Box(Modifier.requiredSize(shownWidth, VIEWPORT)) {
                    LyricsSyllableRenderer(
                        lyrics = shown,
                        currentPositionMs = POSITION,
                        onLineTap = {},
                        look = look,
                        isPlaying = false,
                    )
                }
            }
        }
        rule.waitForIdle()
        assertTrue("measured '$WORD' width", wordPx > 0)
    }

    @Test fun `a long line wraps at the same size instead of shrinking`() {
        val longLead = List(LONG_WORDS) { syl(WORD) }
        // Between the full-size (5.6dp) and old 60%-size (3.36dp) word gaps.
        // Robolectric measures glyphs equally across font sizes, so the gap exposes shrinking.
        val narrow = (2 * wordPx).dp + 4.48.dp

        val one = heightOf(listOf(syl(WORD)), narrow)
        val longHeight = heightOf(longLead, narrow)
        assertTrue(
            "wrapped height $longHeight should stay near $LONG_WORDS rows of $one, not the shrunk half",
            longHeight > one * (LONG_WORDS * 0.75f),
        )
    }

    private fun heightOf(lead: List<TtmlSyllable>, width: Dp): Dp {
        val line = lyrics(lead)
        rule.mainClock.autoAdvance = true
        rule.runOnIdle {
            shownWidth = width
            shown = line
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        val bounds = rule.onNodeWithContentDescription(line.lines.first().lead.text)
            .getUnclippedBoundsInRoot()
        return bounds.height
    }

    private fun syl(text: String) = TtmlSyllable(text, false, 1_000L, 1_400L)

    private fun lyrics(lead: List<TtmlSyllable>): TtmlLyrics {
        val group = TtmlGroup(lead, 1_000L, 1_400L)
        return TtmlLyrics(listOf(TtmlLine(false, group, emptyList())), group.startMs, group.endMs)
    }

    private companion object {
        const val WORD = "W"
        const val LONG_WORDS = 30
        const val POSITION = 0L
        val VIEWPORT = 1800.dp
        val look = LyricsLook(
            fontSize = 20.sp,
            bright = Color.White,
            dim = Color.White,
            ink = Color.White,
            sidePadding = 0.dp,
            lineGap = 0.dp,
            anchor = 0.dp,
            bottomRoom = 0.dp,
        )
    }
}
