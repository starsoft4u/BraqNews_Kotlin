package www.barq.news.custom

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.jcodecraeer.xrecyclerview.ArrowRefreshHeader

class XRecyclerViewHeader(context: Context) : ArrowRefreshHeader(context) {
    init {
        findViewById<View>(com.jcodecraeer.xrecyclerview.R.id.listview_header_text).visibility = View.GONE

        findViewById<View>(com.jcodecraeer.xrecyclerview.R.id.listview_header_arrow).layoutParams =
            RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                }

        findViewById<View>(com.jcodecraeer.xrecyclerview.R.id.listview_header_progressbar).layoutParams =
            RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                }
    }
}
