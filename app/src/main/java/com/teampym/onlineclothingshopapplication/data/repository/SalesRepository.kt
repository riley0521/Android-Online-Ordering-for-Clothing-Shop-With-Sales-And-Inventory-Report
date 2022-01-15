package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.DaySale
import com.teampym.onlineclothingshopapplication.data.models.MonthSale
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.YearSale
import com.teampym.onlineclothingshopapplication.data.util.DAYS_SUB_COLLECTION_OF_MONTHS
import com.teampym.onlineclothingshopapplication.data.util.MONTHS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.SALES_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val salesCollectionRef = db.collection(SALES_COLLECTION)

    suspend fun insert(soldItems: List<OrderDetail>, shippingFee: Double): Boolean {
        return withContext(dispatcher) {

            val calendarDate = Calendar.getInstance()
            calendarDate.timeInMillis = Utils.getTimeInMillisUTC()

            val year = calendarDate.get(Calendar.YEAR).toString()
            val month = Utils.getCurrentMonth(calendarDate.get(Calendar.MONTH))
            val day = calendarDate.get(Calendar.DAY_OF_MONTH).toString()

            try {
                val totalSaleOfDay = soldItems.sumOf { it.subTotal }

                getSalesForWholeYear(year)
                getDailySalesForWholeMonth(year, month)
                val dayObj = getDay(year, month, day)

                // There is multiple transactions in a day rather than month or year
                // That is why you need to reference it first instead of inserting it directly in db
                dayObj.totalSale += (totalSaleOfDay + shippingFee)
                salesCollectionRef.document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .document(month)
                    .collection(DAYS_SUB_COLLECTION_OF_MONTHS)
                    .document(day)
                    .set(dayObj, SetOptions.merge())
                    .await()

                val monthObj = getDailySalesForWholeMonth(year, month)
                val totalSaleOfMonth = monthObj.listOfDays.sumOf { it.totalSale }

                salesCollectionRef.document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .document(month)
                    .set(mapOf("totalSale" to totalSaleOfMonth), SetOptions.merge())
                    .await()

                val yearObj = getSalesForWholeYear(year)
                val totalSaleOfYear = yearObj.listOfMonth.sumOf { it.totalSale }

                salesCollectionRef.document(year)
                    .set(mapOf("totalSale" to totalSaleOfYear), SetOptions.merge())
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun getSalesForWholeYear(
        year: String
    ): YearSale {
        return withContext(dispatcher) {

            val yearDoc = salesCollectionRef.document(year)
                .get()
                .await()

            if (yearDoc.data != null) {
                val yearObj = yearDoc.toObject<YearSale>()!!.copy(id = yearDoc.id)

                val monthDocs = salesCollectionRef.document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .get()
                    .await()

                val listOfMonth = mutableListOf<MonthSale>()
                for (doc in monthDocs.documents) {
                    val monthObj = doc.toObject<MonthSale>()!!.copy(id = doc.id)
                    listOfMonth.add(monthObj)
                }

                yearObj.listOfMonth = listOfMonth
                return@withContext yearObj
            } else {
                salesCollectionRef
                    .document(year)
                    .set(
                        mapOf(
                            "id" to year,
                            "totalSale" to 0
                        ),
                        SetOptions.merge()
                    )
                    .await()

                return@withContext YearSale(id = year, totalSale = 0.0)
            }
        }
    }

    suspend fun getDailySalesForWholeMonth(
        year: String,
        monthName: String
    ): MonthSale {
        return withContext(dispatcher) {

            val monthDoc = salesCollectionRef.document(year)
                .collection(MONTHS_SUB_COLLECTION)
                .document(monthName)
                .get()
                .await()

            if (monthDoc.data != null) {
                val monthObj = monthDoc.toObject<MonthSale>()!!.copy(id = monthDoc.id)

                val dayDocs = salesCollectionRef.document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .document(monthName)
                    .collection(DAYS_SUB_COLLECTION_OF_MONTHS)
                    .get()
                    .await()

                if (dayDocs.documents.isNotEmpty()) {
                    val listOfDay = mutableListOf<DaySale>()
                    for (doc in dayDocs.documents) {
                        val dayObj = doc.toObject<DaySale>()!!.copy(id = doc.id)
                        listOfDay.add(dayObj)
                    }

                    return@withContext monthObj.copy(listOfDays = listOfDay)
                }

                return@withContext monthObj
            } else {
                salesCollectionRef
                    .document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .document(monthName)
                    .set(
                        mapOf(
                            "id" to monthName,
                            "totalSale" to 0
                        ),
                        SetOptions.merge()
                    ).await()

                return@withContext MonthSale(id = monthName, totalSale = 0.0)
            }
        }
    }

    private suspend fun getDay(year: String, monthName: String, day: String): DaySale {
        return withContext(dispatcher) {

            val dayDoc = salesCollectionRef
                .document(year)
                .collection(MONTHS_SUB_COLLECTION)
                .document(monthName)
                .collection(DAYS_SUB_COLLECTION_OF_MONTHS)
                .document(day)
                .get()
                .await()

            if (dayDoc.data != null) {
                return@withContext dayDoc.toObject<DaySale>()!!.copy(id = dayDoc.id)
            } else {
                salesCollectionRef
                    .document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .document(monthName)
                    .collection(DAYS_SUB_COLLECTION_OF_MONTHS)
                    .document(day)
                    .set(
                        mapOf(
                            "id" to day,
                            "totalSale" to 0
                        ),
                        SetOptions.merge()
                    ).await()

                return@withContext DaySale(id = day, totalSale = 0.0)
            }
        }
    }
}
