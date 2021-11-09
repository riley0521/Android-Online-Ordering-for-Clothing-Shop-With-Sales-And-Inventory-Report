package com.teampym.onlineclothingshopapplication.data.db

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PreferencesManager"
private const val SESSION_PREFERENCES = "session_preferences"

enum class SortOrder { BY_NAME, BY_POPULARITY, BY_NEWEST }

enum class PaymentMethod {
    GCASH,
    PAYMAYA,
    BPI,
    COD
}

data class SessionPreferences(
    val sortOrder: SortOrder,
    val paymentMethod: PaymentMethod,
    val userId: String
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
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_POPULARITY.name
            )
            val paymentMethod = PaymentMethod.valueOf(
                preferences[PreferencesKeys.PAYMENT_METHOD] ?: PaymentMethod.COD.name
            )

            val userId = preferences[PreferencesKeys.USER_ID] ?: ""
            SessionPreferences(sortOrder, paymentMethod, userId)
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

    private object PreferencesKeys {
        val SORT_ORDER = preferencesKey<String>("sort_order")
        val PAYMENT_METHOD = preferencesKey<String>("payment_method")
        val USER_ID = preferencesKey<String>("user_id")
    }
}
