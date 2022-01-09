package com.teampym.onlineclothingshopapplication.data.room

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_inventories")
@Parcelize
data class Inventory(
    var pid: String,
    var size: String,
    var stock: Long,
    @PrimaryKey
    var inventoryId: String = "",
    @get:Exclude
    var pCartId: String = "",
    var committed: Long = 0,
    var sold: Long = 0,
    var returned: Long = 0
) : Parcelable {

    constructor() : this(
        "",
        "",
        0L
    )
}
