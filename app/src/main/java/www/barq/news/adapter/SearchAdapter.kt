package www.barq.news.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.cell_category.view.*
import www.barq.news.R
import www.barq.news.activity.NewsActivity
import www.barq.news.activity.NewsType
import www.barq.news.extensions.navigate
import www.barq.news.model.Source

class SearchAdapter(
    private val context: Context,
    private val items: List<Source>
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(inflater.inflate(R.layout.cell_category, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.apply {
            Picasso.get().load(item.imageUrl).placeholder(R.drawable.empty).into(categoryIcon)
            categoryName.text = item.name
            setOnClickListener {
                context.navigate(
                    NewsActivity::class.java,
                    "type" to NewsType.SEARCH.toString(),
                    "source" to item
                )
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}