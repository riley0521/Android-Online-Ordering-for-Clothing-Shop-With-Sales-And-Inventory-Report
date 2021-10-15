package com.teampym.onlineclothingshopapplication.presentation.client.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepositoryImpl
) : ViewModel() {

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    fun loadCategories(): LiveData<List<Category>> {

        repository.getCategories()
            .addSnapshotListener { querySnapshot, _ ->
                val categoriesFromDb = mutableListOf<Category>()
                for (doc in querySnapshot!!) {
                    val item = Category(
                        doc.id,
                        doc.getString("name")!!,
                        doc.getString("imageUrl")!!
                    )

                    categoriesFromDb.add(item)
                }

                _categories.value = categoriesFromDb
            }

        return categories
    }

}