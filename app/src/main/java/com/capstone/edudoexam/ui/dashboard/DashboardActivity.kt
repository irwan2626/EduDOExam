package com.capstone.edudoexam.ui.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.capstone.edudoexam.R
import com.capstone.edudoexam.components.AppContextWrapper
import com.capstone.edudoexam.components.NetworkStatusHelper
import com.capstone.edudoexam.components.ui.AppBarLayout
import com.capstone.edudoexam.databinding.ActivityDashboard2Binding
import com.capstone.edudoexam.ui.LoadingHandler
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale


class DashboardActivity : AppCompatActivity(), NavController.OnDestinationChangedListener, LoadingHandler {

    private val _binding: ActivityDashboard2Binding by lazy {
        ActivityDashboard2Binding.inflate(layoutInflater)
    }
    private val sharedViewModel: SharedViewModel by viewModels()
    private val networkStatusHelper: NetworkStatusHelper by lazy {
        NetworkStatusHelper(this) { isConnected ->
            runOnUiThread {
                if (isConnected) {
                    _binding.noConnectionLayout.apply {
                        animate()
                            .alpha(0f)
                            .withEndAction {
                                visibility = View.GONE
                            }
                            .duration = 400
                    }
                } else {
                    _binding.noConnectionLayout.apply {
                        visibility = View.VISIBLE
                        alpha = 0f
                        animate()
                            .alpha(1f)
                            .duration = 400
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        networkStatusHelper.startListening()
    }

    override fun onStop() {
        super.onStop()
        networkStatusHelper.stopListening()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("UseSupportActionBar", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(_binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.setOnApplyWindowInsetsListener(_binding.navView) { v, windowInsets  ->
            v.setPadding(0, 0, 0, 0)
            windowInsets
        }
        setContentView(_binding.root)

        setSupportActionBar(_binding.appBarLayout.toolbar)

        val navView: BottomNavigationView = _binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_dashboard)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_exams, R.id.nav_histories, R.id.nav_settings)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener(this)

        _binding.apply {
            noConnectionLayout.setOnTouchListener{ _, _ -> true }
            appBarLayout.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                val appBarHeight = _binding.appBarLayout.height
                sharedViewModel.updateTopMargin(appBarHeight)
            }

            setLoading(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_dashboard)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private var lastDestinationId = 0
    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        if(destination.id != lastDestinationId) setLoading(true)
        lastDestinationId = destination.id

        _binding.apply {
            resetUI()
            appBarLayout.removeAllMenus()
            when (destination.id) {
                R.id.nav_home -> {
                    appBarLayout.title    = getString(R.string.app_name)
                    appBarLayout.subtitle = getString(R.string.app_moto)
                }
                else -> {
                    appBarLayout.title    = destination.label.toString()
                    appBarLayout.subtitle = ""
                }
            }
        }
    }

    private fun resetUI() {
       _binding.apply {
           if (!navView.isVisible) showNavBottom()
       }
    }

    fun addMenu(@DrawableRes icon: Int, @ColorInt color: Int = 0, onClick: (View) -> Unit) {
        _binding.appBarLayout.addMenu(icon, color, onClick)
    }

    fun hideNavBottom() {
        try {
            _binding.navView.apply {
                if(isShown) {
                    visibility = View.VISIBLE
                    alpha = 1f
                    animate()
                        .setDuration(80)
                        .alpha(0f)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            visibility = View.GONE
                        }
                        .start()
                }
            }
        } catch (_: Throwable) {

        }
    }

    fun showNavBottom() {
        try {
            _binding.navView.apply {
                if(!isShown) {
                    visibility = View.VISIBLE
                    alpha = 0f
                    animate()
                        .setDuration(200)
                        .alpha(1f)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            }
        } catch (_: Throwable) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setLoading(isLoading: Boolean) {
        // Block touch events when loading
        _binding.loadingLayout.root.setOnTouchListener { _, _ -> isLoading }

        // Check the current visibility state to avoid redundant animations
        if (isLoading && _binding.loadingLayout.root.visibility == View.VISIBLE) return
        if (!isLoading && _binding.loadingLayout.root.visibility == View.GONE) return

        if (isLoading) {
            _binding.loadingLayout.root.visibility = View.VISIBLE
            _binding.loadingLayout.loadingIndicatorContainer.apply {
                // Reset for animation
                alpha = 0f
                scaleX = 2f
                scaleY = 2f
                translationX = 0.5f
                translationY = 0.5f

                // Animate in
                animate()
                    .setDuration(200)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationX(0f)
                    .translationY(0f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        } else {
            _binding.loadingLayout.loadingIndicatorContainer.apply {
                // Animate out
                animate()
                    .setDuration(200)
                    .alpha(0f)
                    .scaleX(2f)
                    .scaleY(2f)
                    .translationX(0.5f)
                    .translationY(0.5f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        // Set visibility to GONE after animation
                        _binding.loadingLayout.root.visibility = View.GONE
                    }
                    .start()
            }
        }
    }

    fun getAppbar() : AppBarLayout {
        return _binding.appBarLayout
    }

    override fun attachBaseContext(newBase: Context?) {
        newBase?.let {
            val language = PreferenceManager.getDefaultSharedPreferences(newBase).getString("pref_language", "en") ?: "en"
            super.attachBaseContext(AppContextWrapper.wrap(it, language))
        } ?: run {
            super.attachBaseContext(newBase)
        }

    }

}