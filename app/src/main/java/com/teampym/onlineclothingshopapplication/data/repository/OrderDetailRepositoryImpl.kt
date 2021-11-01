package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OrderDetailRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
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
                .get()
                .await()
            if (ordersQuery != null) {
                if (userId == ordersQuery["userId"]) {
                    val orderDetailCustomerQuery =
                        orderCollectionRef.document(orderId)
                            .collection(ORDER_DETAILS_SUB_COLLECTION).get()
                            .await()
                    for (document in orderDetailCustomerQuery.documents) {

                        val orderDetail =
                            document.toObject(OrderDetail::class.java)!!.copy(id = document.id)

                        orderDetailList.add(orderDetail)
                    }
                    return orderDetailList
                }
            }
        } else if (userType == UserType.ADMIN.toString()) {
            // no need to compare userId when you are an admin to the order object because admin have higher access level
            val orderDetailAdminQuery =
                orderCollectionRef.document(orderId).collection(ORDER_DETAILS_SUB_COLLECTION).get()
                    .await()
            if (orderDetailAdminQuery != null) {
                for (document in orderDetailAdminQuery.documents) {

                    val orderDetail =
                        document.toObject(OrderDetail::class.java)!!.copy(id = document.id)

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
    ) {
        for (cartItem in cart) {
            val newProd = OrderDetail(
                id = "",
                userId = userId,
                orderId = orderId,
                productId = cartItem.productId,
                inventoryId = cartItem.sizeInv.id,
                productName = cartItem.product.name,
                productImage = cartItem.product.imageUrl,
                size = cartItem.sizeInv.size,
                productPrice = cartItem.product.price,
                quantity = cartItem.quantity,
                subTotal = cartItem.subTotal,
                dateSold = null,
                isExchangeable = false
            )

            orderCollectionRef.document(orderId).collection(ORDER_DETAILS_SUB_COLLECTION)
                .add(newProd)
                .addOnSuccessListener {

                }.addOnFailureListener {

                }

        }
    }

}