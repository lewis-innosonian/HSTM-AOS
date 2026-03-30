package com.example.hstm_aos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.R
import com.example.hstm_aos.model.Organization

class OrganizationAdapter(
    private val orgs: List<Organization>
) : RecyclerView.Adapter<OrganizationAdapter.ViewHolder>() {

    var selectedPosition = 0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radio: RadioButton = view.findViewById(R.id.radio)
        val name: TextView = view.findViewById(R.id.orgName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_org_radio, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = orgs.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val org = orgs[position]

        holder.name.text = org.org_name ?: "Organization"

        holder.radio.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
        }

        holder.radio.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
        }
    }

    fun getSelectedOrg(): Organization {
        return orgs[selectedPosition]
    }
}