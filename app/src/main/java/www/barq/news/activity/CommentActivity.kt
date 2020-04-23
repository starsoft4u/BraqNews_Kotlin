package www.barq.news.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.kennyc.view.MultiStateView
import com.kennyc.view.MultiStateView.VIEW_STATE_CONTENT
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.cell_comment_header.view.*
import kotlinx.android.synthetic.main.state_empty.*
import org.greenrobot.eventbus.EventBus
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.adapter.CommentAdapter
import www.barq.news.extensions.*
import www.barq.news.model.Comment
import www.barq.news.model.News

class CommentActivity : BaseActivity() {
    private lateinit var news: News
    private var tableData = arrayListOf<Comment>()
    private var headerView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        setTitleCenter("")
        news = Klaxon().parse<News>(intent.getStringExtra("news")!!)!!

        setupStateView()
        setupTableView()

        sendButton.setOnClickListener {
            if (!messageBox.validate({ it.isNotBlank() }, R.string.comment_empty)) {
                messageBox.requestFocus()
            } else {
                comment(messageBox.text.toString())
            }
        }

        loadData()
    }

    private fun setupStateView() {
        emptyStateImage.setImageResource(R.drawable.empty_comment)
        emptyStateTitle.visibility = GONE
        emptyStateDescription.setText(R.string.be_the_first_who_comment_on_this_news)
        emptyStateButton.visibility = GONE
    }

    private fun setupTableView() {
        tableView.apply {
            setPullRefreshEnabled(false)
            setLoadingMoreEnabled(false)
            layoutManager = LinearLayoutManager(this@CommentActivity, RecyclerView.VERTICAL, false)
            adapter = CommentAdapter(
                this@CommentActivity, tableData,
                onLikeAction = onLikeAction,
                onDislikeAction = onDislikeAction,
                onFlagAction = onFlagAction
            )
        }
    }

    private val onLikeAction: (Int) -> Unit = {
        val comment = tableData[it]
        val userId = Defaults.userId
        when {
            !isLoggedIn -> message(R.string.you_have_to_login_or_signup)
            comment.userId == userId -> message(R.string.you_cannot_do_this_action_at_your_comment)
            comment.likedArray.contains(userId) -> voteLike(it, 0)
            comment.dislikedArray.contains(userId) -> voteDisLike(it, 0)
            else -> voteLike(it, 1)
        }
    }

    private val onDislikeAction: (Int) -> Unit = {
        val comment = tableData[it]
        val userId = Defaults.userId
        when {
            !isLoggedIn -> message(R.string.you_have_to_login_or_signup)
            comment.userId == userId -> message(R.string.you_cannot_do_this_action_at_your_comment)
            comment.dislikedArray.contains(userId) -> voteDisLike(it, 0)
            comment.likedArray.contains(userId) -> voteLike(it, 0)
            else -> voteDisLike(it, 1)
        }
    }

    private val onFlagAction: (Int) -> Unit = {
        val comment = tableData[it]
        val userId = Defaults.userId
        when {
            !isLoggedIn -> message(R.string.you_have_to_login_or_signup)
            comment.userId == userId -> message(R.string.you_cannot_do_this_action_at_your_comment)
            comment.flaggedArray.contains(userId) -> voteFlag(it, 0)
            else -> voteFlag(it, 1)
        }
    }

    private fun voteLike(index: Int, check: Int) {
        val comment = tableData[index]
        get("/vote_comment/id/${comment.id}/type/liked/checked/$check") {
            tableData[index].liked = tableData[index].likedArray.toMutableList().apply {
                if (check == 1) add(Defaults.userId) else remove(Defaults.userId)
            }.joinToString(",")
            tableView.notifyItemChanged(index)
        }
    }

    private fun voteDisLike(index: Int, check: Int) {
        val comment = tableData[index]
        get("/vote_comment/id/${comment.id}/type/disliked/checked/$check") {
            tableData[index].disliked = tableData[index].dislikedArray.toMutableList().apply {
                if (check == 1) add(Defaults.userId) else remove(Defaults.userId)
            }.joinToString(",")
            tableView.notifyItemChanged(index)
        }
    }

    private fun voteFlag(index: Int, check: Int) {
        val comment = tableData[index]
        get("/vote_comment/id/${comment.id}/type/flagged/checked/$check") {
            tableData[index].flagged = tableData[index].flaggedArray.toMutableList().apply {
                if (check == 1) add(Defaults.userId) else remove(Defaults.userId)
            }.joinToString(",")
            tableView.notifyItemChanged(index)
        }
    }

    private fun comment(message: String) {
        val params = listOf(
            "newsId" to news.id,
            "newsTitle" to news.title!!,
            "sourceId" to news.source.id,
            "comment" to message
        )
        post("/comments", params = params) { res ->
            val comment = Klaxon().parseFromJsonObject<Comment>(res.obj("data")!!)!!
            tableData.add(comment)

            refreshTableView()

            messageBox.text.clear()
            dismissKeyboard()

            EventBus.getDefault().post(Events.Commented(news.id, tableData.count()))
        }
    }

    private fun loadData() {
        get("/comments/newsId/${news.id}") { res ->
            val comments: List<Comment> =
                res.array<JsonObject>("data").orEmpty().map { Klaxon().parseFromJsonObject<Comment>(it)!! }

            tableData.clear()
            tableData.addAll(comments)

            refreshTableView()
        }
    }

    private fun refreshTableView() {
        tableView.adapter?.notifyDataSetChanged()

        if (tableData.isEmpty()) {
            stateView.viewState = MultiStateView.VIEW_STATE_EMPTY
        } else {
            if (headerView == null) {
                tableView.addHeaderView(
                    layoutInflater.inflate(
                        R.layout.cell_comment_header,
                        tableView,
                        false
                    ).apply {
                        Picasso.get().load(news.source.imageUrl).placeholder(R.drawable.empty).into(sourceImage)
                        sourceName.text = news.source.name
                        timeStamp.text = news.time.formattedString(this@CommentActivity)
                        newsTitle.text = news.title
                    }.also {
                        headerView = it
                    })
            }
            stateView.viewState = VIEW_STATE_CONTENT
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.comment, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menuRefresh -> loadData()
        }
        return true
    }

}
