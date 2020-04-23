package www.barq.news.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.jcodecraeer.xrecyclerview.XRecyclerView
import com.kennyc.view.MultiStateView.VIEW_STATE_CONTENT
import com.kennyc.view.MultiStateView.VIEW_STATE_EMPTY
import kotlinx.android.synthetic.main.layout_tableview.*
import kotlinx.android.synthetic.main.state_empty.*
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
import www.barq.news.model.Source

class MyNewsFragment : Fragment() {
    private var tableData = arrayListOf<Any>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_tableview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
            adapter = SourceAdapter(context!!, tableData) {
                val source = tableData[it] as Source
                val type = if (source.id == 0) NewsType.FAVORITE else NewsType.MY_NEWS
                context.navigate(
                    NewsActivity::class.java,
                    "type" to type.toString(),
                    "source" to source
                )
            }
            setRefreshHeader(XRecyclerViewHeader(context!!))
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

        emptyStateButton.setOnClickListener {
            context!!.navigate(EditSourceActivity::class.java)
        }

    }

    private fun loadData(indicator: Boolean) {
        when {
            !isLoggedIn && Defaults.sources.isEmpty() && Defaults.favorites.isEmpty() -> {
                tableData.clear()
                tableView.adapter?.notifyDataSetChanged()
                stateView.viewState = VIEW_STATE_EMPTY
            }
            isLoggedIn -> {
                context!!.get("/my_news", indicator = indicator) { res ->
                    val data = arrayListOf<Any>()
                    val klaxon = Klaxon()
                    res.array<JsonObject>("data").orEmpty().forEach { json ->
                        data.add(json.string("name")!!)
                        data.addAll(json.array<JsonObject>("sources").orEmpty().map {
                            klaxon.parseFromJsonObject<Source>(it)!!
                        })
                    }
                    handleTableData(data, false)
                }
            }
            Defaults.sources.isEmpty() -> {
                handleTableData(listOf(), true)
            }
            else -> {
                context!!.get("/sources/categorize/1/source_ids/${Defaults.sourcesRaw}", indicator = indicator) { res ->
                    val data = arrayListOf<Any>()
                    val klaxon = Klaxon()
                    val mySources = Defaults.sources
                    res.array<JsonObject>("data").orEmpty().forEach { json ->
                        data.add(json.string("name")!!)
                        data.addAll(json.array<JsonObject>("sources").orEmpty().map {
                            klaxon.parseFromJsonObject<Source>(it)!!.apply {
                                isMySource = mySources.contains(id)
                                if (isMySource) {
                                    followerCount += 1
                                }
                            }
                        })
                    }
                    handleTableData(data, true)
                }
            }
        }
    }

    private fun handleTableData(data: List<Any>, favorite: Boolean) {
        tableData.clear()
        if (favorite) {
            tableData.add(getString(R.string.my_favorite))
            tableData.add(
                Source(
                    name = getString(R.string.my_favorite_news),
                    followerCount = Defaults.favorites.size
                )
            )
        }
        tableData.addAll(data)

        tableView.adapter?.notifyDataSetChanged()
        tableView.refreshComplete()
        stateView.viewState = if (tableData.isEmpty()) VIEW_STATE_EMPTY else VIEW_STATE_CONTENT
    }

    @Subscribe
    public fun viewWillAppear(event: Events.SelectTab) {
        if (event.index == 3) {
            loadData(indicator = true)
        }
    }

    @Subscribe
    public fun onFollowChanged(event: Events.Followed) {
        if (MainActivity.selectedTab == 3) {
            loadData(indicator = true)
        }
    }

    @Subscribe
    fun onSourceModified(event: Events.SourceModified) {
        if (MainActivity.selectedTab == 3) {
            loadData(indicator = true)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        EventBus.getDefault().register(this)
    }

    override fun onDetach() {
        EventBus.getDefault().unregister(this)
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = MyNewsFragment()
    }
}
