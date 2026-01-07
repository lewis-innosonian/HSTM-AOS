package com.example.hstm_aos

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderDecoration(
    private val adapter: ContentsAdapter
) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val topChild = parent.getChildAt(0) ?: return
        val position = parent.getChildAdapterPosition(topChild)
        if (position == RecyclerView.NO_POSITION) return

        val headerPos = findHeaderPosition(position)
        val headerView = createHeaderView(parent, headerPos)

        val contactPoint = headerView.bottom
        val childInContact = getChildInContact(parent, contactPoint)

        if (childInContact != null &&
            adapter.getItemViewType(parent.getChildAdapterPosition(childInContact)) == 0
        ) {
            c.save()
            c.translate(0f, (childInContact.top - headerView.height).toFloat())
            headerView.draw(c)
            c.restore()
        } else {
            headerView.draw(c)
        }
    }

    private fun findHeaderPosition(from: Int): Int {
        for (i in from downTo 0) {
            if (adapter.getItemViewType(i) == 0) return i
        }
        return 0
    }

    private fun createHeaderView(parent: RecyclerView, position: Int): View {
        val vh = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(vh, position)
        val view = vh.itemView
        view.measure(
            View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        return view
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.top <= contactPoint && child.bottom >= contactPoint) {
                return child
            }
        }
        return null
    }
}
