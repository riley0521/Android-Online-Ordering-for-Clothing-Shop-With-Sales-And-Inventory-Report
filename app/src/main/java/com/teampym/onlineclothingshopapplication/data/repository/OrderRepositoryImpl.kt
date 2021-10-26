package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.models.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val cartRepository: CartRepositoryImpl,
    private val productRepository: ProductRepositoryImpl
) {

    private val orderCollectionRef = db.collection("Orders")

    // get all orders if you are an admin
    suspend fun getAllOrders(orderBy: String): List<Order>? {
        val orderQuery = orderCollectionRef.whereEqualTo("status", orderBy)
            .orderBy("orderDate", Query.Direction.DESCENDING).get().await()
        val orderList = mutableListOf<Order>()
        if (orderQuery != null) {
            // loop all order based on the status selected by the admin
            for (order in orderQuery.documents) {
                orderList.add(
                    Order(
                        id = order["id"].toString(),
                        userId = order["userId"].toString(),
                        deliveryInformation = DeliveryInformation(
                            id = order["deliveryInformation.id"].toString(),
                            userId = order["deliveryInformation.userId"].toString(),
                            contactNo = order["deliveryInformation.contactNo"].toString(),
                            region = order["deliveryInformation.region"].toString(),
                            province = order["deliveryInformation.province"].toString(),
                            city = order["deliveryInformation.city"].toString(),
                            streetNumber = order["deliveryInformation.streetNumber"].toString(),
                            postalCode = order["deliveryInformation.postalCode"].toString()
                        ),
                        orderDate = SimpleDateFormat(
                            "MM/dd/yyyy",
                            Locale.ENGLISH
                        ).parse(order["orderDate"].toString())!!,
                        totalCost = order["totalCost"].toString().toBigDecimal(),
                        status = order["status"].toString(),
                        paymentMethod = order["paymentMethod"].toString(),
                        orderDetails = null
                    )
                )
            }
            return orderList
        }
        return null
    }

    suspend fun getOrderDetailByOrderId(
        orderId: String,
        userType: String,
        userId: String
    ): List<OrderDetail>? {

        // check the user type first
        // customer must match the userId == Orders.userId && orderId == doc.id because customer cannot view other customer's order/s
        if (userType == UserType.CUSTOMER.toString()) {
            val ordersQuery = orderCollectionRef
                .document(orderId)
                .get()
                .await()
            if (ordersQuery != null) {
                if (userId == ordersQuery["userId"]) {
                    val orderDetailCustomerQuery =
                        orderCollectionRef.document(orderId).collection("orderDetails").get()
                            .await()
                    val orderDetailsForCustomer = mutableListOf<OrderDetail>()
                    // loop all order details
                    for (orderDetail in orderDetailCustomerQuery.documents) {
                        orderDetailsForCustomer.add(
                            OrderDetail(
                                id = orderDetail["id"].toString(),
                                orderId = orderDetail["orderId"].toString(),
                                product = Product(
                                    id = orderDetail["product.id"].toString(),
                                    categoryId = orderDetail["product.categoryId"].toString(),
                                    name = orderDetail["product.name"].toString(),
                                    description = orderDetail["product.description"].toString(),
                                    imageUrl = orderDetail["product.imageUrl"].toString(),
                                    price = orderDetail["product.price"].toString().toBigDecimal(),
                                    flag = orderDetail["product.flag"].toString(),
                                    inventories = null,
                                    productImages = null
                                ),
                                size = orderDetail["size"].toString(),
                                price = orderDetail["price"].toString().toBigDecimal(),
                                quantity = orderDetail["quantity"].toString().toLong(),
                                subTotal = orderDetail["subTotal"].toString().toBigDecimal(),
                                dateSold = null
                            )
                        )
                        return orderDetailsForCustomer
                    }
                }
            }
        } else if (userType == UserType.ADMIN.toString()) {
            // no need to compare userId when you are an admin to the order object because admin have higher access level
            val orderDetailAdminQuery =
                orderCollectionRef.document(orderId).collection("orderDetails").get().await()
            val orderDetailsForAdmin = mutableListOf<OrderDetail>()
            if (orderDetailAdminQuery != null) {
                // loop all order details
                for (orderDetail in orderDetailAdminQuery.documents) {
                    orderDetailsForAdmin.add(
                        OrderDetail(
                            id = orderDetail["id"].toString(),
                            orderId = orderDetail["orderId"].toString(),
                            product = Product(
                                id = orderDetail["product.id"].toString(),
                                categoryId = orderDetail["product.categoryId"].toString(),
                                name = orderDetail["product.name"].toString(),
                                description = orderDetail["product.description"].toString(),
                                imageUrl = orderDetail["product.imageUrl"].toString(),
                                price = orderDetail["product.price"].toString().toBigDecimal(),
                                flag = orderDetail["product.flag"].toString(),
                                inventories = null,
                                productImages = null
                            ),
                            size = orderDetail["size"].toString(),
                            price = orderDetail["price"].toString().toBigDecimal(),
                            quantity = orderDetail["quantity"].toString().toLong(),
                            subTotal = orderDetail["subTotal"].toString().toBigDecimal(),
                            dateSold = null
                        )
                    )
                    return orderDetailsForAdmin
                }
            }
        }
        return null
    }

    // TODO("Submit order for processing and delete all items in cart.")
    suspend fun createOrderByCustomer(userInformation: UserInformation, paymentMethod: String): Order? {

        val newOrder = Order(
            id = "",
            userId = userInformation.userId,
            deliveryInformation = userInformation.deliveryInformation!!.first { it.default },
            orderDate = Calendar.getInstance().time,
            totalCost = userInformation.totalOfCart,
            status = Status.SHIPPING.toString(),
            paymentMethod = paymentMethod,
            orderDetails = null
        )

        val result = orderCollectionRef.add(newOrder).await()
        if(result != null) {
            val orderDetailList = mutableListOf<OrderDetail>()
            for(cartItem in userInformation.cart!!) {
                orderDetailList.add(
                    OrderDetail(
                        id = "",
                        orderId = result.id,
                        product = cartItem.product,
                        size = cartItem.selectedSizeFromInventory.size,
                        price = cartItem.product.price,
                        quantity = cartItem.quantity,
                        subTotal = cartItem.subTotal,
                        dateSold = null
                    )
                )
            }

            val updateOrderDetailMap = mapOf<String, Any>(
                "orderDetails" to orderDetailList
            )
            orderCollectionRef.document(result.id).set(updateOrderDetailMap, SetOptions.merge()).await()

            productRepository.deductStockToCommittedCount(userInformation.cart)
            cartRepository.deleteAllItemFromCart(userInformation.userId)
        }

        return null
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Boolean {
        if(status == Status.SHIPPED.toString()) {
            // TODO("Simply update the status field in db and notify specific user with notificationToken")
        }
        else if(status == Status.DELIVERY.toString()) {
            // TODO("Simply update the status field in db and notify specific user with notificationToken")
        }
        else if(status == Status.COMPLETED.toString()) {
            val updateOrderStatus = mapOf<String, Any>(
                "status" to status
            )

            val result = orderCollectionRef.document(orderId).set(updateOrderStatus, SetOptions.merge()).await()
            if(result != null) {
                val updateOrderDetailDateSold = mapOf<String, Any>(
                    "dateSold" to Calendar.getInstance().time
                )

                val orderDetailQuery = db.collectionGroup("orderDetails").whereEqualTo("orderId", orderId).get().await()
                if(orderDetailQuery != null) {
                    for(orderDetail in orderDetailQuery.documents) {
                        orderCollectionRef.document(orderId).collection("orderDetails").document(orderDetail.id).set(updateOrderDetailDateSold, SetOptions.merge()).await()
                    }
                }
                return true
            }
        }
        else if(status == Status.RETURNED.toString()) {
            // TODO("Use Product Repository to update the deduct number of committed and add it to returned")
        }
        else if(status == Status.CANCELED.toString()) {
            // TODO("Use Product Repository to update the deduct number of committed and add it to stock")
        }
        else {
            // TODO("Simply update the status field in db")
        }
        return false
    }

}