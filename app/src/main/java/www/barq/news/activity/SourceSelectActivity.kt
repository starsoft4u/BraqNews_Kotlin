package www.barq.news.activity

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.kennyc.view.MultiStateView.*
import kotlinx.android.synthetic.main.layout_tableview.*
import kotlinx.android.synthetic.main.state_empty.*
import org.greenrobot.eventbus.EventBus
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.adapter.SourceSelectAdapter
import www.barq.news.extensions.get
import www.barq.news.extensions.isLoggedIn
import www.barq.news.extensions.setTitleCenter
import www.barq.news.extensions.subscribeNotification
import www.barq.news.model.Source

class SourceSelectActivity : BaseActivity() {
    private var tableData = arrayListOf<Any>()

    private var channel = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_tableview)

        setTitleCenter(intent.getStringExtra("title"))
        channel = intent.getIntExtra("channel", 0)

        tableView.apply {
            setHasFixedSize(true)
            setPullRefreshEnabled(false)
            setLoadingMoreEnabled(false)
            layoutManager = LinearLayoutManager(this@SourceSelectActivity, RecyclerView.VERTICAL, false)
            adapter = SourceSelectAdapter(this@SourceSelectActivity, tableData, onButtonAction)
        }

        if (channel == 0) {
            emptyStateButton.visibility = GONE
        }

        loadData()
    }

    private val onButtonAction: (Int, View) -> Unit = { index, view ->
        val source = tableData[index] as Source
        val checked = source.isMySource

        if (isLoggedIn) {
            get("/register_source/id/${source.id}/register/${if (checked) 0 else 1}") {
                handleSelect(view, index, !checked)
            }
        } else {
            handleSelect(view, index, !checked)
        }
    }

    private fun handleSelect(view: View, index: Int, follow: Boolean) {
        val source = tableData[index] as Source

        (tableData[index] as Source).isMySource = follow
        view.visibility = if (follow) VISIBLE else GONE

        // update notification
        if (follow) {
            Defaults.sources = Defaults.sources.apply { add(source.id) }
            Defaults.notificaitons = Defaults.notificaitons.apply { add(source.id) }
            subscribeNotification(source.id.toString(), true)
        } else {
            Defaults.sources = Defaults.sources.apply { remove(source.id) }
            Defaults.notificaitons = Defaults.notificaitons.apply { remove(source.id) }
            subscribeNotification(source.id.toString(), false)
        }

        EventBus.getDefault().post(Events.SourceModified())
    }

    private fun loadData() {
        if (channel == 0 && !isLoggedIn && Defaults.sources.isEmpty()) {
            stateView.viewState = VIEW_STATE_EMPTY
            return
        }

        val url = when {
            channel > 0 -> "/all_sources/channel/$channel"
            isLoggedIn -> "/my_sources"
            else -> "/my_sources/source_ids/${Defaults.sourcesRaw}"
        }
        get(url) { res ->
            val klaxon = Klaxon()
            val mySources = Defaults.sources
            res.array<JsonObject>("data").orEmpty().forEach { json ->
                tableData.add(json.string("name")!!)
                tableData.addAll(json.array<JsonObject>("sources").orEmpty().map {
                    val item = klaxon.parseFromJsonObject<Source>(it)!!
                    if (!isLoggedIn) {
                        item.isMySource = mySources.contains(item.id)
                    }
                    item
                })
            }
            tableView.adapter?.notifyDataSetChanged()
            stateView.viewState = when {
                tableData.isNotEmpty() -> VIEW_STATE_CONTENT
                channel > 0 -> VIEW_STATE_LOADING
                else -> VIEW_STATE_EMPTY
            }
        }
    }
}
