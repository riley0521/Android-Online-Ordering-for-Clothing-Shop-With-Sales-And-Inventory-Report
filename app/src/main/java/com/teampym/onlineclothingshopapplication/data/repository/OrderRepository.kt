package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.presentation.client.orderlist.OrderListPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    val db: FirebaseFirestore,
    private val orderDetailRepository: OrderDetailRepository,
    private val productRepository: ProductRepository,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val auditTrailRepository: AuditTrailRepository,
    private val salesRepository: SalesRepository,
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
        additionalNote: String
    ): Order? {
        return withContext(dispatcher) {

            val newOrder = Order(
                userId = userId,
                additionalNote = additionalNote,
                deliveryInformation = deliveryInformation,
                totalCost = cartList.sumOf { it.subTotal },
                numberOfItems = cartList.size.toLong()
            )

            val result = orderCollectionRef
                .add(newOrder)
                .await()

            if (result != null) {
                return@withContext newOrder.copy(id = result.id)
            } else {
                return@withContext null
            }
        }
    }

    // Notify all admins about the items that the user wants to return.
    suspend fun returnItems(
        username: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        return withContext(dispatcher) {
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

            return@withContext notificationTokenRepository.notifyAllAdmins(
                null,
                "$username wish to return $count item/s:",
                body
            )
        }
    }

    // Notify all admins about cancellation of order.
    suspend fun cancelOrder(
        username: String,
        orderId: String,
        isCommitted: Boolean
    ): Boolean {
        return withContext(dispatcher) {

            // Change Status to Canceled
            changeStatusToCancelled("", false, orderId, Status.CANCELED.name, isCommitted)

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
        username: String,
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
            return@withContext when (status) {
                Status.SHIPPED.name -> {
                    // This is a nested method that will eventually notify the customer in the end.
                    changeStatusToShipped(
                        username,
                        suggestedShippingFee,
                        orderId,
                        status,
                        userType
                    )
                    true
                }
                Status.DELIVERY.name -> {
                    changeStatusToDelivery(username, status, orderId)
                    notificationTokenRepository.notifyCustomer(
                        obj = null,
                        userId = userId,
                        title = "Order (${orderId.take(orderId.length / 2)}...) is on it's way.",
                        body = "Buckle up because your order is on it's way to your home!"
                    )
                    true
                }
                Status.COMPLETED.name -> {
                    // Change the status to completed first
                    // Then deduct the committed to sold
                    val salesOrderList =
                        changeStatusToCompleted(username, userType, orderId, status)

                    salesRepository.insert(salesOrderList)

                    // Then notify the customer
                    notificationTokenRepository.notifyCustomer(
                        obj = null,
                        userId = userId,
                        title = "Order (${orderId.take(orderId.length / 2)}...) is completed!",
                        body = "Yey! Now you can enjoy your newly bought items!"
                    )

                    true
                }
                Status.RETURNED.name -> {
                    changeStatusToReturned(username, userType, orderId, status)
                    true
                }
                Status.CANCELED.name -> {
                    changeStatusToCancelled(username, true, orderId, status, false)
                    notificationTokenRepository.notifyCustomer(
                        obj = null,
                        userId = userId,
                        title = "Order (${orderId.take(orderId.length / 2)}...) is cancelled by admin.",
                        body = cancelReason ?: ""
                    )
                }
                else -> false
            }
        }
    }

    private suspend fun changeStatusToCancelled(
        username: String,
        isAdmin: Boolean,
        orderId: String,
        status: String,
        isCommitted: Boolean,
    ): Boolean {
        val orderDetailList = mutableListOf<OrderDetail>()
        val updateOrderStatus = mapOf<String, Any>(
            "status" to status
        )

        try {
            orderCollectionRef
                .document(orderId)
                .set(updateOrderStatus, SetOptions.merge())
                .await()

            if (isAdmin) {
                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username UPDATED order - $orderId to $status",
                        type = AuditType.ORDER.name
                    )
                )
            }

            if (isCommitted) {
                val anotherRes = orderCollectionRef
                    .document(orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .get()
                    .await()

                if (anotherRes.documents.isNotEmpty()) {
                    for (doc in anotherRes.documents) {
                        val item =
                            doc.toObject<OrderDetail>()!!.copy(id = doc.id, orderId = orderId)
                        orderDetailList.add(item)
                    }

                    return productRepository.deductCommittedToStockCount(orderDetailList)
                }
            }
        } catch (ex: Exception) {
            return false
        }
        return false
    }

    private suspend fun changeStatusToReturned(
        username: String,
        userType: String,
        orderId: String,
        status: String
    ): Boolean {
        return withContext(dispatcher) {
            val orderDetailList = mutableListOf<OrderDetail>()
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            try {
                orderCollectionRef
                    .document(orderId)
                    .set(updateOrderStatus, SetOptions.merge())
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username UPDATED order - $orderId to $status",
                        type = AuditType.ORDER.name
                    )
                )

                val orderDoc = orderCollectionRef
                    .document(orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .get()
                    .await()

                orderDoc?.let { order ->
                    for (document in order.documents) {
                        val orderDetailItem = document
                            .toObject(OrderDetail::class.java)!!.copy(
                            id = document.id,
                            orderId = orderId
                        )

                        orderDetailList.add(orderDetailItem)
                    }
                    return@withContext productRepository.deductSoldToReturnedCount(
                        userType,
                        orderDetailList
                    )
                }
            } catch (ex: java.lang.Exception) {
                return@withContext false
            }
            return@withContext false
        }
    }

    private suspend fun changeStatusToCompleted(
        username: String,
        userType: String,
        orderId: String,
        status: String
    ): List<OrderDetail> {
        return withContext(dispatcher) {
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            try {
                orderCollectionRef
                    .document(orderId)
                    .set(updateOrderStatus, SetOptions.merge())
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username UPDATED order - $orderId to $status",
                        type = AuditType.ORDER.name
                    )
                )

                return@withContext updateOrderDetailsToSold(orderId, userType)
            } catch (ex: java.lang.Exception) {
                return@withContext emptyList()
            }
        }
    }

    private suspend fun changeStatusToDelivery(
        username: String,
        status: String,
        orderId: String
    ): Boolean {
        return withContext(dispatcher) {
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            try {
                orderCollectionRef
                    .document(orderId)
                    .set(updateOrderStatus, SetOptions.merge())
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username UPDATED order - $orderId to $status",
                        type = AuditType.ORDER.name
                    )
                )

                return@withContext true
            } catch (ex: java.lang.Exception) {
                return@withContext false
            }
        }
    }

    private suspend fun changeStatusToShipped(
        username: String,
        suggestedShippingFee: Double?,
        orderId: String,
        status: String,
        userId: String
    ): Boolean {
        return withContext(dispatcher) {
            var sf = 0.0
            suggestedShippingFee?.let {
                sf = it
            }

            val orderDoc = orderCollectionRef
                .document(orderId)
                .get()
                .await()

            orderDoc?.let { order ->
                val updatedOrder = order.toObject<Order>()!!.copy(id = order.id)
                val updateOrderStatus = mapOf<String, Any>(
                    "status" to status,
                    "suggestedShippingFee" to sf
                )

                try {
                    order.reference
                        .set(updateOrderStatus, SetOptions.merge())
                        .await()

                    auditTrailRepository.insert(
                        AuditTrail(
                            username = username,
                            description = "$username UPDATED order - $orderId to $status",
                            type = AuditType.ORDER.name
                        )
                    )

                    updatedOrder.status = status
                    updatedOrder.suggestedShippingFee = sf

                    val orderDetailDocuments = orderCollectionRef
                        .document(orderId)
                        .collection(ORDER_DETAILS_SUB_COLLECTION)
                        .get()
                        .await()

                    return@withContext changeInventoryShipped(
                        orderDetailDocuments,
                        userId,
                        updatedOrder,
                        suggestedShippingFee
                    )
                } catch (ex: Exception) {
                    return@withContext false
                }
            }
            return@withContext false
        }
    }

    private suspend fun changeInventoryShipped(
        orderDetailDocuments: QuerySnapshot,
        userId: String,
        order: Order,
        suggestedShippingFee: Double?
    ): Boolean {
        return withContext(dispatcher) {
            val orderDetailList = mutableListOf<OrderDetail>()
            for (document in orderDetailDocuments.documents) {
                val orderDetailItem = document.toObject(OrderDetail::class.java)!!.copy(
                    id = document.id,
                    orderId = order.id
                )
                orderDetailList.add(orderDetailItem)
            }
            if (
                productRepository.deductStockToCommittedCount(
                    userId,
                    orderDetailList
                )
            ) {
                return@withContext notifyUserAboutShippingFeeOrMadeADeal(
                    userId,
                    order,
                    suggestedShippingFee,
                    null
                )
            }

            return@withContext false
        }
    }

    private suspend fun notifyUserAboutShippingFeeOrMadeADeal(
        userId: String,
        order: Order,
        suggestedShippingFee: Double?,
        isUserAgreedToShippingFee: Boolean?
    ): Boolean {
        return withContext(dispatcher) {

            if (suggestedShippingFee != null && suggestedShippingFee > 0.0) {
                return@withContext notificationTokenRepository.notifyCustomer(
                    order,
                    userId,
                    "Order (${order.id.take(order.id.length / 2)}...)",
                    "Admin suggests that the order fee should be $suggestedShippingFee"
                )
            } else if (isUserAgreedToShippingFee != null && isUserAgreedToShippingFee) {
                return@withContext notificationTokenRepository.notifyAllAdmins(
                    order,
                    "Order (${order.id.take(order.id.length / 2)}...)",
                    "User ($userId) has agreed to the suggested shipping fee."
                )
            }
            return@withContext false
        }
    }

    private suspend fun updateOrderDetailsToSold(
        orderId: String,
        userId: String
    ): List<OrderDetail> {
        return withContext(dispatcher) {
            val orderDetailDocs = db.collectionGroup(ORDER_DETAILS_SUB_COLLECTION)
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            if (orderDetailDocs.documents.isNotEmpty()) {

                val orderDetailList = mutableListOf<OrderDetail>()
                for (document in orderDetailDocs.documents) {
                    val orderDetailItem = document
                        .toObject(OrderDetail::class.java)!!.copy(
                        id = document.id,
                        orderId = orderId
                    )

                    orderDetailItem.dateSold = System.currentTimeMillis()
                    orderDetailItem.isExchangeable = true
                    orderDetailItem.canAddReview = true

                    try {
                        orderCollectionRef.document(orderId)
                            .collection(ORDER_DETAILS_SUB_COLLECTION)
                            .document(orderDetailItem.id)
                            .set(orderDetailItem, SetOptions.merge())
                            .await()

                        orderDetailList.add(orderDetailItem)
                    } catch (ex: Exception) {
                        return@withContext emptyList()
                    }
                }
                return@withContext productRepository.deductCommittedToSoldCount(
                    userId,
                    orderDetailList
                )
            }
            return@withContext emptyList()
        }
    }
}
