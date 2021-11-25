package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
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
    private val productRepository: ProductRepository,
    private val notificationTokenRepository: NotificationTokenRepositoryImpl,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val orderCollectionRef = db.collection(ORDERS_COLLECTION)

    @Inject
    lateinit var service: FCMService<Any>

    // TODO("Create a paging source.")

    // get all orders if you are an admin
    suspend fun getAll(orderBy: String): List<Order> {
        return withContext(dispatcher) {
            val orderList = mutableListOf<Order>()

            val orderDocuments = orderCollectionRef
                .whereEqualTo("status", orderBy)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .await()

            if (orderDocuments.documents.isNotEmpty()) {
                for (document in orderDocuments.documents) {
                    val order = document.toObject(Order::class.java)!!.copy(id = document.id)
                    orderList.add(order.copy(id = document.id))
                }
            }
            orderList
        }
    }

    suspend fun getAllByUserId(userId: String): List<Order> {
        return withContext(dispatcher) {
            val orderList = mutableListOf<Order>()
            val orderDocuments = orderCollectionRef
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (orderDocuments.documents.isNotEmpty()) {
                for (doc in orderDocuments.documents) {
                    val order = doc.toObject<Order>()!!.copy(id = doc.id)
                    orderList.add(order)
                }
            }
            orderList
        }
    }

    // TODO("Submit order for processing and delete all items in cart.")
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
                dateOrdered = System.currentTimeMillis()
            )

            newOrder?.let { o ->
                orderCollectionRef
                    .add(o)
                    .addOnSuccessListener { doc ->
                        newOrder?.id = doc.id
                    }.addOnFailureListener {
                        newOrder = null
                        return@addOnFailureListener
                    }
            }
            newOrder
        }
    }

    suspend fun updateOrderStatus(
        userType: String,
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

        return withContext(dispatcher) {
            var isCompleted = true
            when (status) {
                Status.SHIPPED.toString() -> {
                    isCompleted = changeStatusToShipped(
                        suggestedShippingFee,
                        orderId,
                        status,
                        userType,
                        isCompleted
                    )
                }
                Status.DELIVERY.toString() -> {
                    isCompleted = changeStatusToDelivery(status, orderId, isCompleted)
                }
                Status.COMPLETED.toString() -> {
                    isCompleted = changeStatusToCompleted(status, orderId, isCompleted, userType)
                }
                Status.RETURNED.toString() -> {
                    isCompleted = changeStatusToReturned(status, orderId, userType, isCompleted)
                }
                Status.CANCELED.toString() -> {
                    // Both user and admin can execute this..
                    val updateOrderStatus = mapOf<String, Any>(
                        "status" to status
                    )

                    orderCollectionRef
                        .document(orderId)
                        .set(updateOrderStatus, SetOptions.merge())
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
            isCompleted
        }
    }

    private fun changeStatusToReturned(
        status: String,
        orderId: String,
        userType: String,
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
        status: String,
        orderId: String,
        isCompleted: Boolean,
        userType: String
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
                // TODO("Notify by getting the notification token of the user and then send it.")
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
        val isSuccessful = withContext(dispatcher) {
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
        val isSuccessful = withContext(dispatcher) {
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

                orderDocument.reference.set(agreeMap, SetOptions.merge())
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
            }
            isCompleted
        }
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
