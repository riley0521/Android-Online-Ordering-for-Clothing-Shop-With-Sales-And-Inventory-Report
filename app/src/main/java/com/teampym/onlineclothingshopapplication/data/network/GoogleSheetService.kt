package com.teampym.onlineclothingshopapplication.data.network

import retrofit2.http.POST
import retrofit2.http.Query

interface GoogleSheetService {

    @POST("exec")
    suspend fun insertOrder(
        @Query("action") action: String = "insertOrder",
        @Query("date") date: String,
        @Query("name") name: String,
        @Query("address") address: String,
        @Query("contactNumber") contactNumber: String,
        @Query("totalWithShippingFee") totalWithShippingFee: String,
        @Query("paymentMethod") paymentMethod: String,
        @Query("userId") userId: String,
    )

    @POST("exec")
    suspend fun insertInventory(
        @Query("action") action: String = "insertInventory",
        @Query("date") date: String,
        @Query("productId") productId: String,
        @Query("inventoryId") inventoryId: String,
        @Query("productName") productName: String,
        @Query("size") size: String,
        @Query("stock") stock: String,
        @Query("committed") committed: String,
        @Query("sold") sold: String,
        @Query("returned") returned: String,
        @Query("weightInKg") weightInKg: String,
    )
}
