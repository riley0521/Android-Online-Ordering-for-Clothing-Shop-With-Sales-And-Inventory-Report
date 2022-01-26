package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.network.FCMService
import com.teampym.onlineclothingshopapplication.data.network.GoogleSheetService
import com.teampym.onlineclothingshopapplication.data.network.NotificationData
import com.teampym.onlineclothingshopapplication.data.network.NotificationSingle
import com.teampym.onlineclothingshopapplication.data.repository.NotificationTokenRepository
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.CourierType
import com.teampym.onlineclothingshopapplication.data.util.NOTIFICATION_TOKENS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val orderRepository: OrderRepository,
    private val userInformationDao: UserInformationDao,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val service: FCMService,
    private val googleSheetService: GoogleSheetService,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val searchQuery = MutableLiveData("")
    val statusQuery = MutableLiveData("")

    private val _orderListChannel = Channel<OrderListEvent>()
    val orderEvent = _orderListChannel.receiveAsFlow()

    private val _userSessionFlow = preferencesManager.preferencesFlow
    val userSession = _userSessionFlow.asLiveData()

    var orders = MutableLiveData<PagingData<Order>>()

    fun getOrders() {
        orders = combine(
            searchQuery.asFlow(),
            statusQuery.asFlow(),
            _userSessionFlow
        ) { search, status, user ->
            Triple(search, status, user)
        }.flatMapLatest { (search, status, user) ->
            val queryProducts = if (search.isEmpty()) {
                if (user.userType == UserType.CUSTOMER.name) {
                    db.collection(ORDERS_COLLECTION)
                        .whereEqualTo("status", status)
                        .whereEqualTo("userId", user.userId)
                        .orderBy("dateOrdered", Query.Direction.DESCENDING)
                        .limit(30)
                } else {
                    db.collection(ORDERS_COLLECTION)
                        .whereEqualTo("status", status)
                        .orderBy("dateOrdered", Query.Direction.ASCENDING)
                        .limit(30)
                }
            } else {
                if (user.userType == UserType.CUSTOMER.name) {
                    db.collection(ORDERS_COLLECTION)
                        .whereEqualTo(FieldPath.documentId(), search)
                        .limit(30)
                } else {
                    db.collection(ORDERS_COLLECTION)
                        .whereEqualTo(FieldPath.documentId(), search)
                        .limit(30)
                }
            }

            orderRepository.getSome(
                queryProducts,
                user.userId,
                user.userType
            ).flow.cachedIn(
                viewModelScope
            )
        }.asLiveData() as MutableLiveData<PagingData<Order>>
    }

    fun checkOrderIfCancellable(order: Order): Boolean {
        val dateOrdered = Calendar.getInstance()
        dateOrdered.timeInMillis = order.dateOrdered
        dateOrdered.timeZone = TimeZone.getTimeZone("GMT+8:00")

        // Check if today's day of year is == the day of year in dateOrdered variable
        return Calendar.getInstance()
            .get(Calendar.DAY_OF_YEAR) == dateOrdered.get(Calendar.DAY_OF_YEAR)
    }

    fun cancelOrder(
        order: Order
    ) = viewModelScope.launch {
        val canceledOrder = orderRepository.cancelOrder(
            order.id,
            false
        )

        if (canceledOrder) {

            val res = db.collectionGroup(NOTIFICATION_TOKENS_SUB_COLLECTION)
                .whereEqualTo("userType", UserType.ADMIN.name)
                .get()
                .await()

            if (res != null && res.documents.isNotEmpty()) {
                val tokenList = mutableListOf<String>()

                for (doc in res.documents) {
                    val token = doc.toObject<NotificationToken>()!!.copy(id = doc.id)
                    tokenList.add(token.token)
                }

                val data = NotificationData(
                    title = "Order (${order.id.take(order.id.length / 2)}...) is cancelled by ${order.deliveryInformation.name}",
                    body = "The user cancelled an order.",
                    orderId = order.id,
                )

                val notificationSingle = NotificationSingle(
                    data = data,
                    tokenList = tokenList
                )

                service.notifySingleUser(notificationSingle)
            }

            _orderListChannel.send(
                OrderListEvent.ShowSuccessMessage(
                    "Cancelled Order Successfully!"
                )
            )
        } else {
            _orderListChannel.send(
                OrderListEvent.ShowErrorMessage(
                    "Cancelling order failed. Please try again."
                )
            )
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun deliverOrder(order: Order, type: CourierType, trackingNumber: String) =
        viewModelScope.launch {
            val session = _userSessionFlow.first()
            val userInformation = userInformationDao.getCurrentUser(session.userId)

            userInformation?.let {
                orderRepository.updateOrderStatus(
                    username = "${userInformation.firstName} ${userInformation.lastName}",
                    userType = userInformation.userType,
                    orderId = order.id,
                    status = Status.DELIVERY.name,
                    false,
                    trackingNumber,
                    type.name
                )

                val data = NotificationData(
                    title = "Order (${order.id.take(order.id.length / 2)}...) is on it's way.",
                    body = "Buckle up because your order is on it's way to your home!",
                    orderId = order.id
                )

                val notificationTokenList = notificationTokenRepository.getAll(order.userId)

                if (notificationTokenList.isNotEmpty()) {
                    val tokenList: List<String> = notificationTokenList.map {
                        it.token
                    }

                    val notificationSingle = NotificationSingle(
                        data = data,
                        tokenList = tokenList
                    )

                    service.notifySingleUser(notificationSingle)
                }

                _orderListChannel.send(
                    OrderListEvent.ShowSuccessMessage(
                        "Changed Order status to 'For Delivery' successfully!"
                    )
                )
            }
        }

    @SuppressLint("SimpleDateFormat")
    fun shipOrder(order: Order) = viewModelScope.launch {
        val session = _userSessionFlow.first()
        val userInformation = userInformationDao.getCurrentUser(session.userId)

        userInformation?.let {
            val updatedInventories = orderRepository.updateOrderStatus(
                username = "${userInformation.firstName} ${userInformation.lastName}",
                userType = userInformation.userType,
                orderId = order.id,
                status = Status.SHIPPED.name,
                false,
                null,
                null
            )

            if (updatedInventories.isNotEmpty()) {

                val calendarDate = Calendar.getInstance()
                calendarDate.timeInMillis = Utils.getTimeInMillisUTC()
                val formattedDate = SimpleDateFormat("MM/dd/yyyy").format(calendarDate.time)

                for (inv in updatedInventories) {
                    googleSheetService.insertInventory(
                        date = formattedDate,
                        productId = inv.pid,
                        inventoryId = inv.inventoryId,
                        productName = inv.productName,
                        size = inv.size,
                        stock = inv.stock.toString(),
                        committed = inv.committed.toString(),
                        sold = inv.sold.toString(),
                        returned = inv.returned.toString(),
                        weightInKg = inv.weightInKg.toString() + " Kilogram"
                    )
                }

                val data = NotificationData(
                    title = "Order (${order.id.take(order.id.length / 2)}...) is shipped.",
                    body = "Order is shipped.",
                    orderId = order.id
                )

                val notificationTokenList = notificationTokenRepository.getAll(order.userId)

                if (notificationTokenList.isNotEmpty()) {
                    val tokenList: List<String> = notificationTokenList.map {
                        it.token
                    }

                    val notificationSingle = NotificationSingle(
                        data = data,
                        tokenList = tokenList
                    )

                    service.notifySingleUser(notificationSingle)
                }

                _orderListChannel.send(
                    OrderListEvent.ShowSuccessMessage(
                        "Changed Order status to Shipped successfully!"
                    )
                )
            } else {
                _orderListChannel.send(
                    OrderListEvent.ShowErrorMessage(
                        "Changing Order status to Shipped failed. Please try again."
                    )
                )
            }
        }
    }

    fun receivedOrder(
        order: Order,
        isMarkReceivedByAdmin: Boolean = false
    ) = viewModelScope.launch {

        val isUpdated = orderRepository.updateStatusToCompleted(
            orderId = order.id
        )

        if (isUpdated) {
            if (isMarkReceivedByAdmin) {
                val data = NotificationData(
                    title = "Order (${order.id.take(order.id.length / 2)}...)",
                    body = "Order is marked as received by admin.",
                    orderId = order.id
                )

                val notificationTokenList = notificationTokenRepository.getAll(order.userId)

                if (notificationTokenList.isNotEmpty()) {
                    val tokenList: List<String> = notificationTokenList.map {
                        it.token
                    }

                    val notificationSingle = NotificationSingle(
                        data = data,
                        tokenList = tokenList
                    )

                    service.notifySingleUser(notificationSingle)
                }
            } else {
                val res = db.collectionGroup(NOTIFICATION_TOKENS_SUB_COLLECTION)
                    .whereEqualTo("userType", UserType.ADMIN.name)
                    .get()
                    .await()

                if (res != null && res.documents.isNotEmpty()) {
                    val tokenList = mutableListOf<String>()

                    for (doc in res.documents) {
                        val token = doc.toObject<NotificationToken>()!!.copy(id = doc.id)
                        tokenList.add(token.token)
                    }

                    val data = NotificationData(
                        title = "${order.deliveryInformation.name} received your item/s.",
                        body = "Order ${order.id}",
                        orderId = order.id,
                    )

                    val notificationSingle = NotificationSingle(
                        data = data,
                        tokenList = tokenList
                    )

                    service.notifySingleUser(notificationSingle)
                }
            }

            _orderListChannel.send(
                OrderListEvent.ShowSuccessMessage(
                    "Thanks for ordering to us. Please come back later!"
                )
            )
        } else {
            _orderListChannel.send(
                OrderListEvent.ShowErrorMessage(
                    "Something went wrong. Please try again later."
                )
            )
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun completeOrder(order: Order, isSfShoulderedByAdmin: Boolean) = viewModelScope.launch {
        val session = _userSessionFlow.first()
        val userInformation = userInformationDao.getCurrentUser(session.userId)

        userInformation?.let {
            val updatedInventories = orderRepository.updateOrderStatus(
                username = "${userInformation.firstName} ${userInformation.lastName}",
                userType = userInformation.userType,
                orderId = order.id,
                status = Status.COMPLETED.name,
                isSfShoulderedByAdmin = isSfShoulderedByAdmin,
                null,
                null
            )

            if (updatedInventories.isNotEmpty()) {

                val calendarDate = Calendar.getInstance()
                calendarDate.timeInMillis = Utils.getTimeInMillisUTC()
                val formattedDate = SimpleDateFormat("MM/dd/yyyy").format(calendarDate.time)

                for (inv in updatedInventories) {
                    googleSheetService.insertInventory(
                        date = formattedDate,
                        productId = inv.pid,
                        inventoryId = inv.inventoryId,
                        productName = inv.productName,
                        size = inv.size,
                        stock = inv.stock.toString(),
                        committed = inv.committed.toString(),
                        sold = inv.sold.toString(),
                        returned = inv.returned.toString(),
                        weightInKg = inv.weightInKg.toString() + " Kilogram"
                    )
                }

                val completeAddress = "${order.deliveryInformation.streetNumber} " +
                    "${order.deliveryInformation.city}, " +
                    "${order.deliveryInformation.province}, " +
                    "${order.deliveryInformation.region}, " +
                    order.deliveryInformation.postalCode

                // Modify shipping fee to 0.0 if Shipping fee is shouldered by admin.
                order.shippingFee = if (isSfShoulderedByAdmin.not()) order.shippingFee else 0.0

                googleSheetService.insertOrder(
                    date = formattedDate,
                    name = order.deliveryInformation.name,
                    address = completeAddress,
                    contactNumber = order.deliveryInformation.contactNo,
                    totalWithShippingFee = order.totalPaymentWithShippingFee.toString(),
                    paymentMethod = order.paymentMethod,
                    userId = order.userId
                )

                _orderListChannel.send(
                    OrderListEvent.ShowSuccessMessage(
                        "Changed Order status to Completed successfully!"
                    )
                )
            } else {
                _orderListChannel.send(
                    OrderListEvent.ShowErrorMessage(
                        "Changing Order status to Completed failed. Please try again."
                    )
                )
            }
        }
    }

    fun onAdminCancelResult(
        result: String,
        order: Order
    ) = viewModelScope.launch {
        _orderListChannel.send(OrderListEvent.ShowSuccessMessage("$result - ${order.id}"))
    }

    sealed class OrderListEvent {
        data class ShowSuccessMessage(val msg: String) : OrderListEvent()
        data class ShowErrorMessage(val msg: String) : OrderListEvent()
    }
}
