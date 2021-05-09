package cn.cercis.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.cercis.common.CommonId

@Entity
data class Activity(
    @PrimaryKey val id: CommonId
)