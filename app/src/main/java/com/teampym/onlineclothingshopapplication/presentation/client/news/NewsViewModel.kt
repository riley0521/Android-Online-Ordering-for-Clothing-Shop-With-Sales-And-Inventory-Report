package com.teampym.onlineclothingshopapplication.presentation.client.news

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val state: SavedStateHandle
) : ViewModel() {
}