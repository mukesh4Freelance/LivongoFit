package com.jinxtris.ram.livongofit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.jinxtris.ram.livongofit.R
import com.jinxtris.ram.livongofit.model.StepItem
import kotlinx.android.synthetic.main.adapter_step_item.view.*


class StepItemAdapter(private val stepList: ArrayList<StepItem>) :
    RecyclerView.Adapter<StepItemAdapter.ViewHolder>() {
    //private var stepList = ArrayList<StepItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.adapter_step_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return stepList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.txtStepCount.text = stepList[position].totalCount.toString()
        holder.txtStepDate.text = stepList[position].strDate
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtStepCount: AppCompatTextView = view.txtAdapterStepValue
        val txtStepDate: AppCompatTextView = view.txtAdapterDateValue
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}
