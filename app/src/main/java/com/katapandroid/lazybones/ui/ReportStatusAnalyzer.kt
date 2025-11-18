package com.katapandroid.lazybones.ui

import com.katapandroid.lazybones.data.Post
import java.util.Date

data class ReportAnalysis(
    val publishedReport: Post?,
    val savedReport: Post?,
    val draftReport: Post?,
    val planPost: Post?,
    val goodCount: Int,
    val badCount: Int,
    val status: ReportStatus
)

object ReportStatusAnalyzer {
    fun analyze(posts: List<Post>, poolStart: Date, poolEnd: Date): ReportAnalysis {
        val reportsInPool = posts.filter { post ->
            val postDate = post.date
            val isInPool = postDate >= poolStart && postDate <= poolEnd
            val noChecklist = post.checklist.isEmpty()
            val hasGoodOrBad = post.goodItems.isNotEmpty() || post.badItems.isNotEmpty()
            isInPool && noChecklist && hasGoodOrBad
        }

        val publishedReport = reportsInPool.firstOrNull { !it.isDraft && it.published }
        val savedReport = reportsInPool.firstOrNull { !it.isDraft && !it.published }
        val draftReport = reportsInPool.firstOrNull { it.isDraft }

        val planPost = posts.firstOrNull { post ->
            val postDate = post.date
            val isInPool = postDate >= poolStart && postDate <= poolEnd
            val hasChecklist = post.checklist.isNotEmpty()
            isInPool && hasChecklist
        }

        val countersSource = publishedReport ?: savedReport ?: draftReport
        val goodCount = countersSource?.goodItems?.size ?: 0
        val badCount = countersSource?.badItems?.size ?: 0

        val reportStatus = when {
            publishedReport != null -> ReportStatus.PUBLISHED
            savedReport != null -> ReportStatus.SAVED
            draftReport != null -> ReportStatus.IN_PROGRESS
            else -> ReportStatus.NOT_FILLED
        }

        return ReportAnalysis(
            publishedReport = publishedReport,
            savedReport = savedReport,
            draftReport = draftReport,
            planPost = planPost,
            goodCount = goodCount,
            badCount = badCount,
            status = reportStatus
        )
    }
}

