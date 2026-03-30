package com.example.hstm_aos.adapter

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.R

class PdfPageAdapter(
    private val renderer: PdfRenderer
) : RecyclerView.Adapter<PdfPageAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.ivPage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return VH(v)
    }

    override fun getItemCount() = renderer.pageCount

    override fun onBindViewHolder(h: VH, i: Int) {

        val page = renderer.openPage(i)

        val bmp = Bitmap.createBitmap(
            page.width,
            page.height,
            Bitmap.Config.ARGB_8888
        )

        page.render(
            bmp,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        page.close()
        h.img.setImageBitmap(bmp)
    }
}
