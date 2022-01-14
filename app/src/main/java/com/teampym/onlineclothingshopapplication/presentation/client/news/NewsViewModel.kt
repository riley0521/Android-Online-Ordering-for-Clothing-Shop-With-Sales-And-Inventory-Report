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
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Like
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.network.FCMService
import com.teampym.onlineclothingshopapplication.data.network.NotificationData
import com.teampym.onlineclothingshopapplication.data.network.NotificationSingle
import com.teampym.onlineclothingshopapplication.data.repository.LikeRepository
import com.teampym.onlineclothingshopapplication.data.repository.PostRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.util.NOTIFICATION_TOKENS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    preferencesManager: PreferencesManager,
    private val postRepository: PostRepository,
    private val likeRepository: LikeRepository,
    private val state: SavedStateHandle,
    private val service: FCMService,
    @ApplicationScope val appScope: CoroutineScope
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

    fun onLikePostClicked(
        post: Post,
        isLikeByUser: Boolean
    ) = appScope.launch {
        delay(3000)

        if (isLikeByUser) {
            if (userId.isNotBlank()) {
                val likedByUserSuccess = likeRepository.add(post, Like(postId = post.id, userId = userId))
                if (likedByUserSuccess) {
                    val count = post.numberOfLikes + 1
                    postRepository.updateLikeCount(postId = post.id, count = count)

                    val res = db.collectionGroup(NOTIFICATION_TOKENS_SUB_COLLECTION)
                        .whereEqualTo("userType", UserType.ADMIN.name)
                        .get()
                        .await()

                    if (res != null && res.documents.isNotEmpty()) {
                        val tokenList = mutableListOf<String>()

                        for (doc in res.documents) {
                            val token = doc.toObject<NotificationToken>()!!.copy(id = doc.id)
                            tokenList.add(token.token)
                        }

                        val data = NotificationData(
                            title = "Someone liked your post",
                            body = "Post with title '${post.title}'",
                            postId = post.id,
                        )

                        val notificationSingle = NotificationSingle(
                            data = data,
                            tokenList = tokenList
                        )

                        service.notifySingleUser(notificationSingle)
                    }
                }
            }
        } else {
            if (userId.isNotBlank()) {
                val res = likeRepository.remove(postId = post.id, userId = userId)
                if (res) {
                    val count = post.numberOfLikes - 1
                    postRepository.updateLikeCount(postId = post.id, count = count)
                }
            }
        }
    }

    fun onDeletePostClicked(post: Post) = appScope.launch {
        if (userType == UserType.ADMIN.name) {
            delay(3000)
            postRepository.delete(post.id)
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
                        var count = post.numberOfLikes

                        if (event.isLikeByUser) {
                            count += 1
                        } else {
                            count -= 1
                        }

                        return@map post.copy(
                            numberOfLikes = count,
                            isLikedByCurrentUser = event.isLikeByUser,
                            haveUserId = true
                        )
                    } else return@map post
                }
            }
            is NewsFragment.NewsPagerEvent.Remove -> {
                paging.filter { event.post.id != it.id }
            }
        }
    }

    sealed class NewsEvent {
        data class ShowMessage(val msg: String) : NewsEvent()
    }
}
