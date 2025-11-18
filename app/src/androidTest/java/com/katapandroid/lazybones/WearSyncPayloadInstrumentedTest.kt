package com.katapandroid.lazybones

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katapandroid.lazybones.data.PlanItem
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.sync.WearDataSyncService
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class WearSyncPayloadInstrumentedTest {

    private lateinit var context: Context
    private lateinit var service: WearDataSyncService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        service = WearDataSyncService(context)
    }

    @Test
    fun `plans include post dates`() {
        val planPost = Post(
            id = 10,
            date = Date(1_700_000_000_000),
            content = "Plan holder",
            checklist = listOf("task A", "task B"),
            voiceNotes = emptyList(),
            published = true,
            isDraft = false
        )
        val planItems = listOf(
            PlanItem(id = planPost.id * 1000 + 0, text = "task A"),
            PlanItem(id = planPost.id * 1000 + 1, text = "task B")
        )
        val json = service.buildPayloadJson(
            goodCount = 1,
            badCount = 0,
            reportStatus = "PUBLISHED",
            poolStatus = "ACTIVE",
            timerText = "До конца пула: 00:10:00",
            goodItems = listOf("good"),
            badItems = emptyList(),
            plans = planItems,
            reports = listOf(planPost),
            planPosts = listOf(planPost)
        )

        val plansArray = json.getJSONArray("plans")
        assertEquals(2, plansArray.length())
        val firstPlan = plansArray.getJSONObject(0)
        assertEquals(planPost.date.time, firstPlan.getLong("date"))
        assertEquals("task A", firstPlan.getString("text"))
    }

    @Test
    fun `reports array skips drafts`() {
        val published = Post(
            id = 1,
            date = Date(),
            content = "report",
            checklist = emptyList(),
            voiceNotes = emptyList(),
            published = true,
            isDraft = false,
            goodItems = listOf("done"),
            badItems = emptyList(),
            goodCount = 1,
            badCount = 0
        )
        val draft = published.copy(id = 2, published = false, isDraft = true)

        val json = service.buildPayloadJson(
            goodCount = 1,
            badCount = 0,
            reportStatus = "PUBLISHED",
            poolStatus = "ACTIVE",
            timerText = "",
            goodItems = listOf("done"),
            badItems = emptyList(),
            plans = emptyList(),
            reports = listOf(published, draft),
            planPosts = emptyList()
        )

        val reportsArray = json.getJSONArray("reports")
        assertEquals(1, reportsArray.length())
        val item = reportsArray.getJSONObject(0)
        assertEquals(published.id, item.getLong("id"))
        assertEquals(1, item.getJSONArray("goodItems").length())
    }
}

