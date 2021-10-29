package com.example.android.partialscreenshot.main_fragment.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.partialscreenshot.database.ScreenshotItem

import com.example.android.partialscreenshot.databinding.ListItemPictureBinding
import com.example.android.partialscreenshot.main_fragment.MainFragmentViewModel


class ScreenshotsAdapter(
    private val clickListener: ScreenshotListener,
    private val mainFragmentViewModel: MainFragmentViewModel
) : ListAdapter<ScreenshotItem,
        ScreenshotsAdapter.ViewHolder>(ScreenshotAdapterDiffCallback()) {
    var tracker: SelectionTracker<String>? = null

    init {
        setHasStableIds(true)
    }
    override fun getItemId(position: Int): Long {
        return getItem(position).screenshotID
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
    tracker?.let {
        holder.bind(clickListener, item,it.isSelected(item.storeUri), mainFragmentViewModel)
    }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, this)
    }


     class ViewHolder private constructor(private val binding: ListItemPictureBinding, private val adapter: ScreenshotsAdapter)
        : RecyclerView.ViewHolder(binding.root) {

         fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
             object : ItemDetailsLookup.ItemDetails<String>() {
                 override fun getPosition(): Int = adapterPosition
                 override fun getSelectionKey(): String = adapter.getItem(adapterPosition).storeUri
             }
        fun bind(
            clickListener: ScreenshotListener,
            item: ScreenshotItem,
            isActivated: Boolean = false,
            mainFragmentViewModel: MainFragmentViewModel
        ) {
            binding.screenshot = item
            binding.clickListener = clickListener
            binding.executePendingBindings()

                binding.selectedItem.isVisible = isActivated
                itemView.isSelected = isActivated



        }

        companion object {
            fun from(parent: ViewGroup, screenshotsAdapter: ScreenshotsAdapter): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemPictureBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding,screenshotsAdapter)
            }
        }
    }
}

/**
 * Callback for calculating the diff between two non-null items in a list.
 *
 * Used by ListAdapter to calculate the minumum number of changes between and old list and a new
 * list that's been passed to `submitList`.
 */
class ScreenshotAdapterDiffCallback : DiffUtil.ItemCallback<ScreenshotItem>() {
    override fun areItemsTheSame(oldItem: ScreenshotItem, newItem: ScreenshotItem): Boolean {
        return oldItem.storeUri == newItem.storeUri
    }

    override fun areContentsTheSame(oldItem: ScreenshotItem, newItem: ScreenshotItem): Boolean {
        return oldItem == newItem
    }
}

class ScreenshotListener(val clickListener: (sleepId: Long) -> Unit){
    fun onClick(screenshot: ScreenshotItem) = clickListener(screenshot.screenshotID)
}

class MyItemDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<String>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<String>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as ScreenshotsAdapter.ViewHolder).getItemDetails()
        }
        return null
    }
}

class MyItemKeyProvider(private val adapter: ScreenshotsAdapter) : ItemKeyProvider<String>(SCOPE_CACHED)
{
    override fun getKey(position: Int): String? =
        adapter.currentList[position].storeUri
    override fun getPosition(key: String): Int =
        adapter.currentList.indexOfFirst {it.storeUri == key}
}


