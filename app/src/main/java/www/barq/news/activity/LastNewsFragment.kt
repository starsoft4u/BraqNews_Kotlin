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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.jcodecraeer.xrecyclerview.XRecyclerView
import com.kennyc.view.MultiStateView.VIEW_STATE_CONTENT
import com.kennyc.view.MultiStateView.VIEW_STATE_EMPTY
import kotlinx.android.synthetic.main.layout_tableview.*
import kotlinx.android.synthetic.main.state_empty.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import www.barq.news.*
import www.barq.news.adapter.NewsAdapter
import www.barq.news.custom.XRecyclerViewFooter
import www.barq.news.custom.XRecyclerViewHeader
import www.barq.news.extensions.isLoggedIn
import www.barq.news.extensions.navigate
import www.barq.news.extensions.post
import www.barq.news.model.News

class LastNewsFragment : Fragment() {
    private val tableData = arrayListOf<Any>()
    private var minId = 0L
    private val adsToLoad = arrayListOf<Int>()

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
            adapter = NewsAdapter(context = context!!, items = tableData)
            setRefreshHeader(XRecyclerViewHeader(context!!))
            setFootView(XRecyclerViewFooter(context!!), XRecyclerViewFooter.listener)
            setLoadingListener(object : XRecyclerView.LoadingListener {
                override fun onLoadMore() {
                    loadData(refresh = false, indicator = false)
                }

                override fun onRefresh() {
                    loadData(refresh = true, indicator = false)
                }
            })
        }

        emptyStateButton.setOnClickListener {
            context!!.navigate(EditSourceActivity::class.java)
        }

        val parent = activity as? MainActivity
        loadData(refresh = true, indicator = parent?.fromNotification != true)
    }

    override fun onResume() {
        tableData.filterIsInstance<AdView>().forEach { it.resume() }
        super.onResume()
    }

    override fun onPause() {
        tableData.filterIsInstance<AdView>().forEach { it.pause() }
        super.onPause()
    }

    override fun onDestroy() {
        tableData.filterIsInstance<AdView>().forEach { it.destroy() }
        super.onDestroy()
    }

    private fun loadData(refresh: Boolean, indicator: Boolean) {
        if (!isLoggedIn && Defaults.sources.isEmpty()) {
            tableData.clear()
            minId = 0
            tableView.adapter?.notifyDataSetChanged()
            tableView.refreshComplete()
            tableView.loadMoreComplete()
            stateView.viewState = VIEW_STATE_EMPTY
            return
        }

        if (refresh) {
            minId = 0
            adsToLoad.clear()
            tableView.setLoadingMoreEnabled(true)
        }

        val params = if (isLoggedIn) {
            listOf(
                "type" to "last",
                "id" to Defaults.userId,
                "minId" to minId
            )
        } else {
            listOf(
                "type" to "source_ids",
                "id" to Defaults.sourcesRaw,
                "minId" to minId
            )
        }
        context!!.post("/fetch_news", params = params, indicator = indicator) { res ->
            val klaxon = Klaxon()
            val news = res.array<JsonObject>("data").orEmpty().map {
                klaxon.parseFromJsonObject<News>(it)!!
            }
            if (!isLoggedIn) {
                val favorites = Defaults.favorites
                news.forEach { it.favorited = favorites.contains(it.id) }
            }

            if (refresh) {
                tableData.clear()
            }

            if (news.isNotEmpty()) {
                val start = tableData.count()
                tableData.addAll(news)
                if (Defaults.setting.adsLastNews) {
                    setupAds()
                }
                val end = tableData.count()
                val added = tableData.subList(start, end)

                if (refresh) {
                    tableView.adapter?.notifyDataSetChanged()
                } else {
                    tableView.notifyItemInserted(added, start)
                }
            }

            tableView.refreshComplete()
            tableView.loadMoreComplete()

            if (news.isEmpty()) {
                tableView.setLoadingMoreEnabled(false)
            } else {
                minId = news.minBy { it.id }!!.id
            }

            stateView.viewState = if (tableData.isEmpty()) VIEW_STATE_EMPTY else VIEW_STATE_CONTENT
        }
    }

    private fun setupAds() {
        val isLoading = adsToLoad.isNotEmpty()

        var index = Constants.AD_OFFSET
        while (index < tableData.size) {
            if (tableData[index] is News) {
                tableData.add(index, AdView(context!!).apply {
                    adSize = AdSize.MEDIUM_RECTANGLE
                    adUnitId = Constants.AD_UNIT_ID
                })
                adsToLoad.add(index)
            }

            index += Constants.AD_INTERVAL
        }

        if (!isLoading) {
            preloadNextAd()
        }
    }

    private fun preloadNextAd() {
        if (adsToLoad.isNotEmpty()) {
            val index = adsToLoad.first()
            val adView = tableData[index] as AdView
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    preloadNextAd()
                }

                override fun onAdFailedToLoad(p0: Int) {
                    super.onAdFailedToLoad(p0)
                    preloadNextAd()
                }
            }
            adView.loadAd(AdRequest.Builder().build())
            adsToLoad.removeAt(0)
        }
    }

    @Subscribe
    public fun viewWillAppear(event: Events.SelectTab) {
        if (event.index == 0) {
            loadData(refresh = true, indicator = true)
        }
    }

    @Subscribe
    public fun onFavoriteChanged(event: Events.Favorited) {
        val index = tableData.indexOfFirst { it is News && it.id == event.newsId }
        if (index >= 0) {
            (tableData[index] as News).favorited = event.favorited
            tableView.notifyItemChanged(index)
        }
    }

    @Subscribe
    fun onSourceModified(event: Events.SourceModified) {
        if (MainActivity.selectedTab == 0) {
            loadData(refresh = true, indicator = true)
        }
    }

    @Subscribe
    fun onCommentChanged(event: Events.Commented) {
        val index = tableData.indexOfFirst { it is News && it.id == event.newsId }
        if (MainActivity.selectedTab == 0 && index >= 0) {
            (tableData[index] as News).comment = event.count
            tableView.notifyItemChanged(index)
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
        fun newInstance() = LastNewsFragment()
    }
}