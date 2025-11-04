package com.katapandroid.lazybones.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_items")
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String
)
