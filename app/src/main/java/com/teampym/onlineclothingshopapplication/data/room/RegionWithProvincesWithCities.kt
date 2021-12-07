package com.teampym.onlineclothingshopapplication.data.room

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_regions")
@Parcelize
data class Region(
    @PrimaryKey
    val id: Long,
    val name: String
) : Parcelable {
    constructor() : this(0L, "")
}

@Entity(tableName = "table_provinces")
@Parcelize
data class Province(
    @PrimaryKey
    val id: Long,
    val regionId: Long,
    val name: String
) : Parcelable {
    constructor() : this(0L, 0L, "")
}

data class RegionWithProvinces(
    @Embedded val region: Region,
    @Relation(
        parentColumn = "id",
        entityColumn = "regionId"
    )
    val provinces: List<Province>
)

@Entity(tableName = "table_cities")
@Parcelize
data class City(
    @PrimaryKey
    val id: Long,
    val provinceId: Long,
    val name: String
) : Parcelable {
    constructor() : this(0L, 0L, "")
}

data class ProvinceWithCities(
    @Embedded val province: Province,
    @Relation(
        parentColumn = "id",
        entityColumn = "provinceId"
    )
    val cities: List<City>
)
