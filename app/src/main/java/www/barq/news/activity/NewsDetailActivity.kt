package www.barq.news.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.jcodecraeer.xrecyclerview.XRecyclerView
import com.kennyc.view.MultiStateView.VIEW_STATE_CONTENT
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.cell_ads.view.*
import kotlinx.android.synthetic.main.cell_news.view.*
import kotlinx.android.synthetic.main.cell_webview.view.*
import kotlinx.android.synthetic.main.layout_tableview.*
import org.greenrobot.eventbus.EventBus
import www.barq.news.Constants
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.extensions.*
import www.barq.news.model.News

class NewsDetailActivity : BaseActivity() {
    private var news: News? = null
    private var adView: AdView? = null
    private var webView: ScrollWebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_tableview)

        setTitleCenter("")

        val newsId = intent.getStringExtra("newsId")
        if (newsId != null) {
            loadNews(newsId)
        } else {
            news = Klaxon().parse<News>(intent.getStringExtra("news")!!)!!
            updateUI()
        }
    }

    private fun loadNews(newsId: String) {
        val params = listOf(
            "type" to "favorite_ids",
            "id" to newsId
        )
        post("/fetch_news", params = params) { res ->
            val klaxon = Klaxon()
            news = res.array<JsonObject>("data").orEmpty().map {
                klaxon.parseFromJsonObject<News>(it)!!
            }.first()
            updateUI()
        }
    }

    private fun updateUI() {
        invalidateOptionsMenu()
        setupRecyclerView()
        stateView.viewState = VIEW_STATE_CONTENT
    }

    override fun onResume() {
        adView?.resume()
        super.onResume()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.news_detail, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val icon = if (news?.favorited == true) R.drawable.ic_star_d else R.drawable.ic_star
        menu?.findItem(R.id.menuFavorite)?.setIcon(icon)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menuFavorite -> handleFavorite()
            R.id.menuComment -> navigate(CommentActivity::class.java, "news" to news!!)
            R.id.menuShare -> share(url = news!!.url, content = getString(R.string.share_news), title = news!!.title)
        }
        return true
    }

    private fun handleFavorite() {
        if (isLoggedIn) {
            val params = listOf(
                "newsId" to news!!.id,
                "newsTitle" to news!!.title!!,
                "sourceId" to news!!.source.id,
                "favorite" to !news!!.favorited
            )
            post("/favorite", params = params) { updateFavorite() }
        } else {
            if (news!!.favorited) {
                Defaults.favorites = Defaults.favorites.apply { removeAll { it == news!!.id } }
            } else {
                Defaults.favorites = Defaults.favorites.apply { add(news!!.id) }
            }
            updateFavorite()
        }
    }

    private fun updateFavorite() {
        val favorite = !news!!.favorited
        news!!.favorited = favorite
        invalidateOptionsMenu()
        EventBus.getDefault().post(Events.Favorited(news!!.id, favorite))
    }

    private fun setupRecyclerView() {
        tableView.apply {
            background = ColorDrawable(Color.WHITE)
            setHasFixedSize(true)
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            layoutManager = LinearLayoutManager(this@NewsDetailActivity, RecyclerView.VERTICAL, false)
            adapter = Adapter()
            setPullRefreshEnabled(false)
            setLoadingListener(object : XRecyclerView.LoadingListener {
                override fun onLoadMore() {
                    webView?.scrollEnable = true
                    loadMoreComplete()
                }

                override fun onRefresh() {}
            })
        }
    }

    inner class ScrollWebView : WebView(this) {
        var scrollEnable = true

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            requestDisallowInterceptTouchEvent(scrollEnable)
            return super.onTouchEvent(event)
        }

        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super.onScrollChanged(l, t, oldl, oldt)
            scrollEnable = canScrollVertically(-1)
        }
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return 3
        }

        override fun getItemViewType(position: Int): Int {
            return position
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(this@NewsDetailActivity)
            return when (viewType) {
                0 -> ViewHolder(inflater.inflate(R.layout.cell_news, parent, false))
                1 -> ViewHolder(inflater.inflate(R.layout.cell_ads, parent, false))
                else -> ViewHolder(inflater.inflate(R.layout.cell_webview, parent, false))
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (position) {
                0 -> holder.itemView.apply {
                    newsContainer.background = ColorDrawable(Color.WHITE)
                    Picasso.get().load(news!!.source.imageUrl).placeholder(R.drawable.empty).into(sourceImage)
                    Picasso.get().load(news!!.imageUrl).placeholder(R.drawable.empty).into(newsImage)
                    ConstraintSet().apply {
                        clone(newsContainer)
                        setDimensionRatio(newsImage.id, "1:${news!!.imageRatio ?: 0}")
                        applyTo(newsContainer)
                    }
                    sourceName.text = news!!.source.name
                    timeStamp.text = news!!.time.formattedString(this@NewsDetailActivity)
                    newsTitle.text = news!!.title
                    buttonPanel.visibility = GONE
                }
                1 -> holder.itemView.apply {
                    if (adViewContainer.childCount == 0 && Defaults.setting.adsNewsDetail) {
                        adViewContainer.visibility = VISIBLE
                        adView = AdView(this@NewsDetailActivity).apply {
                            adSize = AdSize.MEDIUM_RECTANGLE
                            adUnitId = Constants.AD_UNIT_ID
                            if (Defaults.setting.adsNewsDetail) {
                                loadAd(AdRequest.Builder().build())
                            }
                        }
                        adViewContainer.addView(adView!!)
                    } else {
                        adViewContainer.visibility = GONE
                    }
                }
                else -> holder.itemView.apply {
                    if (webViewContainer.childCount == 1) {
                        val hud = webViewContainer.findViewById(R.id.hudContainer) as LinearLayout
                        webView = ScrollWebView().apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    super.onPageCommitVisible(view, url)
                                    if (hud.isVisible) {
                                        hud.visibility = GONE
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (hud.isVisible) {
                                        hud.visibility = GONE
                                    }
                                }
                            }
                            webChromeClient = WebChromeClient()
                            settings.javaScriptEnabled = true
                            loadUrl(news!!.url)
                        }
                        webViewContainer.addView(webView!!, 0, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    }
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}
