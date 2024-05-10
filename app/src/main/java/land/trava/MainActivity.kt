package land.trava

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.RangeSlider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException

class MainActivity : AppCompatActivity() {

    private val layoutManager by lazy { GridLayoutManager(this, 2) }
    private val recyclerDataArrayList = ArrayList<PlantDataStruct>()
    private val filterValues = PlantDataStruct(this)
    private val searchList = ArrayList<PlantDataStruct>()
    private val database = Firestore("Plants")
    private val databaseRecordAmount = database.databaseRecordAmount
    private var allowSearchMoreDuringScroll = false
    private var allowLoadMoreDuringScroll = false
    private var allowSearchMore = true
    private var keyboardActive = false
    private lateinit var adapter: RecyclerViewAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var mainLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseAnalytics.getInstance(this)
        Storage.setContext(this)
        recyclerViewSetup()
        searchViewInsets()
        transparentNavBar("On")
        loadFirst12Items()
        scrollListener()
        searchBarListener()
        filterMenuListener()
        climateInfoButtonListener()
        remoteConfigListener()
    }

    override fun onBackPressed() {
        val plantView = findViewById<View>(R.id.plant_view)
        val searchView = findViewById<View>(R.id.search_view)
        val filterView = findViewById<View>(R.id.filter_view)
        val hardiness = findViewById<View>(R.id.hardiness)

        when {
            hardiness.isVisible -> hardiness.visibility = View.GONE
            plantView.isVisible -> {
                plantView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                searchView.visibility = View.VISIBLE
            }
            filterView.isVisible -> {
                filterView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                searchView.visibility = View.VISIBLE
            }
            else -> super.onBackPressed()
        }
    }

    private fun loadFirst12Items() {
        database.start(object : Firestore.getPlantsCallback {
            override fun onComplete(plantsArray: ArrayList<PlantDataStruct>) {
                Log.i("Get Plants", "These plants have been retrieved: $plantsArray")

                recyclerView.adapter = adapter

                recyclerDataArrayList.addAll(plantsArray)
                adapter.increaseDataList(plantsArray)
                allowLoadMoreDuringScroll = true

                findViewById<TextView>(R.id.status_text).isVisible = false
            }
        })
    }

    private fun scrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val searchView = findViewById<View>(R.id.search_view)
                val filterView = findViewById<View>(R.id.filter_view)

                if (dy > 0) {
                    Log.i("Scroll", "Down")
                    if (searchView.visibility == View.VISIBLE) {
                        searchView.visibility = View.GONE
                    }
                } else if (dy < 0 && filterView.visibility == View.GONE) {
                    Log.i("Scroll", "Up")
                    if (searchView.visibility == View.GONE) {
                        searchView.visibility = View.VISIBLE
                    }
                }

                // When scroll-down
                if (dy > 0 && allowLoadMoreDuringScroll) {
                    // Amount of elements on the screen
                    val itemCountOnScreen = layoutManager.itemCount
                    val totalItemCount = recyclerDataArrayList.size

                    if (itemCountOnScreen < databaseRecordAmount && totalItemCount < databaseRecordAmount) {
                        // Amount number of scrolled items
                        val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                        // When the last 6 elements are visible
                        if (itemCountOnScreen - pastVisibleItems <= 6) {
                            // Forbid load new data on scroll
                            allowLoadMoreDuringScroll = false

                            // Callback from getPlant function
                            database.getPlants(object : Firestore.getPlantsCallback {
                                override fun onComplete(plantsArray: ArrayList<PlantDataStruct>) {
                                    Log.i("Get Plants", "This plants have been gotten: $plantsArray")

                                    // Received data add to main array
                                    recyclerDataArrayList.addAll(plantsArray as Collection<PlantDataStruct>)

                                    // When search loading is allowed
                                    if (allowSearchMoreDuringScroll) {
                                        searchList.clear()
                                        searchInNewDataChunkOnScroll(plantsArray)
                                    } else {
                                        adapter.increaseDataList(plantsArray)
                                        allowLoadMoreDuringScroll = true
                                    }
                                }
                            })
                        }
                    }
                }
            }
        })
    }

    /**
     * Remote config listener. Triggered when keys updated
     */
    private fun remoteConfigListener() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()

        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate : ConfigUpdate) {
                Log.d(TAG, "Updated keys: " + configUpdate.updatedKeys)

                remoteConfig.activate().addOnCompleteListener {_ ->
                    val newVersion = remoteConfig.getLong("version").toByte()
                    updateChecker(newVersion)
                    Log.i("Remote Config", "Activate succeeded: $newVersion")
                }
            }

            override fun onError(error : FirebaseRemoteConfigException) {
                Log.w(TAG, "Config update error with code: " + error.code, error)
            }
        })

        val newVersion = remoteConfig.getLong("version").toByte()
        updateChecker(newVersion)
    }

    /**
     * Checks if the application needs to be updated
     */
    private fun updateChecker(newVersion: Byte) {

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val currentVersion = pInfo.versionCode.toByte()
        val updateAlert = findViewById<View>(R.id.update_alert)
        val updateButton = findViewById<Button>(R.id.button_update)

        if (currentVersion != newVersion && newVersion != 0.toByte()) {
            updateAlert.visibility = View.VISIBLE
        } else {
            updateAlert.visibility = View.GONE
        }

        updateButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=land.trava"))
            startActivity(browserIntent)
        }
    }

    /**
     * ClimateInfoButton listener. Triggered when climateInfoButton pressed
     */
    private fun climateInfoButtonListener() {
        val plantClimateInfo = findViewById<Button>(R.id.plant_climate_info)
        val filterClimateInfo = findViewById<Button>(R.id.filter_climate_info)
        val hardiness = findViewById<View>(R.id.hardiness)

        plantClimateInfo.setOnClickListener { hardiness.visibility = View.VISIBLE }
        filterClimateInfo.setOnClickListener { hardiness.visibility = View.VISIBLE }
    }

    /**
     * SearchBar listener. Triggered when searching
     */
    private fun searchBarListener() {
        val search = findViewById<EditText>(R.id.search_bar_text)
        val searchIcon = findViewById<ImageView>(R.id.search_icon)
        val closeIcon = findViewById<ImageView>(R.id.close_icon)

        search.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Log.i("Focus", "Focus active")
                transparentNavBar("Off")
                keyboardActive = true
            } else {
                Log.i("Focus", "Focus lost")
                transparentNavBar("On")
                if (keyboardActive) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(
                        search.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                    keyboardActive = false
                }
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(receiveQuery: Editable) {
                val status = findViewById<TextView>(R.id.status_text)
                status.visibility = View.GONE
                // Forbid load new data by query on scroll
                allowSearchMoreDuringScroll = false
                allowLoadMoreDuringScroll = false
                val query = receiveQuery.toString()
                searchList.clear()
                if (query == "") {
                    Log.i("Query changed", "Query is \"\"")
                    filterValues.name = null
                    closeIcon.visibility = View.GONE
                    setMargin(search, 10f, 0f, 0f, 0f)
                    searchIcon.visibility = View.VISIBLE
                    if (filterEmpty()) {
                        adapter.setFilteredList(recyclerDataArrayList)
                    } else {
                        adapter.setFilteredList(searchFirst12Items())
                    }
                } else {
                    Log.i("Query changed", "Now query is: $query")
                    filterValues.name = query
                    searchIcon.visibility = View.GONE
                    setMargin(search, 16f, 0f, 8f, 0f)
                    closeIcon.visibility = View.VISIBLE
                    adapter.setFilteredList(searchFirst12Items())
                }
            }
        })

        searchIcon.setOnClickListener {
            Log.i("searchIcon", "Search icon have been clicked")
            search.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS)
        }

        closeIcon.setOnClickListener {
            Log.i("closeIcon", "Close icon have been clicked")
            search.clearFocus()
            search.setText("")
        }
    }

    private fun filterMenuListener() {
        val searchView = findViewById<View>(R.id.search_view)
        val recyclerView = findViewById<View>(R.id.recycler_view)
        val filterMenuButton = findViewById<View>(R.id.filter_menu_button)
        val filterView = findViewById<View>(R.id.filter_view)
        val categoryView = findViewById<View>(R.id.category_view)
        val categoryFilterView = findViewById<View>(R.id.category_filter_view)
        val luminosityView = findViewById<View>(R.id.luminosity_view)
        val luminosityFilterView = findViewById<View>(R.id.luminosity_filter_view)
        val applyButton = findViewById<Button>(R.id.button_apply)
        val clearButton = findViewById<Button>(R.id.button_clear)

        filterMenuButton.setOnClickListener {
            filterView.visibility = View.VISIBLE
            searchView.visibility = View.GONE
            recyclerView.visibility = View.GONE
        }

        categoryView.setOnClickListener {
            categoryFilterView.visibility = if (categoryFilterView.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        luminosityView.setOnClickListener {
            luminosityFilterView.visibility = if (luminosityFilterView.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        applyButton.setOnClickListener {
            val status = findViewById<TextView>(R.id.status_text)
            status.visibility = View.GONE

            getFilterValue()
            searchList.clear()

            if (filterValues.name == "" && filterEmpty()) {
                adapter.setFilteredList(recyclerDataArrayList)
            } else {
                adapter.setFilteredList(searchFirst12Items())
            }

            filterView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            searchView.visibility = View.VISIBLE

            categoryFilterView.visibility = View.GONE
            luminosityFilterView.visibility = View.GONE
        }

        clearButton.setOnClickListener {
            val status = findViewById<TextView>(R.id.status_text)
            status.visibility = View.GONE

            filterValues.category = null
            filterValues.climate = null
            filterValues.luminosity = null
            filterValues.bloom = null

            searchList.clear()

            if (filterValues.name == null) {
                adapter.setFilteredList(recyclerDataArrayList)
            } else {
                adapter.setFilteredList(searchFirst12Items())
            }

            filterView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            searchView.visibility = View.VISIBLE

            categoryFilterView.visibility = View.GONE
            luminosityFilterView.visibility = View.GONE
        }
    }

    /**
     * Convert floatList to byteArrayList
     */
    private fun floatListToByteArrayList(floatList: List<Float>): ArrayList<Byte> {
        val byteArrayList = ArrayList<Byte>()

        for (element in floatList) {
            byteArrayList.add(element.toInt().toByte())
        }

        return byteArrayList
    }

    /**
     * Is filter empty?
     */
    private fun filterEmpty(): Boolean {
        return filterValues.category == null &&
                filterValues.climate == null &&
                filterValues.luminosity == null &&
                filterValues.bloom == null
    }

    private fun getFilterValue() {
        val category = ArrayList<Byte>()
        val luminosity = ArrayList<Byte>()

        val categoryCheckBox1 = findViewById<CheckBox>(R.id.categoryCheckBox1)
        val categoryCheckBox2 = findViewById<CheckBox>(R.id.categoryCheckBox2)
        val categoryCheckBox3 = findViewById<CheckBox>(R.id.categoryCheckBox3)
        val categoryCheckBox4 = findViewById<CheckBox>(R.id.categoryCheckBox4)
        val categoryCheckBox5 = findViewById<CheckBox>(R.id.categoryCheckBox5)
        val categoryCheckBox6 = findViewById<CheckBox>(R.id.categoryCheckBox6)
        val categoryCheckBox7 = findViewById<CheckBox>(R.id.categoryCheckBox7)
        val categoryCheckBox8 = findViewById<CheckBox>(R.id.categoryCheckBox8)
        val categoryCheckBox9 = findViewById<CheckBox>(R.id.categoryCheckBox9)
        val categoryCheckBox10 = findViewById<CheckBox>(R.id.categoryCheckBox10)
        val categoryCheckBox11 = findViewById<CheckBox>(R.id.categoryCheckBox11)
        val categoryCheckBox12 = findViewById<CheckBox>(R.id.categoryCheckBox12)
        val luminosityCheckBox1 = findViewById<CheckBox>(R.id.luminosityCheckBox1)
        val luminosityCheckBox2 = findViewById<CheckBox>(R.id.luminosityCheckBox2)
        val luminosityCheckBox3 = findViewById<CheckBox>(R.id.luminosityCheckBox3)

        val climateSlider = findViewById<RangeSlider>(R.id.climateSlider)
        val bloomSlider = findViewById<RangeSlider>(R.id.bloomSlider)

        val climate = floatListToByteArrayList(climateSlider.values)
        val bloom = floatListToByteArrayList(bloomSlider.values)

        if (categoryCheckBox1.isChecked) {
            category.add(1)
        }
        if (categoryCheckBox2.isChecked) {
            category.add(2)
        }
        if (categoryCheckBox3.isChecked) {
            category.add(3)
        }
        if (categoryCheckBox4.isChecked) {
            category.add(4)
        }
        if (categoryCheckBox5.isChecked) {
            category.add(5)
        }
        if (categoryCheckBox6.isChecked) {
            category.add(6)
        }
        if (categoryCheckBox7.isChecked) {
            category.add(7)
        }
        if (categoryCheckBox8.isChecked) {
            category.add(8)
        }
        if (categoryCheckBox9.isChecked) {
            category.add(9)
        }
        if (categoryCheckBox10.isChecked) {
            category.add(10)
        }
        if (categoryCheckBox11.isChecked) {
            category.add(11)
        }
        if (categoryCheckBox12.isChecked) {
            category.add(12)
        }

        if (luminosityCheckBox1.isChecked) {
            luminosity.add(1)
        }
        if (luminosityCheckBox2.isChecked) {
            luminosity.add(2)
        }
        if (luminosityCheckBox3.isChecked) {
            luminosity.add(3)
        }

        if (category.isEmpty()) {
            filterValues.category = null
        } else {
            filterValues.category = category
        }

        if (climate.isEmpty()) {
            filterValues.climate = null
        } else {
            filterValues.climate = climate
        }

        if (luminosity.isEmpty()) {
            filterValues.luminosity = null
        } else {
            filterValues.luminosity = luminosity
        }

        if (bloom.isEmpty()) {
            filterValues.bloom = null
        } else {
            filterValues.bloom = bloom
        }
    }

    private fun checkClimate(value: ArrayList<Byte>, filterValue: ArrayList<Byte>): Boolean {
        return filterValue[0] - value[0] >= 0 && value[value.size - 1] - filterValue[1] >= 0
    }

    private fun checkBloom(value: ArrayList<Byte>, filterValue: ArrayList<Byte>): Boolean {
        return value[value.size - 1] - filterValue[0] >= 0 && filterValue[1] - value[0] >= 0
    }

    private fun filter(prevPlantsArray: ArrayList<PlantDataStruct>): ArrayList<PlantDataStruct> {
        val filteredList = ArrayList<PlantDataStruct>()

        for (plant in prevPlantsArray) {
            if (filterValues.name == null || plant.name?.lowercase()!!.contains(filterValues.name!!.lowercase())) {
                if (filterValues.category == null || plant.category!!.intersect(filterValues.category!!.toSet()).isNotEmpty()) {
                    if (filterValues.climate == null || plant.climate?.let { checkClimate(it, filterValues.climate!!) } == true) {
                        if (filterValues.luminosity == null || plant.luminosity!!.intersect(filterValues.luminosity!!.toSet()).isNotEmpty()) {
                            if (filterValues.bloom == null || plant.bloom?.let { checkBloom(it, filterValues.bloom!!) } == true) {
                                filteredList.add(plant)
                            }
                        }
                    }
                }
            }
        }

        return filteredList
    }

    private fun searchFirst12Items(): ArrayList<PlantDataStruct> {
        searchList.addAll(filter(recyclerDataArrayList))

        if (recyclerDataArrayList.size < databaseRecordAmount) {
            if (searchList.size < database.c) {
                searchInNewDataChunk()
            } else {
                allowSearchMoreDuringScroll = true
                allowLoadMoreDuringScroll = true
            }
        } else {
            if (searchList.isEmpty()) {
                val status = findViewById<TextView>(R.id.status_text)
                status.visibility = View.VISIBLE
                status.setText(R.string.no_data_found)
            }
            allowSearchMoreDuringScroll = false
            allowLoadMoreDuringScroll = false
        }

        return searchList
    }

    private fun searchInNewDataChunk() {
        val status = findViewById<TextView>(R.id.status_text)

        if (recyclerDataArrayList.size < databaseRecordAmount) {
            if (searchList.isEmpty()) {
                if (status.visibility == View.GONE) {
                    status.visibility = View.VISIBLE
                    status.setText(R.string.loading)
                }
            } else {
                status.visibility = View.GONE
            }

            var filteredList: ArrayList<PlantDataStruct> = ArrayList()
            if (searchList.size < database.c) {
                if (allowSearchMore) {
                    allowSearchMore = false

                    database.getPlants(object : Firestore.getPlantsCallback {
                        override fun onComplete(plantsArray: ArrayList<PlantDataStruct>) {
                            Log.i("Get Plants", "This plants have been got: $plantsArray")

                            filteredList = filter(plantsArray)
                            recyclerDataArrayList.addAll(plantsArray as Collection<PlantDataStruct>)
                            searchList.addAll(filteredList)
                            adapter.increaseDataList(filteredList)
                            allowSearchMore = true
                            searchInNewDataChunk()
                        }
                    })
                }
            } else {
                adapter.increaseDataList(filteredList)
                allowSearchMoreDuringScroll = true
                allowLoadMoreDuringScroll = true
            }
        } else {
            if (searchList.isEmpty()) {
                status.visibility = View.VISIBLE
                status.setText(R.string.no_data_found)
            } else {
                status.visibility = View.GONE
            }
            allowSearchMoreDuringScroll = false
            allowLoadMoreDuringScroll = false
        }
    }

    private fun searchInNewDataChunkOnScroll(prevPlantsArray: ArrayList<PlantDataStruct>) {
        val filteredList = filter(prevPlantsArray)
        searchList.addAll(filteredList)

        if (recyclerDataArrayList.size < databaseRecordAmount) {
            if (searchList.size < database.c) {
                if (allowSearchMore) {
                    allowSearchMore = false

                    database.getPlants(object : Firestore.getPlantsCallback {
                        override fun onComplete(plantsArray: ArrayList<PlantDataStruct>) {
                            Log.i("Get Plants", "This plants have been gotten: $plantsArray")

                            recyclerDataArrayList.addAll(plantsArray as Collection<PlantDataStruct>)
                            adapter.increaseDataList(filteredList)
                            allowSearchMore = true
                            searchInNewDataChunkOnScroll(plantsArray)
                        }
                    })
                }
            } else {
                adapter.increaseDataList(filteredList)
                allowSearchMoreDuringScroll = true
                allowLoadMoreDuringScroll = true
            }
        } else {
            allowSearchMoreDuringScroll = false
            allowLoadMoreDuringScroll = false
        }
    }

    private fun recyclerViewSetup() {
        mainLayout = findViewById(R.id.main_layout)
        recyclerView = findViewById(R.id.recycler_view)

        recyclerView.layoutManager = layoutManager
        adapter = RecyclerViewAdapter(recyclerDataArrayList, mainLayout)
    }

    private fun transparentNavBar(flagStatus: String) {
        when (flagStatus) {
            "On" -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                mainLayout.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                window.navigationBarColor = resources.getColor(R.color.green_transparent)

            }
            "Off" -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                mainLayout.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                window.navigationBarColor = resources.getColor(R.color.green_lv_1_5)

                val searchView = findViewById<View>(R.id.search_view)
                val density = getScreenDensity()
                searchView.setPadding(0, 0, 0, (16 * density).toInt())
            }
            else -> {
                Log.e("Fullscreen flags", "Please, use \"On\" or \"Off\"")
            }
        }
    }

    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(): Float {
        val resources = this.resources
        val density = getScreenDensity()

        @SuppressLint("ResourceType") val statusBarResourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(statusBarResourceId) / density
    }

    private fun getScreenDensity(): Float {
        return this.resources.displayMetrics.density
    }

    private fun setMargin(view: View, left: Float, top: Float, right: Float, bottom: Float) {
        val density = getScreenDensity()

        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins((left * density).toInt(), (top * density).toInt(), (right * density).toInt(), (bottom * density).toInt())
        view.layoutParams = params
    }

    private fun searchViewInsets() {
        val searchView = findViewById<View>(R.id.search_view)
        val plantView = findViewById<View>(R.id.plant_view)
        val filterView = findViewById<View>(R.id.filter_view)
        val recyclerView = findViewById<View>(R.id.recycler_view)
        val linearPlantView = findViewById<View>(R.id.linear_plant_view)
        val hardiness = findViewById<View>(R.id.hardiness)

        val density = getScreenDensity()
        val statusBarHeight = getStatusBarHeight()

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { _, insets ->
            var bottom:Double = insets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom.toDouble()
            val cut = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).bottom

            if (bottom == 0.0 && !keyboardActive) {
                bottom += cut
            } else {
                if (cut != 0 && !keyboardActive) {
                    bottom -= 16 * 2.75
                }
            }

            searchView.setPadding(0, 0, 0, (16 * density + bottom).toInt())
            recyclerView.setPadding((8 * density).toInt(), ((8 + statusBarHeight) * density).toInt(), (8 * density).toInt(), (88 * density + bottom).toInt())
            linearPlantView.setPadding(0, (statusBarHeight * density).toInt(), 0, 0)
            hardiness.setPadding((32 * density).toInt(), ((48 + statusBarHeight) * density).toInt(), (32 * density).toInt(), (48 * density + bottom).toInt())
            setMargin(plantView, 0f, 0f, 0f, (bottom / density).toFloat())
            setMargin(filterView, 16f, 16 + statusBarHeight, 16f, (16 + bottom / density).toFloat())
            insets
        }
    }
}