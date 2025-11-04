package com.katapandroid.lazybones.core.database.mapper

import com.katapandroid.lazybones.core.database.entity.PlanItemEntity
import com.katapandroid.lazybones.core.domain.model.PlanItem

fun PlanItemEntity.toDomain(): PlanItem = PlanItem(
    id = id,
    text = text
)

fun PlanItem.toEntity(): PlanItemEntity = PlanItemEntity(
    id = id,
    text = text
)
