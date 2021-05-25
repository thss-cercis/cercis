package cn.cercis.util.helper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.*

class DataBindingViewHolder<T : ViewBinding>(val binding: T) :
    RecyclerView.ViewHolder(binding.root)

abstract class DiffRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder>(
    private val itemCallback: DiffUtil.ItemCallback<T>,
) : RecyclerView.Adapter<VH>() {
    /**
     * Constructor using equals to judge if contents are the same.
     */
    constructor(
        itemSameCallback: (T, T) -> Boolean,
        contentsSameCallback: (T, T) -> Boolean,
    ) : this(object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) = itemSameCallback(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T) =
            contentsSameCallback(oldItem, newItem)
    })

    /**
     * Constructor using equals to judge both if contents are the same and if items are the same.
     */
    constructor() : this(object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) = Objects.equals(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T) = Objects.equals(oldItem, newItem)
    })

    private val differ by lazy {
        AsyncListDiffer(this, itemCallback)
    }

    fun submitList(list: List<T>?) {
        if (list != null) {
            differ.submitList(list)
        }
    }

    val currentList: List<T>
        get() {
            return differ.currentList
        }

    override fun getItemCount(): Int {
        return currentList.size
    }

    companion object {
        abstract class BindingAdapter<T, B : ViewDataBinding>(
            itemSameCallback: (T, T) -> Boolean,
            contentsSameCallback: (T, T) -> Boolean,
        ) : DiffRecyclerViewAdapter<T, DataBindingViewHolder<B>>(
            itemSameCallback,
            contentsSameCallback
        )

        fun <T, B : ViewDataBinding, C : Comparable<C>> getInstance(
            dataSource: LiveData<List<T>>,
            viewLifecycleOwnerSupplier: () -> LifecycleOwner,
            itemIndex: T.() -> C,
            contentsSameCallback: (T, T) -> Boolean,
            inflater: (inflater: LayoutInflater, parent: ViewGroup, viewType: Int) -> B,
            onBindViewHolderWithExecution: BindingAdapter<T, B>.(
                holder: DataBindingViewHolder<B>,
                position: Int,
            ) -> Unit,
            itemViewType: T.() -> Int = { 0 },
        ) = object : BindingAdapter<T, B>(
            { oldItem, newItem -> oldItem.itemIndex().compareTo(newItem.itemIndex()) == 0 },
            contentsSameCallback
        ) {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
            ): DataBindingViewHolder<B> {
                // Do not set lifecycleOwner here or it will call executeBinding with null
                return DataBindingViewHolder(
                    inflater(LayoutInflater.from(parent.context), parent, viewType)
                )
            }

            override fun onBindViewHolder(
                holder: DataBindingViewHolder<B>,
                position: Int,
            ) {
                onBindViewHolderWithExecution(holder, position)
                holder.binding.run {
                    if (lifecycleOwner == null) {
                        lifecycleOwner = viewLifecycleOwnerSupplier()
                    }
                    executePendingBindings()
                }
            }

            override fun getItemViewType(position: Int): Int {
                return currentList[position].itemViewType()
            }
        }.apply {
            submitList(listOf())
            dataSource.run {
                submitList(value)
                observe(viewLifecycleOwnerSupplier(), ::submitList)
            }
        }
    }
}
