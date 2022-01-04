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
import java.lang.Exception
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val salesCollectionRef = db.collection(SALES_COLLECTION)

    suspend fun insert(soldItems: List<OrderDetail>): Boolean {
        return withContext(dispatcher) {

            val calendarDate = Calendar.getInstance()
            calendarDate.timeInMillis = Utils.getTimeInMillisUTC()
            calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
            val year = calendarDate.get(Calendar.YEAR).toString()
            val month = Utils.getCurrentMonth(calendarDate.get(Calendar.MONTH))
            val day = calendarDate.get(Calendar.DAY_OF_MONTH).toString()

            try {
                val totalSaleOfDay = 0.0
                soldItems.forEach {
                    totalSaleOfDay.plus(it.calculatedPrice.toDouble())
                }

                val dayRef = salesCollectionRef.document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .document(month)
                    .collection(DAYS_SUB_COLLECTION_OF_MONTHS)
                    .document(day)
                    .get()
                    .await()

                // There is multiple transactions in a day rather than month or year
                // That is why you need to reference it first instead of inserting it directly in db
                dayRef?.let { doc ->
                    val dayObj = doc.toObject<DaySale>()!!.copy(id = doc.id)
                    dayObj.totalSale.plus(totalSaleOfDay)
                    doc.reference
                        .set(dayObj, SetOptions.merge())
                        .await()
                }

                val totalSaleOfMonth = 0.0
                getDailySalesForWholeMonth(year, month).forEach {
                    totalSaleOfMonth.plus(it.totalSale)
                }

                salesCollectionRef.document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .document(month)
                    .set(MonthSale(totalSaleOfMonth), SetOptions.merge())
                    .await()

                val yearObj = getSalesForWholeYear(year)
                val totalSaleOfYear = 0.0
                yearObj?.let {
                    yearObj.listOfMonth.forEach {
                        totalSaleOfYear.plus(it.totalSale)
                    }

                    salesCollectionRef.document(year)
                        .set(yearObj, SetOptions.merge())
                        .await()
                }

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun getSalesForWholeYear(
        year: String
    ): YearSale? {
        return withContext(dispatcher) {

            val yearDoc = salesCollectionRef.document(year)
                .get()
                .await()

            yearDoc?.let { docRef ->
                val yearObj = docRef.toObject<YearSale>()!!.copy(id = docRef.id)

                val monthDocs = salesCollectionRef.document(year)
                    .collection(MONTHS_SUB_COLLECTION)
                    .get()
                    .await()

                val listOfMonth = mutableListOf<MonthSale>()
                monthDocs?.let { months ->
                    for (doc in months.documents) {
                        val monthObj = doc.toObject<MonthSale>()!!.copy(id = doc.id)
                        listOfMonth.add(monthObj)
                    }
                }

                yearObj.listOfMonth = listOfMonth
                return@withContext yearObj
            }

            return@withContext null
        }
    }

    suspend fun getDailySalesForWholeMonth(
        year: String,
        monthName: String
    ): List<DaySale> {
        return withContext(dispatcher) {

            val dayDocs = salesCollectionRef.document(year)
                .collection(MONTHS_SUB_COLLECTION)
                .document(monthName)
                .collection(DAYS_SUB_COLLECTION_OF_MONTHS)
                .get()
                .await()

            val listOfDay = mutableListOf<DaySale>()
            dayDocs?.let { days ->
                for (doc in days.documents) {
                    val dayObj = doc.toObject<DaySale>()!!.copy(id = doc.id)
                    listOfDay.add(dayObj)
                }
            }

            return@withContext listOfDay
        }
    }
}
