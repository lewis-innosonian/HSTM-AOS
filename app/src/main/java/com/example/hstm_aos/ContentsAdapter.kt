package com.example.hstm_aos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ContentsAdapter(
    private val items: List<ContentsItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is ContentsItem.Header -> TYPE_HEADER
            is ContentsItem.Content -> TYPE_CONTENT
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(
                inflater.inflate(R.layout.item_contents_header, parent, false)
            )
            else -> ContentVH(
                inflater.inflate(R.layout.item_content, parent, false)
            )
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ContentsItem.Header -> (holder as HeaderVH).bind(item)
            is ContentsItem.Content -> (holder as ContentVH).bind(item)
        }
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {

        private val icon = view.findViewById<ImageView>(R.id.headerIcon)
        private val title = view.findViewById<TextView>(R.id.headerTitle)
        private val date = view.findViewById<TextView>(R.id.headerDate)

        fun bind(item: ContentsItem.Header) {
            icon.setImageResource(item.iconRes)
            title.text = item.title
            date.text = item.createdAt
        }
    }

    class ContentVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ContentsItem.Content) {
            itemView.findViewById<TextView>(R.id.contentText).text = item.text
        }
    }
}
