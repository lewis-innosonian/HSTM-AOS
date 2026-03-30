package com.example.hstm_aos.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.Fragment.GuideAndHelpFragment

import com.example.hstm_aos.Fragment.WebViewPopupFragment
import com.example.hstm_aos.R
import com.example.hstm_aos.customview.RoundedLinearLayout

class GuideMenuAdapter(
    private val list: List<GuideAndHelpFragment.GuideMenuItem>,
    private val click: (Int) -> Unit
) : RecyclerView.Adapter<GuideMenuAdapter.VH>() {

    private var selectedPos = 0

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvTitle)
        val icon: ImageView = v.findViewById(R.id.ivType)
        val layout: RoundedLinearLayout = v.findViewById(R.id.titleLayout)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                val item = list[pos]

                if (item.type == GuideAndHelpFragment.MenuType.WEB) {
                    val fragmentManager =
                        (itemView.context as androidx.fragment.app.FragmentActivity).supportFragmentManager
                    WebViewPopupFragment.newInstance(item.url ?: "")
                        .show(fragmentManager, "web_popup")
                    return@setOnClickListener
                }

                // VIDEO / TEXT 클릭 시 기존 동작 유지
                val prev = selectedPos
                selectedPos = pos
                notifyItemChanged(prev)
                notifyItemChanged(pos)

                click(pos)
            }
        }



    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return VH(v)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val item = list[i]

        h.title.text = item.title

        // 타입별 아이콘
        val iconRes = when (item.type) {
            GuideAndHelpFragment.MenuType.VIDEO -> R.drawable.inno_type_video_icon
            GuideAndHelpFragment.MenuType.TEXT -> R.drawable.inno_type_pdf_icon
            GuideAndHelpFragment.MenuType.WEB -> R.drawable.inno_type_outside_icon
        }
        h.icon.setImageResource(iconRes)

        // WEB 타입이면 배경 변경하지 않음
        if (item.type != GuideAndHelpFragment.MenuType.WEB) {
            if (selectedPos == i) {
                h.layout.setBackgroundColorInt(Color.parseColor("#F5F5F5"))
            } else {
                h.layout.setBackgroundColorInt(Color.WHITE)
            }
        }
    }

}
