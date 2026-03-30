package com.example.hstm_aos.Fragment

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.adapter.GuideMenuAdapter
import com.example.hstm_aos.R
import com.example.hstm_aos.customview.RoundedLinearLayout
import kotlinx.android.parcel.Parcelize

class GuideAndHelpFragment : Fragment(R.layout.fragment_guide_help) {

    private var documentList: List<GuideMenuItem> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전달받은 문서 리스트
        documentList = arguments?.getSerializable("documents") as? List<GuideMenuItem> ?: emptyList()

        val rv = view.findViewById<RecyclerView>(R.id.rvMenu)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = GuideMenuAdapter(documentList) { pos ->
            openDocument(pos)
        }

        if (documentList.isNotEmpty()) {
            openDocument(0)
        }
    }

    private fun openDocument(pos: Int) {
        val item = documentList[pos]

        when (item.type) {
            MenuType.VIDEO -> {
                // VideoFragment 호출
                val f = VideoFragment.newInstance(item.url)
                childFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, f)
                    .commit()
            }
            MenuType.TEXT -> {
                // PdfFragment 호출
                val f = PdfFragment.newInstance(item.localFileName)
                childFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, f)
                    .commit()
            }
            MenuType.WEB -> {

            }
        }
    }





    // 데이터 클래스 및 enum
    @Parcelize
    data class GuideMenuItem(
        val title: String,
        val type: MenuType,
        val localFileName: String = "",
        val url: String = "",
        val fileStoreId: String = "",
        val fileStoreOrgNodeId: String = ""
    ) : Parcelable

    @Parcelize
    enum class MenuType : Parcelable {
        VIDEO, TEXT, WEB
    }
}
