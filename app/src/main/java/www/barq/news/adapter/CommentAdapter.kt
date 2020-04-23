package www.barq.news.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.cell_comment.view.*
import www.barq.news.Defaults
import www.barq.news.R
import www.barq.news.extensions.formattedString
import www.barq.news.model.Comment

class CommentAdapter(
    private val context: Context,
    private val items: List<Comment>,
    private val onLikeAction: ((Int) -> Unit)?,
    private val onDislikeAction: ((Int) -> Unit)?,
    private val onFlagAction: ((Int) -> Unit)?
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return if (items.isNotEmpty()) items.count() + 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == items.count()) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val layout = if (viewType == 0) R.layout.cell_comment else R.layout.comment_note
        return ViewHolder(inflater.inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= items.size) {
            return
        }

        val item = items[position]
        holder.itemView.apply {
            Picasso.get().load(item.userPhoto).placeholder(R.drawable.ic_user).into(userPhoto)
            userName.text = item.userName
            timeStamp.text = item.issuedAt.formattedString(context)
            comment.text = item.comment
            val userId = Defaults.userId
            like.text = item.likedArray.count().toString()
            likeButton.apply {
                setImageResource(if (item.likedArray.contains(userId)) R.drawable.ic_like_d else R.drawable.ic_like)
                setOnClickListener { onLikeAction?.invoke(position) }
            }
            dislike.text = item.dislikedArray.count().toString()
            dislikeButton.apply {
                setImageResource(if (item.dislikedArray.contains(userId)) R.drawable.ic_dislike_d else R.drawable.ic_dislike)
                setOnClickListener { onDislikeAction?.invoke(position) }
            }
            flag.text = item.flaggedArray.count().toString()
            flagButton.apply {
                setImageResource(if (item.flaggedArray.contains(userId)) R.drawable.ic_flag_d else R.drawable.ic_flag)
                setOnClickListener { onFlagAction?.invoke(position) }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}