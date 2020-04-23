package www.barq.news.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import www.barq.news.BarqNewsApp
import www.barq.news.Defaults
import www.barq.news.Events
import www.barq.news.R
import www.barq.news.extensions.get
import www.barq.news.extensions.message
import www.barq.news.extensions.navigate
import www.barq.news.extensions.setTitleCenter

class MainActivity : AppCompatActivity() {
    companion object {
        var selectedTab: Int = 0
            set(value) {
                if (field != value) {
                    field = value
                    EventBus.getDefault().post(Events.SelectTab(value))
                }
            }
    }

    var fromNotification = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setTitleCenter(getString(R.string.last_news))

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_gear_d)
        }

        intent.getStringExtra("newsId")?.let {
            navigate(NewsDetailActivity::class.java, "newsId" to it)
            fromNotification = true
        }

        setupViewPager()
        setupTabBar()
        loadSetting()
    }

    override fun onDestroy() {
        BarqNewsApp.hideHUD()
        super.onDestroy()
    }

    private fun setupTabBar() {
        tabBar.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navLastNews -> {
                    if (viewPager.currentItem != 0) {
                        viewPager.currentItem = 0
                    }
                    selectedTab = 0
                    setTitleCenter(getString(R.string.last_news))
                }
                R.id.navNewspaper -> {
                    if (viewPager.currentItem != 1) {
                        viewPager.currentItem = 1
                    }
                    selectedTab = 1
                    setTitleCenter(getString(R.string.news_paper))
                }
                R.id.navMagazine -> {
                    if (viewPager.currentItem != 2) {
                        viewPager.currentItem = 2
                    }
                    selectedTab = 2
                    setTitleCenter(getString(R.string.magazine))
                }
                R.id.navMyNews -> {
                    if (viewPager.currentItem != 3) {
                        viewPager.currentItem = 3
                    }
                    selectedTab = 3
                    setTitleCenter(getString(R.string.my_news))
                }
                R.id.navSetting -> {
                    if (viewPager.currentItem != 4) {
                        viewPager.currentItem = 4
                    }
                    selectedTab = 4
                    setTitleCenter(getString(R.string.setting))
                }
            }
            supportActionBar?.setDisplayHomeAsUpEnabled(selectedTab != 4)
            invalidateOptionsMenu()
            true
        }
    }

    private fun setupViewPager() {
        viewPager.apply {
            offscreenPageLimit = 5
            adapter = object : FragmentPagerAdapter(supportFragmentManager) {
                override fun getItem(position: Int): Fragment {
                    return when (position) {
                        0 -> LastNewsFragment.newInstance()
                        1 -> NewspaperFragment.newInstance()
                        2 -> MagazineFragment.newInstance()
                        3 -> MyNewsFragment.newInstance()
                        else -> SettingFragment.newInstance()
                    }
                }

                override fun getCount(): Int {
                    return 5
                }
            }
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> tabBar.selectedItemId = R.id.navLastNews
                        1 -> tabBar.selectedItemId = R.id.navNewspaper
                        2 -> tabBar.selectedItemId = R.id.navMagazine
                        3 -> tabBar.selectedItemId = R.id.navMyNews
                        else -> tabBar.selectedItemId = R.id.navSetting
                    }
                }
            })
        }
    }

    private fun loadSetting() {
        get("/settings", indicator = false) { res ->
            val json = res.obj("data")!!
            Defaults.setting = Klaxon().parseFromJsonObject(json)!!
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (tabBar.selectedItemId == R.id.navSetting) {
            menu?.clear()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                tabBar.selectedItemId = R.id.navSetting
                true
            }
            R.id.menuSearch -> {
                navigate(SearchActivity::class.java)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
