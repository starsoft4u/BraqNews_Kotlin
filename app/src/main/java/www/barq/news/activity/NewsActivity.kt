package www.barq.news.activity

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.widget.CompoundButton
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
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.layout_tableview.*
import kotlinx.android.synthetic.main.source.view.*
import kotlinx.android.synthetic.main.state_empty.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import www.barq.news.Constants
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.adapter.NewsAdapter
import www.barq.news.custom.RoundTransform
import www.barq.news.custom.XRecyclerViewFooter
import www.barq.news.custom.XRecyclerViewHeader
import www.barq.news.extensions.*
import www.barq.news.model.News
import www.barq.news.model.Source

enum class NewsType {
    MAGAZINE, FAVORITE, NEWSPAPER, MY_NEWS, SEARCH;

    val adsEnable: Boolean
        get() = when (this) {
            MAGAZINE -> Defaults.setting.adsMagazine
            NEWSPAPER -> Defaults.setting.adsNewspaper
            else -> false
        }
}

class NewsActivity : BaseActivity() {
    private val tableData = arrayListOf<Any>()
    private var minId = 0L
    private val adsToLoad = arrayListOf<Int>()

    private var header: View? = null

    private lateinit var type: NewsType
    private lateinit var source: Source

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_tableview)

        // params
        type = NewsType.valueOf(intent.getStringExtra("type")!!)
        source = Klaxon().parse(intent.getStringExtra("source")!!)!!

        setTitleCenter(source.name)

        setupTableView()
        setupStateView()

        loadData(refresh = true, indicator = true)

        // EventBus
        EventBus.getDefault().register(this)
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
        EventBus.getDefault().unregister(this)
        tableData.filterIsInstance<AdView>().forEach { it.destroy() }
        super.onDestroy()
    }

    private fun setupTableView() {
        tableView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@NewsActivity, RecyclerView.VERTICAL, false)
            adapter = NewsAdapter(this@NewsActivity, tableData)

            // Header
            setRefreshHeader(XRecyclerViewHeader(this@NewsActivity))
            if (type != NewsType.FAVORITE && type != NewsType.MAGAZINE) {
                header = this@NewsActivity.layoutInflater.inflate(R.layout.source, tableView, false)
                tableView.addHeaderView(header!!.apply {
                    Picasso.get().load(source.backgroundUrl).placeholder(R.drawable.empty).into(bannerImage)
                    Picasso.get().load(source.imageUrl).transform(RoundTransform(radius = 8f))
                        .placeholder(R.drawable.empty).into(sourceImage)
                    sourceName.text = source.name
                    sourceDescription.text = source.description
                    updateSourceView()
                    followButton.setOnClickListener { onFollowAction() }
                    notification.setOnCheckedChangeListener { view, checked ->
                        onNotificationAction(view, checked)
                    }
                })
            }

            // Footer
            setLoadingMoreEnabled(type != NewsType.FAVORITE)
            setFootView(XRecyclerViewFooter(this@NewsActivity), XRecyclerViewFooter.listener)

            // Listener
            setLoadingListener(object : XRecyclerView.LoadingListener {
                override fun onLoadMore() {
                    loadData(refresh = false, indicator = false)
                }

                override fun onRefresh() {
                    loadData(refresh = true, indicator = false)
                }
            })
        }
    }

    private fun setupStateView() {
        if (type == NewsType.FAVORITE) {
            emptyStateTitle.setText(R.string.you_don_t_have_any_news_in_your_favorite)
            emptyStateDescription.setText(R.string.to_add_news_to_your_favorite_click_on_star_on_top_bar_of_any_news)
        } else {
            emptyStateTitle.setText(R.string.no_news_available)
            emptyStateDescription.visibility = GONE
        }
        emptyStateButton.visibility = GONE
    }

    private fun updateSourceView() {
        header?.followers?.text = getString(R.string.followers_, source.followerCount)
        header?.followButton?.apply {
            if (source.isMySource) {
                setBackgroundResource(R.drawable.button_primary)
                setImageResource(R.drawable.ic_check_w)
            } else {
                setBackgroundResource(R.drawable.border_round_red)
                setImageResource(R.drawable.ic_check)
            }
        }
        header?.notification?.apply {
            isEnabled = Defaults.notifyGlobal && source.isMySource
            isChecked = source.isMySource && Defaults.notificaitons.contains(source.id)
        }
    }

    private fun onNotificationAction(view: CompoundButton, checked: Boolean) {
        if (isLoggedIn && view.isEnabled) {
            get("/block_source/id/${source.id}/block/${if (checked) 0 else 1}") {
                handleNotification(checked)
            }
        } else {
            handleNotification(checked)
        }
    }

    private fun handleNotification(checked: Boolean) {
        if (checked) {
            Defaults.notificaitons = Defaults.notificaitons.apply { add(source.id) }
            subscribeNotification(source.id.toString(), true)
        } else {
            Defaults.notificaitons = Defaults.notificaitons.apply { remove(source.id) }
            subscribeNotification(source.id.toString(), false)
        }
    }

    private fun onFollowAction() {
        val checked = source.isMySource
        if (isLoggedIn) {
            get("/register_source/id/${source.id}/register/${if (checked) 0 else 1}") {
                handleFollow(!checked)
            }
        } else {
            handleFollow(!checked)
        }

    }

    private fun handleFollow(follow: Boolean) {
        if (follow) {
            Defaults.sources = Defaults.sources.apply { add(source.id) }
            source.followerCount += 1
        } else {
            Defaults.sources = Defaults.sources.apply { remove(source.id) }
            source.followerCount -= 1
        }

        source.isMySource = follow

        updateSourceView()

        EventBus.getDefault().post(Events.Followed(source.id, follow))
    }

    private fun loadData(refresh: Boolean, indicator: Boolean) {
        if (type == NewsType.FAVORITE && !isLoggedIn && Defaults.favorites.isEmpty()) {
            tableView.refreshComplete()
            tableView.loadMoreComplete()
            return
        }

        if (refresh) {
            minId = 0
            adsToLoad.clear()
            tableView.setLoadingMoreEnabled(type != NewsType.FAVORITE)
        }

        val params: List<Pair<String, Any>> = when {
            type == NewsType.FAVORITE && isLoggedIn -> listOf(
                "minId" to minId,
                "type" to "favorite",
                "id" to Defaults.userId
            )
            type == NewsType.FAVORITE && !isLoggedIn -> listOf(
                "minId" to minId,
                "type" to "favorite_ids",
                "id" to Defaults.favoritesRaw
            )
            type == NewsType.MAGAZINE -> listOf(
                "minId" to minId,
                "type" to "category",
                "id" to source.id
            )
            else -> listOf(
                "minId" to minId,
                "type" to "source",
                "id" to source.id
            )
        }

        post("/fetch_news", params = params, indicator = indicator) { res ->
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
                if (type.adsEnable) {
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
                tableData.add(index, AdView(this).apply {
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
    public fun onFavoriteChanged(event: Events.Favorited) {
        val index = tableData.indexOfFirst { it is News && it.id == event.newsId }
        if (index >= 0) {
            (tableData[index] as News).favorited = event.favorited
            tableView.notifyItemChanged(index)
        }
    }

    @Subscribe
    fun onCommentChanged(event: Events.Commented) {
        val index = tableData.indexOfFirst { it is News && it.id == event.newsId }
        if (index >= 0) {
            (tableData[index] as News).comment = event.count
            tableView.notifyItemChanged(index)
        }
    }
}
