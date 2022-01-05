package com.teampym.onlineclothingshopapplication.presentation.client.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
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
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var userType: String? = null

    private val events = MutableStateFlow<List<NewsFragment.NewsPagerEvent>>(emptyList())

    private val _newsChannel = Channel<NewsEvent>()
    val newsEvent = _newsChannel.receiveAsFlow()

    fun onViewEvent(
        event: NewsFragment.NewsPagerEvent
    ) = viewModelScope.launch {
        events.value += event
    }

    suspend fun fetchUserSession() {
        _userSession.postValue(preferencesManager.preferencesFlow.first())
        userId.postValue(_userSession.value?.userId)
        userType = _userSession.value?.userType
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

    private suspend fun onLikePostClicked(
        post: Post,
        isLikeByUser: Boolean
    ): Long {
        return withContext(Dispatchers.IO) {
            if (isLikeByUser) {
                userId.value?.let { userId ->
                    val res = likeRepository.add(
                        post,
                        Like(postId = post.id, userId = userId)
                    )
                    if (res) {
                        val count = post.numberOfLikes + 1
                        val postToUpdate =
                            postRepository.updateLikeCount(postId = post.id, count = count)
                        if (postToUpdate) {
                            _newsChannel.send(NewsEvent.ShowMessage("You Liked this post."))
                            return@withContext count
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
                            _newsChannel.send(NewsEvent.ShowMessage("You unlike this post."))
                            return@withContext count
                        }
                    }
                }
            }
            return@withContext 0L
        }
    }

    private fun onDeletePostClicked(post: Post) = viewModelScope.launch {
        userType?.let { type ->
            if (type == UserType.ADMIN.name) {
                val res = postRepository.delete(post.id)
                if (res) {
                    _newsChannel.send(NewsEvent.ShowMessage("Post deleted successfully!"))
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
                        val newCount = onLikePostClicked(event.post, event.isLikeByUser)

                        it.isLikedByCurrentUser = event.isLikeByUser
                        return@map it.copy(numberOfLikes = newCount)
                    } else return@map it
                }
            }
            is NewsFragment.NewsPagerEvent.Remove -> {
                onDeletePostClicked(event.post)

                paging.filter { event.post.id != it.id }
            }
        }
    }

    sealed class NewsEvent {
        data class ShowMessage(val msg: String) : NewsEvent()
    }
}
