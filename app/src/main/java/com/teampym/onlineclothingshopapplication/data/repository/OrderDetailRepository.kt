package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.presentation.client.return_items.ReturnItemsPagingSource
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

    // TODO
    // Make individual order detail item per quantity of cart item.
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
                    subTotal = item.subTotal,
                    product = item.product,
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

    suspend fun update(orderDetail: OrderDetail): Boolean {
        return withContext(dispatcher) {
            try {
                orderCollectionRef
                    .document(orderDetail.orderId)
                    .collection(ORDER_DETAILS_SUB_COLLECTION)
                    .document(orderDetail.id)
                    .set(orderDetail, SetOptions.merge())
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    fun getAllRequestedToReturnItems(queryOrderItems: Query) =
        Pager(
            PagingConfig(
                pageSize = 30,
                prefetchDistance = 30,
                enablePlaceholders = false
            )
        ) {
            ReturnItemsPagingSource(queryOrderItems)
        }

    suspend fun canReturnItem(orderDetail: OrderDetail): Boolean {
        return withContext(dispatcher) {
            val res = orderCollectionRef
                .document(orderDetail.orderId)
                .collection(ORDER_DETAILS_SUB_COLLECTION)
                .document(orderDetail.id)
                .get()
                .await()
            if (res != null) {
                val item = res.toObject<OrderDetail>()!!.copy(id = res.id)
                return@withContext item.canReturn
            } else {
                false
            }
        }
    }

    suspend fun canAddReview(orderDetail: OrderDetail): Boolean {
        return withContext(dispatcher) {
            val res = orderCollectionRef
                .document(orderDetail.orderId)
                .collection(ORDER_DETAILS_SUB_COLLECTION)
                .document(orderDetail.id)
                .get()
                .await()
            if (res != null) {
                val item = res.toObject<OrderDetail>()!!.copy(id = res.id)
                return@withContext item.canAddReview
            } else {
                false
            }
        }
    }
}
