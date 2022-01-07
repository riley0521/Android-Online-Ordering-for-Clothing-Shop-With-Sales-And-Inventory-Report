package com.teampym.onlineclothingshopapplication.presentation.client.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
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
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
    private val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    private val _userSession = preferencesManager.preferencesFlow
    val userSession = _userSession.asLiveData()

    companion object {
        const val USER_ID = "user_id"
        const val USER_TYPE = "user_type"
    }

    var userId = state.get(USER_ID) ?: ""
        set(value) {
            field = value
            state.set(USER_ID, value)
        }

    var userType = state.get(USER_TYPE) ?: ""
        set(value) {
            field = value
            state.set(USER_TYPE, value)
        }

    private val _newsChannel = Channel<NewsEvent>()
    val newsEvent = _newsChannel.receiveAsFlow()

    private val events = MutableStateFlow<List<NewsFragment.NewsPagerEvent>>(emptyList())

    private val _query = db.collection(POSTS_COLLECTION)
        .orderBy("dateCreated", Query.Direction.DESCENDING)
        .limit(30)

    lateinit var posts: LiveData<PagingData<Post>>

    init {
        viewModelScope.launch {
            val session = _userSession.first()
            userId = session.userId
            userType = session.userType

            posts = postRepository
                .getSome(userId, _query)
                .flow
                .cachedIn(viewModelScope)
                .combine(events) { pagingData, events ->
                    events.fold(pagingData) { acc, event ->
                        applyEvents(acc, event)
                    }
                }.asLiveData()
        }
    }

    fun onViewEvent(
        event: NewsFragment.NewsPagerEvent
    ) = viewModelScope.launch {
        events.value += event
    }

    private fun onLikePostClicked(
        post: Post,
        isLikeByUser: Boolean
    ) = viewModelScope.launch {
        if (isLikeByUser) {
            if (userId.isNotBlank()) {
                val res = likeRepository.add(post, Like(postId = post.id, userId = userId))
                if (res) {
                    val count = post.numberOfLikes + 1
                    val postToUpdate =
                        postRepository.updateLikeCount(postId = post.id, count = count)
                    if (postToUpdate) {
                        _newsChannel.send(NewsEvent.ShowMessage("You Liked this post."))
                    }
                }
            }
        } else {
            if (userId.isNotBlank()) {
                val res = likeRepository.remove(postId = post.id, userId = userId)
                if (res) {
                    val count = post.numberOfLikes - 1
                    val postToUpdate =
                        postRepository.updateLikeCount(postId = post.id, count = count)
                    if (postToUpdate) {
                        _newsChannel.send(NewsEvent.ShowMessage("You unlike this post."))
                    }
                }
            }
        }
    }

    private fun onDeletePostClicked(post: Post) = viewModelScope.launch {

        if (userType == UserType.ADMIN.name) {
            val res = postRepository.delete(post.id)
            if (res) {
                _newsChannel.send(NewsEvent.ShowMessage("Post deleted successfully!"))
            }
        }
    }

    private fun applyEvents(
        paging: PagingData<Post>,
        event: NewsFragment.NewsPagerEvent
    ): PagingData<Post> {
        return when (event) {
            is NewsFragment.NewsPagerEvent.Update -> {
                paging.map { post ->
                    if (event.post.id == post.id) {
                        val res = onLikePostClicked(event.post, event.isLikeByUser)

                        if (res.isCompleted) {
                            return@map post.copy(
                                numberOfLikes = if (event.isLikeByUser) post.numberOfLikes++ else post.numberOfLikes--,
                                isLikedByCurrentUser = event.isLikeByUser,
                                haveUserId = true
                            )
                        } else return@map post
                    } else return@map post
                }
            }
            is NewsFragment.NewsPagerEvent.Remove -> {
                val res = viewModelScope.launch {
                    onDeletePostClicked(event.post)
                }

                if (res.isCompleted) {
                    paging.filter { event.post.id != it.id }
                } else {
                    paging.filter { event.post.id != it.id }
                }
            }
        }
    }

    sealed class NewsEvent {
        data class ShowMessage(val msg: String) : NewsEvent()
    }
}
