package com.katapandroid.lazybones.wear.model

data class PlanItem(
    val id: Long,
    val text: String,
    val date: Long = 0L
)

data class ReportItem(
    val id: Long,
    val date: Long,
    val goodCount: Int,
    val badCount: Int,
    val published: Boolean,
    val goodItems: List<String>,
    val badItems: List<String>,
    val checklist: List<String>
)

data class WatchData(
    val goodCount: Int = 0,
    val badCount: Int = 0,
    val reportStatus: String? = null,
    val poolStatus: String? = null,
    val timerText: String? = null,
    val goodItems: List<String> = emptyList(),
    val badItems: List<String> = emptyList(),
    val plans: List<PlanItem> = emptyList(),
    val reports: List<ReportItem> = emptyList()
) {
    fun hasMeaningfulContent(): Boolean {
        return goodCount != 0 ||
            badCount != 0 ||
            !reportStatus.isNullOrBlank() ||
            !timerText.isNullOrBlank() ||
            plans.isNotEmpty() ||
            reports.isNotEmpty()
    }
}

