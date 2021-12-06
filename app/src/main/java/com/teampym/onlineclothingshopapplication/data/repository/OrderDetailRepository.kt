package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderDetailRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val orderCollectionRef = db.collection(ORDERS_COLLECTION)

    suspend fun getByOrderId(
        orderId: String,
        userType: String,
        userId: String
    ): List<OrderDetail> {

        // check the user type first
        // customer must match the userId == Orders.userId && orderId == doc.id because customer cannot view other customer's order/s
        return withContext(dispatcher) {
            val orderDetailList = mutableListOf<OrderDetail>()
            if (userType == UserType.CUSTOMER.toString()) {
                val orderDetailDocuments = orderCollectionRef
                    .document(orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .get()
                    .await()

                if (orderDetailDocuments.documents.isNotEmpty()) {
                    if (userId == orderDetailDocuments.documents[0]["userId"]) {
                        for (document in orderDetailDocuments.documents) {
                            val orderDetail = document
                                .toObject(OrderDetail::class.java)!!.copy(id = document.id)

                            orderDetailList.add(orderDetail)
                        }
                    }
                }
            } else if (userType == UserType.ADMIN.toString()) {
                // no need to compare userId when you are an admin to the order object because admin have higher access level
                val orderDetailAdminDocuments = orderCollectionRef
                    .document(orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .get()
                    .await()

                if (orderDetailAdminDocuments.documents.isNotEmpty()) {
                    for (document in orderDetailAdminDocuments.documents) {
                        val orderDetail = document
                            .toObject(OrderDetail::class.java)!!.copy(id = document.id)

                        orderDetailList.add(orderDetail)
                    }
                }
            }
            orderDetailList
        }
    }

    suspend fun insertAll(
        orderId: String,
        userId: String,
        cart: List<Cart>
    ): List<OrderDetail> {
        return withContext(dispatcher) {
            val orderDetailList = mutableListOf<OrderDetail>()
            for (item in cart) {
                val newOrderDetail = OrderDetail(
                    userId = userId,
                    orderId = orderId,
                    inventoryId = item.inventory.inventoryId,
                    size = item.inventory.size,
                    quantity = item.quantity,
                    product = item.product,
                    subTotal = item.subTotal,
                )

                val result = orderCollectionRef
                    .document(orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .add(newOrderDetail)
                    .await()

                if (result != null) {
                    orderDetailList.add(newOrderDetail)
                }
            }
            orderDetailList
        }
    }
}
