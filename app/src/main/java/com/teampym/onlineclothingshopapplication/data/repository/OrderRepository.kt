package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.presentation.client.orderlist.OrderListPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    val db: FirebaseFirestore,
    private val orderDetailRepository: OrderDetailRepository,
    private val productRepository: ProductRepository,
    private val notificationTokenRepository: NotificationTokenRepositoryImpl,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val orderCollectionRef = db.collection(ORDERS_COLLECTION)

    fun getSome(queryOrders: Query, userId: String, userType: String) =
        Pager(
            PagingConfig(
                pageSize = 30,
                prefetchDistance = 30,
                enablePlaceholders = false
            )
        ) {
            OrderListPagingSource(
                userId,
                userType,
                queryOrders,
                orderDetailRepository
            )
        }

    // This will notify all admins about new order (SHIPPING).
    // This will return a new created order
    // Insert the order details
    // Then you can finally notify all admins in the viewModel.
    suspend fun create(
        userId: String,
        cartList: List<Cart>,
        deliveryInformation: DeliveryInformation,
        paymentMethod: String,
        additionalNote: String
    ): Order? {
        return withContext(dispatcher) {
            var newOrder: Order? = Order(
                userId = userId,
                totalCost = cartList.sumOf { it.subTotal },
                paymentMethod = paymentMethod,
                deliveryInformation = deliveryInformation,
                suggestedShippingFee = 0.0,
                additionalNote = additionalNote,
                dateOrdered = System.currentTimeMillis(),
                numberOfItems = cartList.size.toLong()
            )

            newOrder?.let { o ->
                val result = orderCollectionRef
                    .add(o)
                    .await()
                if (result != null) {
                    newOrder?.id = result.id

                    // Insert all items in the cart after adding order in database
                    newOrder?.orderDetailList = orderDetailRepository.insertAll(
                        result.id,
                        userId,
                        cartList
                    )
                } else {
                    newOrder = null
                }
            }
            newOrder
        }
    }

    // Notify all admins about the items that the user wants to return.
    suspend fun returnItems(
        username: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        return withContext(dispatcher) {
            val isSuccessful: Boolean

            val body = ""
            var count = 1

            // The output should like this
            // John Doe wants to return:
            // 1. Product1(S)
            // 2. Product2(M)
            // 3. Product3(L)
            for (item in orderDetailList) {
                body.plus("$count. ${item.product.name}(${item.size})\n")
                count++
            }

            isSuccessful = notificationTokenRepository.notifyAllAdmins(
                null,
                "$username wants to return:",
                body
            )

            isSuccessful
        }
    }

    // Notify all admins about cancellation of order.
    suspend fun cancelOrder(
        username: String,
        orderId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        return withContext(dispatcher) {

            productRepository.deductCommittedToStockCount(
                orderDetailList
            )

            val isSuccessful = notificationTokenRepository.notifyAllAdmins(
                null,
                "Order (${orderId.take(orderId.length / 2)}...) is cancelled by $username",
                "I'm sorry but I changed my mind about ordering."
            )

            isSuccessful
        }
    }

    // This will notify all admins that the user agreed to suggested shipping fee.
    suspend fun agreeToShippingFee(userId: String, orderId: String): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            val orderDocument = orderCollectionRef
                .document(orderId)
                .get()
                .await()
            if (orderDocument != null) {
                val updatedOrder = orderDocument.toObject<Order>()!!.copy(id = orderDocument.id)
                val agreeMap = mapOf<String, Any>(
                    "isUserAgreedToShippingFee" to true
                )

                val result = orderDocument.reference.set(agreeMap, SetOptions.merge()).await()
                if (result != null) {
                    updatedOrder.isUserAgreedToShippingFee = true
                    isCompleted = notifyUserAboutShippingFeeOrMadeADeal(
                        userId,
                        updatedOrder,
                        null,
                        true
                    )
                }
            }
            isCompleted
        }
    }

    // This will be executed by admins only.
    suspend fun updateOrderStatus(
        userId: String,
        userType: String,
        orderId: String,
        status: String,
        cancelReason: String?,
        suggestedShippingFee: Double?
    ): Boolean {

        // SHIPPED = Use Product Repository to update the deduct number of stock and add it to committed
        // DELIVERY = Simply update the status field in db and notify specific user with notificationToken
        // COMPLETED = Use Product Repository to update the deduct number of committed to sold
        // RETURNED = Use Product Repository to update the deduct number of committed and add it to returned
        // CANCELED = Use Product Repository to update the deduct number of committed and add it to stock
        // I think cancel should be made before the order is shipped (while in shipping mode)

        return withContext(dispatcher) {
            var isCompleted = true
            when (status) {
                Status.SHIPPED.name -> {
                    // This is a nested method that will eventually notify the customer in the end.
                    isCompleted = changeStatusToShipped(
                        suggestedShippingFee,
                        orderId,
                        status,
                        userType,
                        isCompleted
                    )
                }
                Status.DELIVERY.name -> {
                    isCompleted = changeStatusToDelivery(status, orderId, isCompleted)
                    notificationTokenRepository.notifyCustomer(
                        obj = null,
                        userId = userId,
                        title = "Order (${orderId.take(orderId.length / 2)}...) is on it's way.",
                        body = "Buckle up because your order is on it's way to your home!"
                    )
                }
                Status.COMPLETED.name -> {
                    isCompleted = changeStatusToCompleted(userType, orderId, status, isCompleted)
                    notificationTokenRepository.notifyCustomer(
                        obj = null,
                        userId = userId,
                        title = "Order (${orderId.take(orderId.length / 2)}...) is completed!",
                        body = "Yey! Now you can enjoy your newly bought items!"
                    )
                }
                Status.RETURNED.name -> {
                    isCompleted = changeStatusToReturned(userType, orderId, status, isCompleted)
                }
                Status.CANCELED.name -> {
                    isCompleted = changeStatusToCancelled(orderId, status, isCompleted)
                    notificationTokenRepository.notifyCustomer(
                        obj = null,
                        userId = userId,
                        title = "Order (${orderId.take(orderId.length / 2)}...) is cancelled by admin.",
                        body = cancelReason ?: ""
                    )
                }
            }
            isCompleted
        }
    }

    private fun changeStatusToCancelled(
        orderId: String,
        status: String,
        isCompleted: Boolean
    ): Boolean {
        var isSuccess = isCompleted
        val orderDetailList = mutableListOf<OrderDetail>()
        val updateOrderStatus = mapOf<String, Any>(
            "status" to status
        )

        orderCollectionRef
            .document(orderId)
            .set(updateOrderStatus, SetOptions.merge())
            .addOnSuccessListener {
                orderCollectionRef
                    .document(orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .get()
                    .addOnSuccessListener {
                        runBlocking {
                            for (document in it.documents) {
                                val orderDetailItem = document
                                    .toObject(OrderDetail::class.java)!!.copy(id = document.id)

                                orderDetailList.add(orderDetailItem)
                            }
                            isSuccess =
                                productRepository.deductCommittedToStockCount(
                                    orderDetailList
                                )
                        }
                    }.addOnFailureListener {
                        isSuccess = false
                        return@addOnFailureListener
                    }
            }.addOnFailureListener {
                isSuccess = false
                return@addOnFailureListener
            }
        return isSuccess
    }

    private fun changeStatusToReturned(
        userType: String,
        orderId: String,
        status: String,
        isCompleted: Boolean
    ): Boolean {
        var isSuccess = isCompleted
        val orderDetailList = mutableListOf<OrderDetail>()
        val updateOrderStatus = mapOf<String, Any>(
            "status" to status
        )

        orderCollectionRef
            .document(orderId)
            .set(updateOrderStatus, SetOptions.merge())
            .addOnSuccessListener {
                orderCollectionRef
                    .document(orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .get()
                    .addOnSuccessListener {
                        runBlocking {
                            for (document in it.documents) {
                                val orderDetailItem = document
                                    .toObject(OrderDetail::class.java)!!.copy(id = document.id)

                                orderDetailList.add(orderDetailItem)
                            }
                            isSuccess =
                                productRepository.deductSoldToReturnedCount(
                                    userType,
                                    orderDetailList
                                )
                        }
                    }.addOnFailureListener {
                        isSuccess = false
                        return@addOnFailureListener
                    }
            }.addOnFailureListener {
                isSuccess = false
                return@addOnFailureListener
            }
        return isSuccess
    }

    private fun changeStatusToCompleted(
        userType: String,
        orderId: String,
        status: String,
        isCompleted: Boolean
    ): Boolean {
        var isSuccess = isCompleted
        val updateOrderStatus = mapOf<String, Any>(
            "status" to status
        )

        orderCollectionRef
            .document(orderId)
            .set(updateOrderStatus, SetOptions.merge())
            .addOnSuccessListener {
                runBlocking {
                    isSuccess = updateOrderDetailsToSold(orderId, userType, isSuccess)
                }
            }.addOnFailureListener {
                isSuccess = false
                return@addOnFailureListener
            }
        return isSuccess
    }

    private fun changeStatusToDelivery(
        status: String,
        orderId: String,
        isCompleted: Boolean
    ): Boolean {
        var isSuccess = isCompleted
        val updateOrderStatus = mapOf<String, Any>(
            "status" to status
        )

        orderCollectionRef
            .document(orderId)
            .set(updateOrderStatus, SetOptions.merge())
            .addOnSuccessListener {
            }.addOnFailureListener {
                isSuccess = false
                return@addOnFailureListener
            }
        return isSuccess
    }

    private fun changeStatusToShipped(
        suggestedShippingFee: Double?,
        orderId: String,
        status: String,
        userId: String,
        isCompleted: Boolean
    ): Boolean {

        var isSuccess = isCompleted

        var sf = 0.0
        suggestedShippingFee?.let {
            sf = it
        }

        orderCollectionRef
            .document(orderId)
            .get()
            .addOnSuccessListener {
                val updatedOrder = it.toObject<Order>()!!.copy(id = it.id)
                val updateOrderStatus = mapOf<String, Any>(
                    "status" to status,
                    "suggestedShippingFee" to sf
                )

                it.reference.set(updateOrderStatus)
                    .addOnSuccessListener {
                        runBlocking {
                            updatedOrder.status = status
                            updatedOrder.suggestedShippingFee = sf

                            val orderDetailDocuments = orderCollectionRef
                                .document(orderId)
                                .collection(ORDER_DETAILS_SUB_COLLECTION)
                                .get()
                                .await()

                            isSuccess = if (orderDetailDocuments.documents.isNotEmpty()) {
                                changeInventoryShipped(
                                    orderDetailDocuments,
                                    userId,
                                    updatedOrder,
                                    suggestedShippingFee
                                )
                            } else {
                                false
                            }
                        }
                    }.addOnFailureListener {
                        isSuccess = false
                        return@addOnFailureListener
                    }
            }.addOnFailureListener {
                isSuccess = false
                return@addOnFailureListener
            }
        return isSuccess
    }

    private fun changeInventoryShipped(
        orderDetailDocuments: QuerySnapshot,
        userId: String,
        order: Order,
        suggestedShippingFee: Double?
    ): Boolean {
        var isCompleted: Boolean

        val orderDetailList = mutableListOf<OrderDetail>()
        for (document in orderDetailDocuments.documents) {
            val orderDetailItem =
                document.toObject(OrderDetail::class.java)!!.copy(id = document.id)
            orderDetailList.add(orderDetailItem)
        }
        runBlocking {
            isCompleted = if (productRepository.deductStockToCommittedCount(
                    userId,
                    orderDetailList
                )
            ) {
                notifyUserAboutShippingFeeOrMadeADeal(
                    userId,
                    order,
                    suggestedShippingFee,
                    null
                )
            } else {
                false
            }
        }
        return isCompleted
    }

    private suspend fun notifyUserAboutShippingFeeOrMadeADeal(
        userId: String,
        order: Order,
        suggestedShippingFee: Double?,
        isUserAgreedToShippingFee: Boolean?
    ): Boolean {
        val isSuccessful = withContext(dispatcher) {
            var isCompleted = true

            var sf = 0.0
            suggestedShippingFee?.let {
                sf = it
            }

            var isAgree = false
            isUserAgreedToShippingFee?.let {
                isAgree = it
            }

            when {
                sf > 0.0 -> {
                    isCompleted = notificationTokenRepository.notifyCustomer(
                        order,
                        userId,
                        "Order (${order.id.take(order.id.length / 2)}...)",
                        "Admin suggests that the order fee should be $sf"
                    )
                }
                isAgree -> {
                    isCompleted = notificationTokenRepository.notifyAllAdmins(
                        order,
                        "Order (${order.id.take(order.id.length / 2)}...)",
                        "User ($userId) has agreed to the suggested shipping fee."
                    )
                }
            }
            return@withContext isCompleted
        }
        return isSuccessful
    }

    private suspend fun updateOrderDetailsToSold(
        orderId: String,
        userId: String,
        currentStatus: Boolean
    ): Boolean {
        return withContext(dispatcher) {
            var isCompleted = currentStatus

            val orderDetailDocuments = db.collectionGroup(ORDER_DETAILS_SUB_COLLECTION)
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            if (orderDetailDocuments.documents.isNotEmpty()) {
                val orderDetailList = mutableListOf<OrderDetail>()
                for (document in orderDetailDocuments.documents) {
                    val orderDetailItem = document
                        .toObject(OrderDetail::class.java)!!.copy(id = document.id)

                    orderDetailItem.dateSold = System.currentTimeMillis()

                    val result = orderCollectionRef.document(orderId)
                        .collection(ORDER_DETAILS_SUB_COLLECTION)
                        .document(orderDetailItem.id)
                        .set(orderDetailItem, SetOptions.merge())
                        .await()
                    if (result != null) {
                        orderDetailList.add(orderDetailItem)
                    }
                }
                isCompleted = productRepository.deductCommittedToSoldCount(
                    userId,
                    orderDetailList
                )
            }
            isCompleted
        }
    }
}
