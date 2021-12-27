package com.teampym.onlineclothingshopapplication.data.room

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PreferencesManager"
private const val SESSION_PREFERENCES = "session_preferences"

enum class SortOrder { BY_NAME, BY_POPULARITY, BY_NEWEST }

const val MOST_POPULAR = "MOST POPULAR"
const val NEWEST = "NEWEST"

enum class PaymentMethod {
    GCASH,
    PAYMAYA,
    BPI,
    COD
}

data class SessionPreferences(
    val sortOrder: SortOrder = SortOrder.BY_NAME,
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val userId: String,
    val userType: String,
    val categoryId: String
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val dataStore = context.createDataStore(SESSION_PREFERENCES)

    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_NAME.name
            )
            val paymentMethod = PaymentMethod.valueOf(
                preferences[PreferencesKeys.PAYMENT_METHOD] ?: PaymentMethod.COD.name
            )

            val userId = preferences[PreferencesKeys.USER_ID] ?: ""
            val userType = preferences[PreferencesKeys.USER_TYPE] ?: UserType.CUSTOMER.name
            val categoryId = preferences[PreferencesKeys.CATEGORY_ID] ?: ""

            SessionPreferences(sortOrder, paymentMethod, userId, userType, categoryId)
        }

    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updatePaymentMethod(paymentMethod: PaymentMethod) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAYMENT_METHOD] = paymentMethod.name
        }
    }

    suspend fun updateUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
        }
    }

    suspend fun updateUserType(userType: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_TYPE] = userType
        }
    }

    suspend fun updateCategoryId(categoryId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CATEGORY_ID] = categoryId
        }
    }

    suspend fun resetAllFields() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = SortOrder.BY_NAME.name
            preferences[PreferencesKeys.PAYMENT_METHOD] = PaymentMethod.COD.name
            preferences[PreferencesKeys.USER_ID] = ""
            preferences[PreferencesKeys.USER_TYPE] = UserType.CUSTOMER.name
            preferences[PreferencesKeys.CATEGORY_ID] = ""
        }
    }

    private object PreferencesKeys {
        val SORT_ORDER = preferencesKey<String>("sort_order")
        val PAYMENT_METHOD = preferencesKey<String>("payment_method")
        val USER_ID = preferencesKey<String>("user_id")
        val USER_TYPE = preferencesKey<String>("user_type")
        val CATEGORY_ID = preferencesKey<String>("category_id")
    }
}
