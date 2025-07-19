package com.katapandroid.lazybones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katapandroid.lazybones.data.Post
import com.katapandroid.lazybones.data.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ReportsViewModel(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    
    private val _customPosts = MutableStateFlow<List<Post>>(emptyList())
    val customPosts: StateFlow<List<Post>> = _customPosts.asStateFlow()

    init {
        postRepository.getAllPosts().onEach { posts ->
            _posts.value = posts.filter { it.goodItems.isNotEmpty() || it.badItems.isNotEmpty() }
            _customPosts.value = posts.filter { it.checklist.isNotEmpty() && it.goodItems.isEmpty() && it.badItems.isEmpty() }
        }.launchIn(viewModelScope)
    }

    suspend fun addPost(post: Post) = postRepository.insert(post)
    suspend fun deletePost(post: Post) {
        postRepository.delete(post)
    }
    
    suspend fun deleteCustomPost(post: Post) {
        postRepository.delete(post)
    }
} 