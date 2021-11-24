package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OrderDetailRepositoryImpl @Inject constructor(
    db: FirebaseFirestore
) {

    private val orderCollectionRef = db.collection(ORDERS_COLLECTION)

    suspend fun getByOrderId(
        orderId: String,
        userType: String,
        userId: String
    ): List<OrderDetail> {

        // check the user type first
        // customer must match the userId == Orders.userId && orderId == doc.id because customer cannot view other customer's order/s
        val orderDetailList = mutableListOf<OrderDetail>()
        if (userType == UserType.CUSTOMER.toString()) {
            val ordersQuery = orderCollectionRef
                .document(orderId)
                .collection(ORDER_DETAILS_SUB_COLLECTION)
                .get()
                .await()

            if (ordersQuery.documents.isNotEmpty()) {
                if (userId == ordersQuery.documents[0]["userId"]) {
                    for (document in ordersQuery.documents) {
                        val orderDetail = document
                            .toObject(OrderDetail::class.java)!!.copy(id = document.id)

                        orderDetailList.add(orderDetail)
                    }
                    return orderDetailList
                }
            }
        } else if (userType == UserType.ADMIN.toString()) {
            // no need to compare userId when you are an admin to the order object because admin have higher access level
            val orderDetailAdminQuery = orderCollectionRef
                .document(orderId)
                .collection(ORDER_DETAILS_SUB_COLLECTION)
                .get()
                .await()

            if (orderDetailAdminQuery != null) {
                for (document in orderDetailAdminQuery.documents) {
                    val orderDetail = document
                        .toObject(OrderDetail::class.java)!!.copy(id = document.id)

                    orderDetailList.add(orderDetail)
                }
                return orderDetailList
            }
        }
        return orderDetailList
    }

    suspend fun insertAll(
        orderId: String,
        userId: String,
        cart: List<Cart>
    ): List<OrderDetail> {
        val orderDetailList = withContext(Dispatchers.IO) {
            val orderDetailListTemp = mutableListOf<OrderDetail>()
            for (cartItem in cart) {
                val newOrderDetail = OrderDetail(
                    userId = userId,
                    orderId = orderId,
                    inventoryId = cartItem.inventory.inventoryId,
                    size = cartItem.inventory.size,
                    quantity = cartItem.quantity,
                    product = cartItem.product,
                    subTotal = cartItem.subTotal,
                )

                orderCollectionRef
                    .document(orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .document()
                    .set(newOrderDetail)
                    .addOnSuccessListener {
                        orderDetailListTemp.add(newOrderDetail)
                    }.addOnFailureListener {
                        return@addOnFailureListener
                    }
            }
            return@withContext orderDetailListTemp
        }
        return orderDetailList
    }
}
