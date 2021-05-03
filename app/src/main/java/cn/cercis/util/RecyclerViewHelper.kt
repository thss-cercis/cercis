package cn.cercis.util

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.*

class DataBindingViewHolder<T : ViewBinding>(val binding: T) :
    RecyclerView.ViewHolder(binding.root)

abstract class DiffRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder>(private val itemCallback: DiffUtil.ItemCallback<T>) :
    RecyclerView.Adapter<VH>() {
    /**
     * Constructor using equals to judge if contents are the same.
     */
    constructor(
        itemSameCallback: (a: T, b: T) -> Boolean,
        contentsSameCallback: (a: T, b: T) -> Boolean = Objects::equals,
    ) : this(object :
        DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) = itemSameCallback(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T) =
            contentsSameCallback(oldItem, newItem)
    })

    /**
     * Constructor using equals to judge both if contents are the same and if items are the same.
     */
    constructor() : this(object :
        DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) = Objects.equals(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T) = Objects.equals(oldItem, newItem)
    })

    private val differ by lazy {
        AsyncListDiffer(this, itemCallback)
    }

    fun submitList(list: List<T>) {
        differ.submitList(list)
    }

    val currentList: List<T>
        get() {
            return differ.currentList
        }

    override fun getItemCount(): Int {
        return currentList.size
    }
}
