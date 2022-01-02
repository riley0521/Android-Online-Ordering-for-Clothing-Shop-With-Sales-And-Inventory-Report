package com.teampym.onlineclothingshopapplication.presentation.client.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.models.Like
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.repository.LikeRepository
import com.teampym.onlineclothingshopapplication.data.repository.PostRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.SessionPreferences
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private var posts: Flow<PagingData<Post>>? = null
        private set

    val postPagingData: LiveData<PagingData<Post>> get() = posts?.asLiveData()!!

    private val _userSession = MutableLiveData<SessionPreferences>()
    val userSession: LiveData<SessionPreferences> get() = _userSession

    val userId = MutableLiveData<String?>(null)

    private val events = MutableStateFlow<List<NewsFragment.NewsPagerEvent>>(emptyList())

    fun onViewEvent(
        event: NewsFragment.NewsPagerEvent
    ) = viewModelScope.launch {
        events.value += event
    }

    suspend fun fetchUserSession() {
        _userSession.value = preferencesManager.preferencesFlow.first()
        userId.value = _userSession.value?.userId
    }

    fun getPostsPagingData() {
        val query = db.collection(PRODUCTS_COLLECTION)
            .orderBy("dateCreated", Query.Direction.DESCENDING)
            .limit(30)

        posts = postRepository
            .getSome(userId.value, query)
            .flow
            .cachedIn(viewModelScope)
            .combine(events) { pagingData, events ->
                events.fold(pagingData) { acc, event ->
                    applyEvents(acc, event)
                }
            }
    }

    private fun onLikePostClicked(
        post: Post,
        isLikeByUser: Boolean
    ) = viewModelScope.launch {
        if (isLikeByUser) {
            userId.value?.let { userId ->
                val res = likeRepository.add(postId = post.id, Like(postId = post.id, userId = userId))
                if (res) {
                    val count = post.numberOfLikes + 1
                    val postToUpdate =
                        postRepository.updateLikeCount(postId = post.id, count = count)
                    if (postToUpdate) {
                    }
                }
            }
        } else {
            userId.value?.let { userId ->
                val res = likeRepository.remove(postId = post.id, userId = userId)
                if (res) {
                    val count = post.numberOfLikes - 1
                    val postToUpdate =
                        postRepository.updateLikeCount(postId = post.id, count = count)
                    if (postToUpdate) {
                    }
                }
            }
        }
    }

    private fun applyEvents(
        paging: PagingData<Post>,
        event: NewsFragment.NewsPagerEvent
    ): PagingData<Post> {
        return when (event) {
            is NewsFragment.NewsPagerEvent.Update -> {
                paging.map {
                    if (event.post.id == it.id) {
                        onLikePostClicked(event.post, event.isLikeByUser)

                        it.isLikedByCurrentUser = event.isLikeByUser
                        return@map it
                    } else return@map it
                }
            }
        }
    }
}
