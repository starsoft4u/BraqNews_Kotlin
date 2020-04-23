package www.barq.news.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.cell_setting.view.*
import www.barq.news.R

class SettingMenuView : LinearLayout {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        attrs?.let {
            val attributes = context.obtainStyledAttributes(it, R.styleable.SettingMenuView)

            text.text = attributes.getText(R.styleable.SettingMenuView_text)
            icon.setImageResource(attributes.getResourceId(R.styleable.SettingMenuView_icon, R.drawable.empty))

            attributes.recycle()
        }
    }

    init {
        inflate(context, R.layout.cell_setting, this)
    }
}