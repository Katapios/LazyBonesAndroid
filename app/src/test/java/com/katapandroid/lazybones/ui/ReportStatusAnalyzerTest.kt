package com.katapandroid.lazybones.ui

import com.katapandroid.lazybones.data.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.Date

class ReportStatusAnalyzerTest {

    private val now = Date()
    private val poolStart = Date(now.time - 1_000)
    private val poolEnd = Date(now.time + 1_000)

    @Test
    fun `published report has priority`() {
        val published = createReport(published = true, goodItems = listOf("done"))
        val saved = createReport(published = false, isDraft = false)
        val analysis = ReportStatusAnalyzer.analyze(listOf(published, saved), poolStart, poolEnd)

        assertSame(published, analysis.publishedReport)
        assertEquals(ReportStatus.PUBLISHED, analysis.status)
        assertEquals(1, analysis.goodCount)
    }

    @Test
    fun `saved report used when no published`() {
        val saved = createReport(published = false, isDraft = false, badItems = listOf("nope"))
        val draft = createReport(isDraft = true)
        val analysis = ReportStatusAnalyzer.analyze(listOf(saved, draft), poolStart, poolEnd)

        assertSame(saved, analysis.savedReport)
        assertEquals(ReportStatus.SAVED, analysis.status)
        assertEquals(1, analysis.badCount)
    }

    @Test
    fun `draft report controls when only draft`() {
        val draft = createReport(isDraft = true, goodItems = listOf("wip"))
        val analysis = ReportStatusAnalyzer.analyze(listOf(draft), poolStart, poolEnd)

        assertSame(draft, analysis.draftReport)
        assertEquals(ReportStatus.IN_PROGRESS, analysis.status)
        assertEquals(1, analysis.goodCount)
    }

    @Test
    fun `plan post detected when checklist present`() {
        val plan = Post(
            id = 99,
            date = now,
            content = "plan",
            checklist = listOf("task"),
            voiceNotes = emptyList(),
            published = true
        )
        val analysis = ReportStatusAnalyzer.analyze(listOf(plan), poolStart, poolEnd)
        assertSame(plan, analysis.planPost)
    }

    private fun createReport(
        published: Boolean = false,
        isDraft: Boolean = false,
        goodItems: List<String> = emptyList(),
        badItems: List<String> = emptyList()
    ) = Post(
        id = System.nanoTime(),
        date = now,
        content = "report",
        checklist = emptyList(),
        voiceNotes = emptyList(),
        published = published,
        isDraft = isDraft,
        goodItems = goodItems,
        badItems = badItems,
        goodCount = goodItems.size,
        badCount = badItems.size
    )
}

