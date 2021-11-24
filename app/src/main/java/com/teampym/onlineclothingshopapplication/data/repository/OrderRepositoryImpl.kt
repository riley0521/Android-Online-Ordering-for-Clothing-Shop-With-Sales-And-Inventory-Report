package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.network.FCMService
import com.teampym.onlineclothingshopapplication.data.network.NotificationData
import com.teampym.onlineclothingshopapplication.data.network.NotificationSingle
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.util.NOTIFICATION_TOKENS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    val db: FirebaseFirestore,
    private val notificationTokenRepository: NotificationTokenRepositoryImpl,
    private val orderDetailRepository: OrderDetailRepositoryImpl,
    private val productRepository: ProductRepositoryImpl
) {

    private val orderCollectionRef = db.collection(ORDERS_COLLECTION)

    @Inject
    lateinit var service: FCMService<Any>

    // get all orders if you are an admin
    suspend fun getAll(orderBy: String): List<Order> {
        val orderList = mutableListOf<Order>()

        val ordersQuery = orderCollectionRef
            .whereEqualTo("status", orderBy)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .get()
            .await()

        if (ordersQuery.documents.isNotEmpty()) {
            // loop all order based on the status selected by the admin
            for (document in ordersQuery.documents) {
                val order = document.toObject(Order::class.java)!!.copy(id = document.id)
                orderList.add(order.copy(id = document.id))
            }
            return orderList
        }
        return orderList
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
        paymentMethod: String,
        additionalNote: String
    ): Order? {

        val createdOrder = withContext(Dispatchers.IO) {
            var newOrder: Order? = Order(
                userId = userId,
                totalCost = cartList.sumOf { it.subTotal },
                paymentMethod = paymentMethod,
                deliveryInformation = deliveryInformation,
                suggestedShippingFee = 0.0,
                additionalNote = additionalNote
            )

            newOrder?.let {
                orderCollectionRef
                    .add(it)
                    .addOnSuccessListener { doc ->
                        runBlocking {
                            val orderDetailList = orderDetailRepository.insertAll(
                                doc.id,
                                userId,
                                cartList
                            )
                            newOrder?.id = doc.id
                            newOrder?.orderDetailList = orderDetailList
                        }
                    }.addOnFailureListener {
                        newOrder = null
                        return@addOnFailureListener
                    }
            }
            return@withContext newOrder
        }
        return createdOrder
    }

    suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: String,
        suggestedShippingFee: Double?
    ): Boolean {

        // SHIPPED = Use Product Repository to update the deduct number of stock and add it to committed
        // DELIVERY = Simply update the status field in db and notify specific user with notificationToken
        // COMPLETED = Use Product Repository to update the deduct number of committed to sold
        // RETURNED = Use Product Repository to update the deduct number of committed and add it to returned
        // CANCELED = Use Product Repository to update the deduct number of committed and add it to stock
        // I think cancel should be made before the order is shipped (while in shipping mode)

        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            when (status) {
                Status.SHIPPED.toString() -> {
                    isCompleted = changeStatusToShipped(
                        suggestedShippingFee,
                        orderId,
                        status,
                        userId,
                        isCompleted
                    )
                }
                Status.DELIVERY.toString() -> {
                    isCompleted = changeStatusToDelivery(status, orderId, isCompleted)
                }
                Status.COMPLETED.toString() -> {
                    isCompleted = changeStatusToCompleted(status, orderId, isCompleted, userId)
                }
                Status.RETURNED.toString() -> {
                    isCompleted = changeStatusToReturned(status, orderId, userId, isCompleted)
                }
                Status.CANCELED.toString() -> {
                    // Both user and admin can execute this..
                    val updateOrderStatus = mapOf<String, Any>(
                        "status" to status
                    )

                    orderCollectionRef.document(orderId).set(updateOrderStatus, SetOptions.merge())
                        .addOnSuccessListener {
                            // Both user and admin can execute this..
                            // TODO("Simply update the status field in db")
                            // TODO("Notify all admins. Maybe I can create topic that admins can listen to.")
                            // TODO("Vice Versa admin can notify specific user if there are no more stocks, that is the reason why it is cancelled")
                        }.addOnFailureListener {
                            isCompleted = false
                            return@addOnFailureListener
                        }
                }
                Status.SHIPPING.toString() -> {
                    // Both user and admin can execute this..
                    // TODO("Simply update the status field in db")
                    // TODO("Notify all admins. Maybe I can create topic that admins can listen to.")
                }
            }
            return@withContext isCompleted
        }
        return isSuccessful
    }

    private suspend fun changeStatusToReturned(
        status: String,
        orderId: String,
        userId: String,
        isCompleted: Boolean
    ): Boolean {
        val isPartiallyCompleted = withContext(Dispatchers.IO) {
            var isSuccess = isCompleted
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            orderCollectionRef
                .document(orderId)
                .set(updateOrderStatus, SetOptions.merge())
                .addOnSuccessListener {
                    runBlocking {
                        val orderDetailQuery = orderCollectionRef
                            .document(orderId)
                            .collection(ORDER_DETAILS_SUB_COLLECTION)
                            .get()
                            .await()

                        if (orderDetailQuery != null) {
                            val orderDetailList = mutableListOf<OrderDetail>()
                            for (document in orderDetailQuery.documents) {
                                val orderDetailItem = document.toObject(OrderDetail::class.java)
                                orderDetailItem?.let {
                                    orderDetailList.add(it)
                                }
                            }

                            isSuccess = productRepository.deductSoldToReturnedCount(userId, orderDetailList)
                        }
                    }
                }.addOnFailureListener {
                    isSuccess = false
                    return@addOnFailureListener
                }
            return@withContext isSuccess
        }
        return isPartiallyCompleted
    }

    private suspend fun changeStatusToCompleted(
        status: String,
        orderId: String,
        isCompleted: Boolean,
        userId: String
    ): Boolean {
        val isPartiallyCompleted = withContext(Dispatchers.IO) {
            var isSuccess = isCompleted
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            orderCollectionRef
                .document(orderId)
                .set(updateOrderStatus, SetOptions.merge())
                .addOnSuccessListener {
                    runBlocking {
                        isSuccess = updateOrderDetailsToSold(orderId, userId, isSuccess)
                    }
                }.addOnFailureListener {
                    isSuccess = false
                    return@addOnFailureListener
                }
            return@withContext isSuccess
        }
        return isPartiallyCompleted
    }

    private suspend fun changeStatusToDelivery(
        status: String,
        orderId: String,
        isCompleted: Boolean
    ): Boolean {
        val isPartiallyCompleted = withContext(Dispatchers.IO) {
            var isSuccess = isCompleted
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            orderCollectionRef
                .document(orderId)
                .set(updateOrderStatus, SetOptions.merge())
                .addOnSuccessListener {
                    // TODO("Notify by getting the notification token of the user and then send it.")
                }.addOnFailureListener {
                    isSuccess = false
                    return@addOnFailureListener
                }
            return@withContext isSuccess
        }
        return isPartiallyCompleted
    }

    private suspend fun changeStatusToShipped(
        suggestedShippingFee: Double?,
        orderId: String,
        status: String,
        userId: String,
        isCompleted: Boolean
    ): Boolean {
        val isPartiallyCompleted = withContext(Dispatchers.IO) {
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

                                val orderDetailQuery = orderCollectionRef
                                    .document(orderId)
                                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                                    .get()
                                    .await()

                                isSuccess = if (orderDetailQuery != null) {
                                    changeInventoryShipped(
                                        orderDetailQuery,
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
            return@withContext isSuccess
        }
        return isPartiallyCompleted
    }

    private suspend fun changeInventoryShipped(
        orderDetailQuery: QuerySnapshot,
        userId: String,
        order: Order,
        suggestedShippingFee: Double?
    ): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            val isCompleted: Boolean

            val orderDetailList = mutableListOf<OrderDetail>()
            for (document in orderDetailQuery.documents) {
                val orderDetailItem = document.toObject(OrderDetail::class.java)
                orderDetailItem?.let {
                    orderDetailList.add(it)
                }
            }
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
            return@withContext isCompleted
        }
        return isSuccessful
    }

    private suspend fun notifyUserAboutShippingFeeOrMadeADeal(
        userId: String,
        order: Order,
        suggestedShippingFee: Double?,
        isUserAgreedToShippingFee: Boolean?
    ): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
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
                    isCompleted = notifyCustomer(
                        order,
                        userId,
                        "Order (${order.id})",
                        "Admin suggests that the order fee should be $sf"
                    )
                }
                isAgree -> {
                    isCompleted = notifyAllAdmins(
                        order,
                        "Order (${order.id})",
                        "User ($userId) has agreed to the suggested shipping fee."
                    )
                }
            }
            return@withContext isCompleted
        }
        return isSuccessful
    }

    private suspend fun notifyCustomer(
        order: Order,
        userId: String,
        title: String,
        body: String
    ): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            val data = NotificationData<Any>(
                title = title,
                body = body,
                obj = order
            )

            val notificationTokenList = notificationTokenRepository.getAll(userId)
            val tokenList: List<String> = notificationTokenList.map {
                it.token
            }

            val notificationSingle = NotificationSingle(
                data = data,
                tokenList = tokenList
            )

            service.notifySingleUser(notificationSingle)
            true
        }
        return isSuccessful
    }

    private suspend fun notifyAllAdmins(
        order: Order,
        title: String,
        body: String
    ): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            db.collectionGroup(NOTIFICATION_TOKENS_SUB_COLLECTION)
                .whereEqualTo("userType", UserType.ADMIN.name)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    runBlocking {

                        val tokenList = mutableListOf<String>()
                        for (doc in querySnapshot.documents) {
                            val token = doc.toObject<NotificationToken>()!!.copy(id = doc.id)
                            tokenList.add(token.token)
                        }

                        val data = NotificationData<Any>(
                            title = title,
                            body = body,
                            obj = order
                        )

                        val notificationSingle = NotificationSingle(
                            data = data,
                            tokenList = tokenList
                        )

                        service.notifySingleUser(notificationSingle)
                    }
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            return@withContext isCompleted
        }
        return isSuccessful
    }

    suspend fun agreeToShippingFee(userId: String, orderId: String): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            orderCollectionRef
                .document(orderId)
                .get()
                .addOnSuccessListener {
                    val updatedOrder = it.toObject<Order>()!!.copy(id = it.id)
                    val agreeMap = mapOf<String, Any>(
                        "isUserAgreedToShippingFee" to true
                    )

                    it.reference.set(agreeMap, SetOptions.merge())
                        .addOnSuccessListener {
                            runBlocking {
                                updatedOrder.isUserAgreedToShippingFee = true
                                isCompleted = notifyUserAboutShippingFeeOrMadeADeal(
                                    userId,
                                    updatedOrder,
                                    null,
                                    true
                                )
                            }
                        }.addOnFailureListener {
                            isCompleted = false
                            return@addOnFailureListener
                        }
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
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
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = currentStatus
            val orderDetailQuery = db.collectionGroup(ORDER_DETAILS_SUB_COLLECTION)
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            if (orderDetailQuery != null) {
                val orderDetailList = mutableListOf<OrderDetail>()
                for (document in orderDetailQuery.documents) {

                    val orderDetailItem = document
                        .toObject(OrderDetail::class.java)!!.copy(id = document.id)

                    orderDetailItem.let { o ->
                        val updateOrderDetailDateSold = mapOf<String, Any>(
                            "dateSold" to System.currentTimeMillis()
                        )

                        orderDetailList.add(o)
                        orderCollectionRef.document(orderId)
                            .collection(ORDER_DETAILS_SUB_COLLECTION)
                            .document(o.id)
                            .set(updateOrderDetailDateSold, SetOptions.merge())
                            .addOnSuccessListener {
                                runBlocking {
                                    productRepository.deductCommittedToSoldCount(
                                        userId,
                                        orderDetailList
                                    )
                                }
                            }.addOnFailureListener {
                                isCompleted = false
                                return@addOnFailureListener
                            }
                        return@withContext isCompleted
                    }
                }
            }
            false
        }
        return isSuccessful
    }
}
