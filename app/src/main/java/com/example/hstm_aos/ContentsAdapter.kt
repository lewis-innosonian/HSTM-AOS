package com.example.hstm_aos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ContentsAdapter(
    private val items: MutableList<ContentsItem>,
    private val onStartClick: (ContentsItem.Content) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is ContentsItem.Header) TYPE_HEADER else TYPE_CONTENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_contents_header, parent, false))
        } else {
            ContentVH(inflater.inflate(R.layout.item_content, parent, false), onStartClick)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ContentsItem.Header -> (holder as HeaderVH).bind(item)
            is ContentsItem.Content -> (holder as ContentVH).bind(item)
        }
    }

    fun updateItems(newItems: List<ContentsItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ContentsItem.Header) {
            itemView.findViewById<ImageView>(R.id.headerIcon).setImageResource(item.iconRes)
            itemView.findViewById<TextView>(R.id.headerTitle).text = item.title
            itemView.findViewById<TextView>(R.id.headerDate).text = item.createdAt
        }
    }

    class ContentVH(
        view: View,
        private val onStartClick: (ContentsItem.Content) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val statusIcon = view.findViewById<ImageView>(R.id.trainingStatusImageView)
        private val text = view.findViewById<TextView>(R.id.contentText)
        private val startLayout = view.findViewById<View>(R.id.startLayout)

        fun bind(item: ContentsItem.Content) {
            text.text = item.text

            when (item.status) {
                TrainingStatus.LOCKED -> {
                    statusIcon.setImageResource(R.drawable.inno_training_lock_icon)
                    startLayout.visibility = View.GONE
                    text.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black4))
                }

                TrainingStatus.AVAILABLE -> {
                    statusIcon.setImageResource(R.drawable.inno_uncheck_icon)
                    startLayout.visibility = View.VISIBLE
                    text.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black2)
                    )
                }

                TrainingStatus.COMPLETED -> {
                    statusIcon.setImageResource(R.drawable.inno_check_icon)
                    startLayout.visibility = View.VISIBLE
                    text.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black2)
                    )
                }
            }

            startLayout.setOnClickListener {
                onStartClick(item)
            }
        }
    }
}
