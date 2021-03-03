package cn.edu.tsinghua.thss.cercis

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import cn.edu.tsinghua.thss.cercis.ui.contacts.ContactListFragment
import cn.edu.tsinghua.thss.cercis.ui.session_list.SessionListFragment
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFlipper = findViewById(R.id.main_view_flipper)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            switchFragment(menuItem.itemId)
            true
        }

        bottomNavigation.selectedItemId = R.id.page_messages
    }

    private fun switchFragment(id: Int) {
        val fragment: Fragment? = when (id) {
            R.id.page_messages -> SessionListFragment()
            R.id.page_contacts -> ContactListFragment()
            R.id.page_discovery -> ContactListFragment()
            else -> null
        }
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.main_view_flipper, fragment)
                    .commit()
        }
    }

    private lateinit var viewFlipper: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
}