package www.barq.news.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.cell_source.view.*
import kotlinx.android.synthetic.main.cell_source_section.view.*
import www.barq.news.R
import www.barq.news.model.Source

class SourceAdapter(
    private val context: Context,
    private val items: List<Any>,
    private val onItemClicked: ((Int) -> Unit)
) : RecyclerView.Adapter<SourceAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is Source) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == 0) {
            ViewHolder(inflater.inflate(R.layout.cell_source, parent, false))
        } else {
            ViewHolder(inflater.inflate(R.layout.cell_source_section, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Source -> {
                holder.itemView.apply {
                    if (item.id == 0) {
                        sourceImage.setImageResource(R.drawable.ic_favorite)
                        followers.text = context.getString(R.string.news_in_my_favorite, item.followerCount)
                    } else {
                        Picasso.get().load(item.imageUrl).placeholder(R.drawable.empty).into(sourceImage)
                        if (item.followerCount == 1) {
                            followers.setText(R.string._follower)
                        } else {
                            followers.text = context.getString(R.string._followers, item.followerCount)
                        }
                    }
                    sourceName.text = item.name
                    setOnClickListener { onItemClicked.invoke(position) }
                }
            }
            is String -> {
                holder.itemView.categoryName.text = item
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}