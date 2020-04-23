package www.barq.news.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.cell_ads.view.*
import kotlinx.android.synthetic.main.cell_news.view.*
import www.barq.news.R
import www.barq.news.activity.CommentActivity
import www.barq.news.activity.NewsDetailActivity
import www.barq.news.extensions.formattedString
import www.barq.news.extensions.navigate
import www.barq.news.extensions.share
import www.barq.news.model.News

class NewsAdapter(
    private val context: Context,
    private val items: List<Any>
) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is News) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == 0) {
            ViewHolder(inflater.inflate(R.layout.cell_news, parent, false))
        } else {
            ViewHolder(inflater.inflate(R.layout.cell_ads, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is News -> {
                holder.itemView.apply {
                    Picasso.get().load(item.source.imageUrl).placeholder(R.drawable.empty).into(sourceImage)
                    Picasso.get().load(item.imageUrl).placeholder(R.drawable.empty).into(newsImage)

                    ConstraintSet().apply {
                        clone(newsContainer)
                        setDimensionRatio(newsImage.id, "1:${item.imageRatio ?: 0}")
                        applyTo(newsContainer)
                    }

                    sourceName.text = item.source.name
                    timeStamp.text = item.time.formattedString(context)
                    newsTitle.text = item.title
                    comment.text = item.comment.toString()
                    shareButton.setOnClickListener {
                        context.share(
                            url = item.url,
                            content = context.getString(R.string.share_news),
                            title = item.title
                        )
                    }
                    commentButton.setOnClickListener {
                        context.navigate(CommentActivity::class.java, "news" to item)
                    }
                    setOnClickListener {
                        context.navigate(NewsDetailActivity::class.java, "news" to item)
                    }
                }
            }
            is AdView -> {
                holder.itemView.apply {
                    if (adViewContainer.childCount > 0) {
                        adViewContainer.removeAllViews()
                    }
                    if (item.parent != null) {
                        (item.parent as? ViewGroup)?.removeView(item)
                    }
                    adViewContainer.addView(item)
                }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}