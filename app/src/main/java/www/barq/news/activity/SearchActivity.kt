package www.barq.news.activity

import android.os.Bundle
import android.view.View.GONE
import android.view.View.LAYOUT_DIRECTION_RTL
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.jcodecraeer.xrecyclerview.XRecyclerView
import com.kennyc.view.MultiStateView.VIEW_STATE_CONTENT
import com.kennyc.view.MultiStateView.VIEW_STATE_EMPTY
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.state_empty.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.adapter.SearchAdapter
import www.barq.news.custom.XRecyclerViewHeader
import www.barq.news.extensions.dismissKeyboard
import www.barq.news.extensions.get
import www.barq.news.extensions.setTitleCenter
import www.barq.news.model.Source

class SearchActivity : BaseActivity() {
    private var tableData = arrayListOf<Source>()
    private var filter = arrayListOf<Source>()

    private var query: String = ""
    private var fromMySource = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setTitleCenter(getString(R.string.search))
        supportActionBar?.elevation = 0f

        // Segment
        optionMySource.setOnClickListener {
            fromMySource = true
            doSearch()
        }
        optionAllSource.setOnClickListener {
            fromMySource = false
            doSearch()
        }

        // Setup
        setupTableView()
        setupStateView()
        setupSearchView()

        loadData()

        // EventBus
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        dismissKeyboard()
        super.onPause()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private fun setupSearchView() {
        searchView.apply {
            val edit = findViewById<EditText>(R.id.search_src_text)
            edit.layoutDirection = LAYOUT_DIRECTION_RTL
            findViewById<LinearLayout>(R.id.search_edit_frame).layoutDirection = LAYOUT_DIRECTION_RTL
            // Dismiss keyboard when X button clicked
//            findViewById<ImageView>(R.id.search_close_btn).setOnClickListener {
//                edit.text.clear()
//                setQuery("", false)
//                dismissKeyboard()
//            }

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    dismissKeyboard()
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    this@SearchActivity.query = newText ?: ""
                    doSearch()
                    return true
                }
            })
        }
    }

    private fun setupTableView() {
        tableView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SearchActivity, RecyclerView.VERTICAL, false)
            adapter = SearchAdapter(this@SearchActivity, filter)
            setRefreshHeader(XRecyclerViewHeader(this@SearchActivity))
            setLoadingMoreEnabled(false)
            setLoadingListener(object : XRecyclerView.LoadingListener {
                override fun onLoadMore() {
                    //
                }

                override fun onRefresh() {
                    loadData()
                }
            })
        }
    }

    private fun setupStateView() {
        emptyStateTitle.setText(R.string.no_result_found)
        emptyStateDescription.setText(R.string.please_expand_your_search_to_find_more_results)
        emptyStateButton.visibility = GONE
    }

    private fun loadData() {
        get("/search") { res ->
            val klaxon = Klaxon()
            val sources = res.array<JsonObject>("data").orEmpty().map {
                klaxon.parseFromJsonObject<Source>(it)!!
            }
            tableData.clear()
            tableData.addAll(sources)

            doSearch()
        }
    }

    private fun doSearch() {
        filter.clear()
        filter.addAll(when {
            query.isBlank() -> arrayListOf()
            fromMySource -> tableData.filter {
                it.isMySource
                        && (it.name!!.toLowerCase().contains(query.toLowerCase())
                        || it.account!!.toLowerCase().contains(query.toLowerCase()))
            }
            else -> tableData.filter {
                it.name!!.toLowerCase().contains(query.toLowerCase())
                        || it.account!!.toLowerCase().contains(query.toLowerCase())
            }
        })
        tableView.adapter?.notifyDataSetChanged()
        stateView.viewState = if (filter.isEmpty()) VIEW_STATE_EMPTY else VIEW_STATE_CONTENT
    }

    @Subscribe
    public fun onFavoriteChanged(event: Events.Followed) {
        tableData.firstOrNull { it.id == event.sourceId }?.isMySource = event.follow
        filter.firstOrNull { it.id == event.sourceId }?.isMySource = event.follow
    }
}
