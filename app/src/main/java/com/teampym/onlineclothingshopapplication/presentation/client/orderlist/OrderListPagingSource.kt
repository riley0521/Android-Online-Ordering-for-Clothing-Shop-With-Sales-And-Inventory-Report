package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.repository.OrderDetailRepository
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

class OrderListPagingSource(
    private val userId: String,
    private val userType: String,
    private val queryOrders: Query,
    private val orderDetailRepository: OrderDetailRepository
) : PagingSource<QuerySnapshot, Order>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Order>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Order> {
        return try {

            val currentPage = params.key ?: queryOrders
                .get()
                .await()

            var nextPage: QuerySnapshot? = null

            if (currentPage.size() > 30) {
                val lastVisibleItem = currentPage!!.documents[currentPage.size() - 1]
                nextPage = queryOrders.startAfter(lastVisibleItem).get().await()
            }

            var orderList = mutableListOf<Order>()
            for (document in currentPage.documents) {

                var order = document.toObject<Order>()!!.copy(id = document.id)
                CoroutineScope(Dispatchers.IO).launch {
                    val orderDetailList = async {
                        orderDetailRepository.getByOrderId(
                            document.id,
                            userType,
                            order.userId
                        )
                    }

                    order = order.copy(
                        orderDetailList = orderDetailList.await()
                    )
                }
                orderList.add(order)
            }

            if (userType == UserType.CUSTOMER.name) {
                val isCorrectUser = orderList.firstOrNull { it.userId == userId }
                if (isCorrectUser != null) {
                    orderList.sortByDescending { it.dateOrdered }
                } else {
                    orderList = mutableListOf()
                }
            }

            LoadResult.Page(
                data = orderList,
                prevKey = null,
                nextKey = nextPage
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
