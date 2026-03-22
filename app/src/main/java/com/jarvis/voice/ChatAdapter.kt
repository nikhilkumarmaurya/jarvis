package com.jarvis.voice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = mutableListOf<ChatMessage>()
    companion object { const val TYPE_USER = 0; const val TYPE_JARVIS=1 }
    fun addMessage(m: ChatMessage) { messages.add(m); notifyItemInserted(messages.size-1) }
    fun getHistory() = messages.toList()
    override fun getItemViewType(p: Int) = if(messages[p].role=="user") TYPE_USER else TYPE_JARVIS
    override fun onCreateViewHolder(parent: ViewGroup, t: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if(t==TYPE_USER) UserVH(inf.inflate(R.layout.item_bubble_user,parent,false)) else JarvisVH(inf.inflate(R.layout.item_bubble_jarvis,parent,false))
    }
    override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
        when(h){ is UserVH -> h.tv.text=messages[p].text; is JarvisVH -> h.tv.text=messages[p].text }
    }
    override fun getItemCount() = messages.size
    inner class UserVH(view: View):RecyclerView.ViewHolder(view){ val tv:TextView-findViewById(R.id.tvMessage) }
    inner class JarvisVH(view: View):RecyclerView.ViewHolder(view){ val tv:TextView=findViewById(R.id.tvMessage) }
}
