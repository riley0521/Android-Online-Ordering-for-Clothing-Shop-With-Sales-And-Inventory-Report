package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.db.CartWithProductAndInventory
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val orderDetailRepository: OrderDetailRepositoryImpl,
    private val cartRepository: CartRepositoryImpl,
    private val productRepository: ProductRepositoryImpl
) {

    private val orderCollectionRef = db.collection(ORDERS_COLLECTION)

    // get all orders if you are an admin
    suspend fun getAll(orderBy: String): List<Order>? {
        val ordersQuery = orderCollectionRef
            .whereEqualTo("status", orderBy)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .get()
            .await()

        val orderList = mutableListOf<Order>()
        if (ordersQuery.documents.isNotEmpty()) {
            // loop all order based on the status selected by the admin
            for (document in ordersQuery.documents) {
                val order = document.toObject(Order::class.java)!!.copy(id = document.id)
                orderList.add(order.copy(id = document.id))
            }
            return orderList
        }
        return null
    }

    suspend fun getByUserId(userId: String, orderId: String): Order {
        val orderQuery = orderCollectionRef
            .document(orderId)
            .get()
            .await()

        if (orderQuery.data != null) {
            val order = orderQuery.toObject(Order::class.java)!!.copy(id = orderQuery.id)
            if (order.userId == userId)
                return order
            return Order()
        }
        return Order()
    }

    // TODO("Submit order for processing and delete all items in cart.")
    suspend fun create(
        userId: String,
        cartList: List<Cart>,
        deliveryInformation: DeliveryInformation,
        paymentMethod: String
    ): Order {

        val newOrder = Order(
            userId = userId,
            totalCost = cartList.sumOf { it.subTotal },
            paymentMethod = paymentMethod,
            deliveryInformation = deliveryInformation
        )

        orderCollectionRef
            .add(newOrder)
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    orderDetailRepository.insertAll(
                        it.id,
                        userId,
                        cartList
                    )

                    // Delete all items from cart after placing order
                    cartRepository.deleteAll(userId)
                }
            }.addOnFailureListener {
            }
        return Order()
    }

    suspend fun updateOrderStatus(userId: String, orderId: String, status: String): Boolean {

        // SHIPPED = Use Product Repository to update the deduct number of stock and add it to committed
        // DELIVERY = Simply update the status field in db and notify specific user with notificationToken
        // COMPLETED = Use Product Repository to update the deduct number of committed to sold
        // RETURNED = Use Product Repository to update the deduct number of committed and add it to returned
        // CANCELED = Use Product Repository to update the deduct number of committed and add it to stock
        // I think cancel should be made before the order is shipped (while in shipping mode)

        if (status == Status.SHIPPED.toString()) {
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            val result =
                orderCollectionRef.document(orderId).set(updateOrderStatus, SetOptions.merge())
                    .await()
            if (result != null) {
                val orderDetailQuery =
                    orderCollectionRef.document(orderId).collection("orderDetails").get().await()
                if (orderDetailQuery != null) {

                    val orderDetailList = mutableListOf<OrderDetail>()
                    for (document in orderDetailQuery.documents) {
                        val orderDetailItem = document.toObject(OrderDetail::class.java)
                        orderDetailItem?.let {
                            orderDetailList.add(it)
                        }
                    }
                    productRepository.deductStockToCommittedCount(userId, orderDetailList)
                    return true
                }
            }
        } else if (status == Status.DELIVERY.toString()) {
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            val result =
                orderCollectionRef.document(orderId).set(updateOrderStatus, SetOptions.merge())
                    .await()
            if (result != null) {
                // TODO("Notify by getting the notification token of the user and then send it.")
                return true
            }
        } else if (status == Status.COMPLETED.toString()) {
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            val result =
                orderCollectionRef.document(orderId).set(updateOrderStatus, SetOptions.merge())
                    .await()
            if (result != null) {
                val updateOrderDetailDateSold = mapOf<String, Any>(
                    "dateSold" to Calendar.getInstance().time
                )

                val orderDetailQuery =
                    db.collectionGroup("orderDetails").whereEqualTo("orderId", orderId).get()
                        .await()
                if (orderDetailQuery != null) {

                    val orderDetailList = mutableListOf<OrderDetail>()
                    for (document in orderDetailQuery.documents) {

                        val orderDetailItem = document.toObject(OrderDetail::class.java)
                        orderDetailItem?.let {
                            orderDetailList.add(it)
                            orderCollectionRef.document(orderId).collection("orderDetails")
                                .document(it.id).set(updateOrderDetailDateSold, SetOptions.merge())
                                .await()
                        }

                        productRepository.deductCommittedToSoldCount(userId, orderDetailList)
                        return true
                    }
                }
            }
        } else if (status == Status.RETURNED.toString()) {
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            val result =
                orderCollectionRef.document(orderId).set(updateOrderStatus, SetOptions.merge())
                    .await()
            if (result != null) {
                val orderDetailQuery =
                    orderCollectionRef.document(orderId).collection("orderDetails").get().await()
                if (orderDetailQuery != null) {

                    val orderDetailList = mutableListOf<OrderDetail>()
                    for (document in orderDetailQuery.documents) {
                        val orderDetailItem = document.toObject(OrderDetail::class.java)
                        orderDetailItem?.let {
                            orderDetailList.add(it)
                        }
                    }

                    productRepository.deductSoldToReturnedCount(userId, orderDetailList)
                    return true
                }
            }
        } else if (status == Status.CANCELED.toString()) {
            // Both user and admin can execute this..
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            val result =
                orderCollectionRef.document(orderId).set(updateOrderStatus, SetOptions.merge())
                    .await()
            if (result != null) {
                // Both user and admin can execute this..
                // TODO("Simply update the status field in db")
                // TODO("Notify all admins. Maybe I can create topic that admins can listen to.")
                // TODO("Vice Versa admin can notify specific user if there are no more stocks, that is the reason why it is cancelled")
                return true
            }
        } else if (status == Status.SHIPPING.toString()) {
            // Both user and admin can execute this..
            // TODO("Simply update the status field in db")
            // TODO("Notify all admins. Maybe I can create topic that admins can listen to.")
            return true
        }
        return false
    }
}
