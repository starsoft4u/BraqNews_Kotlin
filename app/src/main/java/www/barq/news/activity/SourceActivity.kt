package www.barq.news.activity

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.jcodecraeer.xrecyclerview.XRecyclerView
import com.kennyc.view.MultiStateView.VIEW_STATE_CONTENT
import com.kennyc.view.MultiStateView.VIEW_STATE_LOADING
import kotlinx.android.synthetic.main.layout_tableview.*
import kotlinx.android.synthetic.main.state_empty.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.adapter.SourceAdapter
import www.barq.news.custom.XRecyclerViewHeader
import www.barq.news.extensions.get
import www.barq.news.extensions.isLoggedIn
import www.barq.news.extensions.navigate
import www.barq.news.extensions.setTitleCenter
import www.barq.news.model.Category
import www.barq.news.model.Source

class SourceActivity : BaseActivity() {
    private var tableData = arrayListOf<Source>()

    private lateinit var category: Category

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_tableview)

        category = Klaxon().parse(intent.getStringExtra("category")!!)!!
        setTitleCenter(category.name)

        tableView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SourceActivity, RecyclerView.VERTICAL, false)
            adapter = SourceAdapter(this@SourceActivity, tableData) {
                context.navigate(
                    NewsActivity::class.java,
                    "type" to NewsType.NEWSPAPER.toString(),
                    "source" to tableData[it]
                )
            }
            setRefreshHeader(XRecyclerViewHeader(this@SourceActivity))
            setLoadingMoreEnabled(false)
            setLoadingListener(object : XRecyclerView.LoadingListener {
                override fun onLoadMore() {
                    //
                }

                override fun onRefresh() {
                    loadData(indicator = false)
                }
            })
        }

        loadData(indicator = true)

        // EventBus
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private fun loadData(indicator: Boolean) {
        get("/sources/category_id/${category.id}", indicator = indicator) { res ->
            val klaxon = Klaxon()
            val sources = res.array<JsonObject>("data").orEmpty().map {
                klaxon.parseFromJsonObject<Source>(it)!!
            }

            tableData.clear()
            tableData.addAll(sources)
            if (!isLoggedIn) {
                val mySources = Defaults.sources
                tableData.forEach { it.isMySource = mySources.contains(it.id) }
            }

            tableView.adapter?.notifyDataSetChanged()
            tableView.refreshComplete()
            stateView.viewState = if (tableData.isEmpty()) VIEW_STATE_LOADING else VIEW_STATE_CONTENT
        }
    }

    @Subscribe
    public fun onFollowChanged(event: Events.Followed) {
        val index = tableData.indexOfFirst { it.id == event.sourceId }
        if (index >= 0) {
            tableData[index].apply {
                followerCount += (if (event.follow) 1 else -1)
                isMySource = event.follow
            }
            tableView.notifyItemChanged(index)
        }
    }
}
