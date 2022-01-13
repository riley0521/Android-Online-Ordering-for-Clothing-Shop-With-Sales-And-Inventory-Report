package com.teampym.onlineclothingshopapplication.data.room

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Entity(tableName = "table_cart")
@Parcelize
data class Cart(
    var userId: String,
    var subTotal: Double,
    @Embedded var product: Product,
    @Embedded var inventory: Inventory,
    @PrimaryKey
    var id: String = "",
    var quantity: Long = 1
) : Parcelable {

    constructor() : this(
        "",
        0.0,
        Product(),
        Inventory()
    )

    @IgnoredOnParcel
    @get:Exclude
    val calculatedTotalPrice: BigDecimal get() = quantity.toBigDecimal() * product.price.toBigDecimal()

    @IgnoredOnParcel
    @get:Exclude
    val totalWeight: Double get() = quantity * inventory.weightInKg
}
