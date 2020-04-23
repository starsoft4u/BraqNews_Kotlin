package www.barq.news.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.cell_category.view.*
import www.barq.news.R
import www.barq.news.model.Category

class CategoryAdapter(
    private val context: Context,
    private val items: List<Category>,
    private val onItemClicked: ((Int) -> Unit)
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

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
            when {
                item.id > 0 -> Picasso.get().load(item.iconUrl).placeholder(R.drawable.empty).into(categoryIcon)
                item.channelId == 0 -> categoryIcon.setImageResource(R.drawable.ic_channel0)
                item.channelId == 1 -> categoryIcon.setImageResource(R.drawable.ic_channel1)
                item.channelId == 2 -> categoryIcon.setImageResource(R.drawable.ic_channel2)
                item.channelId == 3 -> categoryIcon.setImageResource(R.drawable.ic_channel3)
                item.channelId == 4 -> categoryIcon.setImageResource(R.drawable.ic_channel4)
                item.channelId == 5 -> categoryIcon.setImageResource(R.drawable.ic_channel5)
                item.channelId == 6 -> categoryIcon.setImageResource(R.drawable.ic_channel6)
            }
            categoryName.text = item.name
            setOnClickListener { onItemClicked.invoke(position) }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}