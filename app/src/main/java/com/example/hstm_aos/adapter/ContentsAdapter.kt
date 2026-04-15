package com.example.hstm_aos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.ContentsItem
import com.example.hstm_aos.R
import com.example.hstm_aos.TrainingStatus

class ContentsAdapter(
    private val items: MutableList<ContentsItem.Content>,
    private val onStartClick: (ContentsItem.Content) -> Unit
) : RecyclerView.Adapter<ContentsAdapter.ContentVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content, parent, false)
        return ContentVH(view, onStartClick)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ContentVH, position: Int) {
        holder.bind(items[position])
    }

    fun updateItems(newItems: List<ContentsItem.Content>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ContentVH(
        view: View,
        private val onStartClick: (ContentsItem.Content) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val statusIcon = view.findViewById<ImageView>(R.id.trainingStatusImageView)
        private val text = view.findViewById<TextView>(R.id.contentText)
        private val rootLayout  = view.findViewById<RelativeLayout>(R.id.rootLayout)
        private val durationTextView = view.findViewById<TextView>(R.id.durationTextView)

        fun bind(item: ContentsItem.Content) {
            text.text = item.text
            durationTextView.text = item.duration

            when (item.status) {
                TrainingStatus.LOCKED -> {
                    rootLayout.setOnClickListener(null)
                    statusIcon.setImageResource(R.drawable.inno_training_lock_icon)
                    text.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black4)
                    )
                    durationTextView.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black4)
                    )
                }

                TrainingStatus.AVAILABLE -> {
                    rootLayout.setOnClickListener {
                        onStartClick(item)
                    }
                    statusIcon.setImageResource(R.drawable.inno_uncheck_icon)
                    text.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black2)
                    )
                    durationTextView.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black2)
                    )
                }

                TrainingStatus.COMPLETED -> {
                    rootLayout.setOnClickListener {
                        onStartClick(item)
                    }
                    statusIcon.setImageResource(R.drawable.inno_check_icon)
                    text.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black2)
                    )
                    durationTextView.setTextColor(
                        ContextCompat.getColor(text.context, R.color.black2)
                    )
                }
            }
        }
    }
}