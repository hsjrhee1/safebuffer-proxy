package com.safebuffer.app.ui.transcript

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safebuffer.app.R
import com.safebuffer.app.data.model.TranscriptSegment
import java.text.SimpleDateFormat
import java.util.*

class TranscriptAdapter(
    private val onSegmentTapped: (Int) -> Unit
) : ListAdapter<TranscriptSegment, TranscriptAdapter.VH>(DIFF) {

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView  = view.findViewById(R.id.tvSegmentTime)
        val tvText: TextView  = view.findViewById(R.id.tvSegmentText)
        val bubble: View      = view.findViewById(R.id.bubbleBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transcript_segment, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val seg = getItem(position)

        holder.tvTime.text = timeFmt.format(Date(seg.absoluteStartMs))
        holder.tvText.text = seg.text

        // 선택 구간 강조
        if (seg.isSelected) {
            holder.bubble.setBackgroundResource(R.drawable.bg_segment_selected)
            holder.tvTime.setTextColor(0xFF_FF_FF_FF.toInt())
            holder.tvText.setTextColor(0xFF_FF_FF_FF.toInt())
        } else {
            holder.bubble.setBackgroundResource(R.drawable.bg_segment_normal)
            holder.tvTime.setTextColor(0xFF_88_88_88.toInt())
            holder.tvText.setTextColor(0xFF_DD_DD_DD.toInt())
        }

        holder.itemView.setOnClickListener {
            onSegmentTapped(seg.id)
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<TranscriptSegment>() {
            override fun areItemsTheSame(a: TranscriptSegment, b: TranscriptSegment) = a.id == b.id
            override fun areContentsTheSame(a: TranscriptSegment, b: TranscriptSegment) = a == b
        }
    }
}
