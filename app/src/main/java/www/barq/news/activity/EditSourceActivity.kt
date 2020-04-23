package www.barq.news.activity

import android.os.Bundle
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kennyc.view.MultiStateView.VIEW_STATE_CONTENT
import kotlinx.android.synthetic.main.layout_tableview.*
import www.barq.news.R
import www.barq.news.adapter.CategoryAdapter
import www.barq.news.custom.XRecyclerViewHeader
import www.barq.news.extensions.navigate
import www.barq.news.extensions.setTitleCenter
import www.barq.news.extensions.toPixel
import www.barq.news.model.Category

class EditSourceActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_tableview)

        setTitleCenter(getString(R.string.edit_source))

        val tableData = listOf(
            Category(channelId = 0, name = getString(R.string.my_sources)),
            Category(channelId = 1, name = getString(R.string.news_paper)),
            Category(channelId = 2, name = getString(R.string.magazine)),
            Category(channelId = 3, name = getString(R.string.tv_channels)),
            Category(channelId = 4, name = getString(R.string.web_sources)),
            Category(channelId = 5, name = getString(R.string.government)),
            Category(channelId = 6, name = getString(R.string.sports_clubs))
        )

        tableView.apply {
            setPadding(toPixel(12f).toInt())
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@EditSourceActivity, RecyclerView.VERTICAL, false)
            adapter = CategoryAdapter(this@EditSourceActivity, tableData) {
                navigate(SourceSelectActivity::class.java, "title" to tableData[it].name!!, "channel" to it)
            }
            setRefreshHeader(XRecyclerViewHeader(this@EditSourceActivity))
            setLoadingMoreEnabled(false)
            setPullRefreshEnabled(false)
        }

        stateView.viewState = VIEW_STATE_CONTENT
    }
}
