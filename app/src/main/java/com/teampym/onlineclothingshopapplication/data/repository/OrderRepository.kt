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
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.presentation.client.orderlist.OrderListPagingSource
import kotlinx.coroutines.CoroutineDispatcher
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

    suspend fun getOne(orderId: String): Order {
        return withContext(dispatcher) {
            val orderDoc = orderCollectionRef.document(orderId)
                .get()
                .await()

            orderDoc?.let {
                return@withContext orderDoc.toObject<Order>()!!.copy(
                    id = orderDoc.id
                )
            }

            return@withContext Order()
        }
    }

    // This will notify all admins about new order (SHIPPING).
    // This will return a new created order
    // Insert the order details
    // Then you can finally notify all admins in the viewModel.
    suspend fun create(
        userId: String,
        cartList: List<Cart>,
        deliveryInformation: DeliveryInformation,
        additionalNote: String,
        paymentMethod: PaymentMethod,
        shippingFee: Double
    ): Order? {
        return withContext(dispatcher) {

            val newOrder = Order(
                userId = userId,
                additionalNote = additionalNote,
                deliveryInformation = deliveryInformation,
                totalCost = cartList.sumOf { it.subTotal },
                numberOfItems = cartList.size.toLong(),
                paymentMethod = paymentMethod.name,
                shippingFee = shippingFee,
                courierType = "",
                trackingNumber = ""
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

    suspend fun updateOrderToPaid(orderId: String): Boolean {
        return withContext(dispatcher) {

            try {
                orderCollectionRef
                    .document(orderId)
                    .set(
                        mapOf(
                            "paid" to true
                        ),
                        SetOptions.merge()
                    ).await()

                return@withContext true
            } catch (ex: java.lang.Exception) {
                return@withContext false
            }
        }
    }

    // Notify all admins about the items that the user wants to return.
    suspend fun returnItem(
        orderItem: OrderDetail,
    ): Boolean {
        return withContext(dispatcher) {

            orderCollectionRef
                .document(orderItem.orderId)
                .collection(ORDER_DETAILS_SUB_COLLECTION)
                .document(orderItem.id)
                .set(
                    mutableMapOf(
                        "requestedToReturn" to true
                    ),
                    SetOptions.merge()
                )
                .await()

            true
        }
    }

    suspend fun confirmReturnedItem(
        orderItem: OrderDetail
    ): Boolean {
        return withContext(dispatcher) {

            orderCollectionRef
                .document(orderItem.orderId)
                .collection(ORDER_DETAILS_SUB_COLLECTION)
                .document(orderItem.id)
                .set(
                    mutableMapOf(
                        "returned" to true
                    ),
                    SetOptions.merge()
                )
                .await()

            true
        }
    }

    // Notify all admins about cancellation of order.
    suspend fun cancelOrder(
        orderId: String,
        isCommitted: Boolean,
    ): Boolean {
        return withContext(dispatcher) {

            // Change Status to Canceled
            changeStatusToCancelled("", false, orderId, Status.CANCELED.name, isCommitted)

            return@withContext true
        }
    }

    suspend fun updateStatusToCompleted(
        orderId: String
    ): Boolean {
        return withContext(dispatcher) {

            orderCollectionRef
                .document(orderId)
                .set(
                    mutableMapOf(
                        "status" to Status.COMPLETED.name,
                        "receivedByUser" to true
                    ),
                    SetOptions.merge()
                )
                .await()

            true
        }
    }

    // This will be executed by admins only.
    suspend fun updateOrderStatus(
        username: String,
        userType: String,
        orderId: String,
        status: String,
        isSfShoulderedByAdmin: Boolean = false,
        trackingNumber: String?,
        courierType: String?
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
                        orderId,
                        status,
                        userType
                    )
                    true
                }
                Status.DELIVERY.name -> {
                    changeStatusToDelivery(username, status, orderId, trackingNumber, courierType)
                    true
                }
                Status.COMPLETED.name -> {
                    // Change the status to completed first
                    // Then deduct the committed to sold
                    val salesOrderList = changeStatusToCompleted(
                        username,
                        orderId,
                        status,
                        isSfShoulderedByAdmin
                    )

                    val orderDoc = getOne(orderId)

                    salesRepository.insert(salesOrderList, orderDoc.shippingFee)
                    true
                }
                Status.CANCELED.name -> {
                    changeStatusToCancelled(username, true, orderId, status, false)
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
        isCommitted: Boolean
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
                    val orderDetailList = orderDetailRepository.getByOrderId(
                        orderId,
                        UserType.ADMIN.name,
                        ""
                    )
                    if (orderDetailList.isNotEmpty()) {
                        return@withContext productRepository.deductCommittedToStockCount(
                            orderDetailList
                        )
                    }
                }

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    private suspend fun changeStatusToCompleted(
        username: String,
        orderId: String,
        status: String,
        isSfShoulderedByAdmin: Boolean
    ): List<OrderDetail> {
        return withContext(dispatcher) {
            val orderDoc = getOne(orderId)

            val updateOrderStatus = mutableMapOf<String, Any>(
                "status" to status,
                "shippingFee" to orderDoc.shippingFee,
                "recordedToSales" to true
            )

            if (isSfShoulderedByAdmin) {
                updateOrderStatus["shippingFee"] = 0.0
            }

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

                return@withContext updateOrderDetailsToSold(orderId)
            } catch (ex: java.lang.Exception) {
                return@withContext emptyList()
            }
        }
    }

    private suspend fun changeStatusToDelivery(
        username: String,
        status: String,
        orderId: String,
        trackingNumber: String?,
        courierType: String?
    ): Boolean {
        return withContext(dispatcher) {

            var t = ""
            trackingNumber?.let {
                t = it
            }

            var c = ""
            courierType?.let {
                c = it
            }

            val updateOrderStatus = mapOf<String, Any>(
                "status" to status,
                "courierType" to c,
                "trackingNumber" to t
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
        orderId: String,
        status: String,
        userId: String
    ): Boolean {
        return withContext(dispatcher) {
            val orderDoc = orderCollectionRef
                .document(orderId)
                .get()
                .await()

            orderDoc?.let { order ->
                val updatedOrder = order.toObject<Order>()!!.copy(id = order.id)
                val updateOrderStatus = mapOf<String, Any>(
                    "status" to status
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

                    val orderDetailDocuments = orderCollectionRef
                        .document(orderId)
                        .collection(ORDER_DETAILS_SUB_COLLECTION)
                        .get()
                        .await()

                    return@withContext changeInventoryShipped(
                        orderDetailDocuments,
                        userId,
                        updatedOrder
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
        order: Order
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

            productRepository.deductStockToCommittedCount(
                userId,
                orderDetailList
            )

            return@withContext true
        }
    }

    private suspend fun updateOrderDetailsToSold(
        orderId: String
    ): List<OrderDetail> {
        return withContext(dispatcher) {
            val orderDetailList = mutableListOf<OrderDetail>()

            val orderDetailDocs = orderCollectionRef
                .document(orderId)
                .collection(ORDER_DETAILS_SUB_COLLECTION)
                .get()
                .await()

            for (document in orderDetailDocs.documents) {
                val orderDetailItem = document.toObject<OrderDetail>()!!.copy(
                    id = document.id,
                    orderId = orderId
                )

                orderDetailItem.dateSold = System.currentTimeMillis()
                orderDetailItem.canReturn = true
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
            productRepository.deductCommittedToSoldCount(
                orderDetailList
            )

            return@withContext orderDetailList
        }
    }
}
