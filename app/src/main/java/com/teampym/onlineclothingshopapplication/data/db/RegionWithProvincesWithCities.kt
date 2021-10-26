package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "table_regions")
data class Region(
    @PrimaryKey
    val id: Long,
    val name: String
)

@Entity(tableName = "table_provinces")
data class Province(
    @PrimaryKey
    val id: Long,
    val regionId: Long,
    val name: String
)

data class RegionWithProvinces(
    @Embedded val region: Region,
    @Relation(
        parentColumn = "id",
        entityColumn = "regionId"
    )
    val provinces: List<Province>
)

@Entity(tableName = "table_cities")
data class City(
    @PrimaryKey
    val id: Long,
    val provinceId: Long,
    val name: String
)

data class ProvinceWithCities(
    @Embedded val province: Province,
    @Relation(
        parentColumn = "id",
        entityColumn = "provinceId"
    )
    val cities: List<City>
)