package com.teampym.onlineclothingshopapplication.data.room

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val TAG = "PreferencesManager"
private const val SESSION_PREFERENCES = "session_preferences"

enum class SortOrder { BY_NAME, BY_POPULARITY, BY_NEWEST }

const val MOST_POPULAR = "MOST POPULAR"
const val NEWEST = "NEWEST"

enum class PaymentMethod {
    COD,
    CREDIT_DEBIT
}

data class SessionPreferences(
    val sortOrder: SortOrder = SortOrder.BY_NAME,
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val filterLogType: AuditType = AuditType.CATEGORY,
    val userId: String = "",
    val userType: String = "",
    val categoryId: String = ""
)

val Context.dataStore by preferencesDataStore(
    name = SESSION_PREFERENCES
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val preferencesFlow = context.dataStore.data
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
            val logType = AuditType.valueOf(
                preferences[PreferencesKeys.LOG_TYPE] ?: AuditType.CATEGORY.name
            )

            val userId = preferences[PreferencesKeys.USER_ID] ?: ""
            val userType = preferences[PreferencesKeys.USER_TYPE] ?: ""
            val categoryId = preferences[PreferencesKeys.CATEGORY_ID] ?: ""

            SessionPreferences(sortOrder, paymentMethod, logType, userId, userType, categoryId)
        }

    suspend fun updateSortOrder(sortOrder: SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updatePaymentMethod(paymentMethod: PaymentMethod) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAYMENT_METHOD] = paymentMethod.name
        }
    }

    suspend fun updateFilterLogType(auditType: AuditType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOG_TYPE] = auditType.name
        }
    }

    suspend fun updateUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
        }
    }

    suspend fun getUserType(): String {
        return context.dataStore.data.map { it[PreferencesKeys.USER_TYPE] ?: "" }.first()
    }

    suspend fun updateUserType(userType: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_TYPE] = userType
        }
    }

    suspend fun updateCategoryId(categoryId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CATEGORY_ID] = categoryId
        }
    }

    suspend fun resetAllFields() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = SortOrder.BY_NAME.name
            preferences[PreferencesKeys.PAYMENT_METHOD] = PaymentMethod.COD.name
            preferences[PreferencesKeys.LOG_TYPE] = AuditType.CATEGORY.name
            preferences[PreferencesKeys.USER_ID] = ""
            preferences[PreferencesKeys.USER_TYPE] = ""
            preferences[PreferencesKeys.CATEGORY_ID] = ""
        }
    }

    object PreferencesKeys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val PAYMENT_METHOD = stringPreferencesKey("payment_method")
        val LOG_TYPE = stringPreferencesKey("log_type")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_TYPE = stringPreferencesKey("user_type")
        val CATEGORY_ID = stringPreferencesKey("category_id")
    }
}
