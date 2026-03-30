package com.example.hstm_aos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.R
import com.example.hstm_aos.ble.BleDevice

class DeviceAdapter(
    private val items: MutableList<BleDevice>,
    private val onClick: (BleDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    private var connectionMap: Map<String, Boolean> = emptyMap()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.deviceNameText)
        val state: TextView = v.findViewById(R.id.deviceState)
        val connectImageView: ImageView = v.findViewById(R.id.connectImageView)
        val progressBar: ProgressBar = v.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_device, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val d = items[i]

        h.name.text = d.device.name ?: d.device.address

        h.progressBar.visibility = View.INVISIBLE
        val connected = connectionMap[d.device.address] ?: d.isConnected
        h.state.text =
            if (connected) d.firmwareVersion?.let { "v.$it" } ?: "" else ""
        h.connectImageView.visibility = if (connected) View.VISIBLE else View.INVISIBLE
        h.itemView.setOnClickListener {
            h.progressBar.visibility = View.VISIBLE
            onClick(d)
        }
    }

    fun submitList(list: List<BleDevice>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun updateConnectionState(stateMap: Map<String, Boolean>) {
        connectionMap = stateMap
        notifyDataSetChanged()
    }

    fun updateVersion(address: String, version: String) {
        val index = items.indexOfFirst { it.device.address == address }
        if (index == -1) return

        items[index].firmwareVersion = version
        notifyItemChanged(index)
    }

}
