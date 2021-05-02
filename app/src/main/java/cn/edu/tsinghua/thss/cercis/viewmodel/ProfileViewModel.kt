package cn.edu.tsinghua.thss.cercis.viewmodel

import androidx.databinding.BindingAdapter
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import cn.edu.tsinghua.thss.cercis.util.Resource
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ProfileViewModel @Inject constructor(
    userRepository: UserRepository,
) : ViewModel() {
    private val currentUserResource = userRepository.userDetail().asFlow()
        .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    val currentUserLoading = Transformations.map(currentUserResource) {
        it?.let { it is Resource.Loading } ?: false
    }
    val currentUser = Transformations.map(currentUserResource) { it?.data }
    val currentUserLoadingStatus = Transformations.map(currentUserResource) {
        when (it) {
            is Resource.Error -> "Error: ${it.message}"
            is Resource.Loading -> "Loading, showing local cache"
            is Resource.Success -> "Success!"
            else -> "Preparing request"
        }
    }

    companion object {
        @JvmStatic
        @BindingAdapter("avatarImageUrl")
        fun loadImage(view: ShapeableImageView, url: String?) {
            Glide.with(view.context)
                .load(url)
                .fallback(R.drawable.outline_perm_identity_24)
                .placeholder(R.drawable.outline_perm_identity_24)
                .into(view)
        }
    }
}