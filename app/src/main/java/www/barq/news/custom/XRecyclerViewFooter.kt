package www.barq.news.custom

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.setPadding
import com.jcodecraeer.xrecyclerview.CustomFooterViewCallBack
import com.jcodecraeer.xrecyclerview.LoadingMoreFooter
import www.barq.news.extensions.toPixel

class XRecyclerViewFooter(context: Context) : LoadingMoreFooter(context) {
    init {
        setPadding(toPixel(24f).toInt())
        visibility = View.GONE
        children
            .filter { it is TextView }
            .forEach { it.visibility = View.GONE }
    }

    companion object {
        val listener = object : CustomFooterViewCallBack {
            override fun onSetNoMore(yourFooterView: View?, noMore: Boolean) {}
            override fun onLoadingMore(yourFooterView: View?) {}
            override fun onLoadMoreComplete(yourFooterView: View?) {}
        }
    }
}