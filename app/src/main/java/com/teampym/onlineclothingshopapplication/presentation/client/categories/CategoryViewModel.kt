package com.teampym.onlineclothingshopapplication.presentation.client.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepositoryImpl,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    fun updateUserId(userId: String) = viewModelScope.launch {
        preferencesManager.updateUserId(userId)
    }

    fun loadCategories(): LiveData<List<Category>> {

        repository.getCategories()
            .addSnapshotListener { querySnapshot, _ ->
                val categoriesFromDb = mutableListOf<Category>()
                for (doc in querySnapshot!!) {
                    val item = doc.toObject(Category::class.java)

                    val c = item.copy(id = doc.id)
                    categoriesFromDb.add(c)
                }

                _categories.value = categoriesFromDb
            }

        return categories
    }
}
