package com.example.hstm_aos.Fragment

import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.adapter.PdfPageAdapter
import com.example.hstm_aos.R
import java.io.File

class PdfFragment : Fragment(R.layout.fragment_text) {

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    companion object {
        fun newInstance(fileName: String) =
            PdfFragment().apply {
                arguments = Bundle().apply {
                    putString("file", fileName)
                }
            }
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        val fileName = arguments?.getString("file") ?: return

        val file = File(requireContext().filesDir, fileName)

        if (!file.exists()) {

            android.util.Log.e("PdfFragment", "File not found: $fileName")
            return
        }

        openPdf(v, file)
    }

    private fun openPdf(v: View, file: File) {
        fileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

        renderer = PdfRenderer(fileDescriptor!!)

        val rv = v.findViewById<RecyclerView>(R.id.rvPdf)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = PdfPageAdapter(renderer!!)
    }

    override fun onDestroyView() {
        renderer?.close()
        fileDescriptor?.close()
        super.onDestroyView()
    }
}
