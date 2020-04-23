package www.barq.news.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.jcodecraeer.xrecyclerview.XRecyclerView
import com.kennyc.view.MultiStateView.VIEW_STATE_CONTENT
import com.kennyc.view.MultiStateView.VIEW_STATE_LOADING
import kotlinx.android.synthetic.main.layout_tableview.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.adapter.CategoryAdapter
import www.barq.news.custom.XRecyclerViewHeader
import www.barq.news.extensions.get
import www.barq.news.extensions.navigate
import www.barq.news.extensions.toPixel
import www.barq.news.model.Category

class NewspaperFragment : Fragment() {
    private var tableData = arrayListOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_tableview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableView.apply {
            setPadding(toPixel(12f).toInt())
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
            adapter = CategoryAdapter(context!!, tableData) {
                context.navigate(SourceActivity::class.java, "category" to tableData[it])
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
    }

    private fun loadData(indicator: Boolean) {
        context!!.get("/list_categories/channel/1", indicator = indicator) { res ->
            val json = res.array<JsonObject>("data").orEmpty()

            val klaxon = Klaxon()
            tableData.clear()
            tableData.addAll(json.map { klaxon.parseFromJsonObject<Category>(it)!! })

            tableView.adapter?.notifyDataSetChanged()
            tableView.refreshComplete()
            stateView.viewState = if (tableData.isEmpty()) VIEW_STATE_LOADING else VIEW_STATE_CONTENT
        }
    }

    @Subscribe
    public fun viewWillAppear(event: Events.SelectTab) {
        if (event.index == 1) {
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
        fun newInstance() = NewspaperFragment()
    }
}
