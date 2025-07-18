package com.katapandroid.lazybones.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_items")
data class PlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String
) 