package www.barq.news.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.cell_source_select.view.*
import kotlinx.android.synthetic.main.cell_source_section.view.*
import www.barq.news.R
import www.barq.news.model.Source

class SourceSelectAdapter(
    private val context: Context,
    private val items: List<Any>,
    private val onButtonAction: ((Int, View) -> Unit)
) : RecyclerView.Adapter<SourceSelectAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is Source) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == 0) {
            ViewHolder(inflater.inflate(R.layout.cell_source_select, parent, false))
        } else {
            ViewHolder(inflater.inflate(R.layout.cell_source_section, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Source -> {
                holder.itemView.apply {
                    Picasso.get().load(item.imageUrl).placeholder(R.drawable.empty).into(sourceImage)
                    sourceName.text = item.name
                    if (item.followerCount == 1) {
                        followers.setText(R.string._follower)
                    } else {
                        followers.text = context.getString(R.string._followers, item.followerCount)
                    }
                    checkButton.visibility = if (item.isMySource) VISIBLE else GONE
                    checkButton.setOnClickListener { onButtonAction.invoke(position, checkButton) }
                    addButton.setOnClickListener { onButtonAction.invoke(position, checkButton) }
                }
            }
            is String -> {
                holder.itemView.categoryName.text = item
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}