package ddvm.coinz

import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.gson.JsonObject
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_main_bar.*
import kotlinx.android.synthetic.main.navigation_drawer_header.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener,
        PermissionsListener, DownloadCompleteListener,
        NavigationView.OnNavigationItemSelectedListener{

    private val tag = "MainActivity"

    private var mapView: MapView? = null
    private var map: MapboxMap? = null

    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null

    private var downloadDate = ""   //date of last downloaded map, format yyyy/MM/dd
    //private val preferencesFile = "MyPrefsFile"
    private var mapJson = ""        //downloaded geo-json map
    private var dateFormatted = ""   //current date formated as string

    private val coins = mutableListOf<Coin>()  //list storing coins available for collection on the map
    private val coinsMarkersMap = mutableMapOf<String, Long>()  //map matching coins id with their marker's id

    private var autocollection = false
    private val places = listOf(Bank, Shop, Tower)        //list of special places on the map

    private var originLocation: Location? = null
    private var originLatLng: LatLng? = null
    private lateinit var permissionsManager: PermissionsManager
    private var locationEngine: LocationEngine? = null
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    companion object {
        const val EXTRA_LOCATION = "userLastLocation"
        const val collectRange = 25  //range to collect coin in meters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        //Firebase authentication
        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth?.currentUser

        //Cloud firestore
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        //MapBox
        Mapbox.getInstance(this, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        //Navigation drawer
        nav_view.setNavigationItemSelectedListener(this)

        drawer_layout.addDrawerListener(
                object : DrawerLayout.DrawerListener {
                    override fun onDrawerOpened(drawerView: View) {}
                    override fun onDrawerClosed(drawerView: View) {}
                    override fun onDrawerStateChanged(newState: Int) {}
                    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                        updateDrawerHeader()
                    }
                }
        )

        recenter_fab.setOnClickListener {
            if(originLocation!=null)
                setCameraPosition(originLatLng!!) }

        //download user data from firestore
        User.downloadUserData(mAuth, firestore) {userDataDownloaded()}

        Log.d(tag,"[onCreate]: coins size: ${coins.size}")
        Log.d(tag, "[onCreate]: mapJson $mapJson end")
    }

    //updates the data in the header
    private fun updateDrawerHeader() {
        header_username.text = User.getUsername()
        header_gold.text = Utils.formatGold(User.getGold())             //display formatted gold

        //display vision range and check if vision is amplified by the tower
        //if vision amplified set text color to green
        if(originLatLng!=null) {
            val towerBuff = towerBuff(originLatLng!!)
            val range = (User.getVisionRange()*towerBuff).toInt()
            header_vision_range.text = range.toString() + 'm'
            if(towerBuff > 1)
                header_vision_range.setTextColor(Color.GREEN)
        } else {
            header_vision_range.text = User.getVisionRange().toString() + 'm'
        }

        val nCoins = User.getWallet().size  //number of coins in wallet
        val maxCapacity = User.getWalletCapacity()
        header_wallet_capacity.text = "$nCoins/$maxCapacity"
    }

    //handling item selections from navigation menu
    //starts chosen activity
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_wallet -> {
                //start activity and pass the current location
                startActivity(Intent(this, WalletActivity::class.java)
                        .putExtra(EXTRA_LOCATION, originLatLng))
            }
            R.id.nav_shop -> {
                startActivity(Intent(this, ShopActivity::class.java)
                        .putExtra(EXTRA_LOCATION, originLatLng))
            }
            R.id.nav_send_coins -> {
                startActivity(Intent(this, SendCoinsActivity::class.java)
                        .putExtra(EXTRA_LOCATION, originLatLng))
            }
            R.id.nav_received_coins -> {
                startActivity(Intent(this, ReceivedCoinsActivity::class.java))
            }
            R.id.nav_leaderboard -> {
                startActivity(Intent(this, LeaderboardActivity::class.java))
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            //sign the user out and go to login screen
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()    //sign out the user from the current session
                finish()
                startActivity(Intent(this,LoginActivity::class.java))   //go to login screen
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap == null){
            Log.d(tag, "[onMapReady] mapboxMap is null")
        }else{
            map = mapboxMap
            //Set user interface options
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true

            //make location information available
            enableLocation()
            //add markers of important places
            addPlacesMarkers()

            map?.setOnMarkerClickListener {marker ->
                if(originLatLng!=null) {
                    val distance = marker.position.distanceTo(originLatLng)       //distance from the user to the coin
                    if (distance <= collectRange) {
                        if(checkSpaceInWallet()) {
                            collectCoin(marker.title)
                            Toast.makeText(this, "Coin collected!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, getString(R.string.wallet_full), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this,
                                "Distance to coin: ${distance.roundToInt()}m",
                                Toast.LENGTH_SHORT)
                                .show()
                    }
                }
                true
            }
        }
    }

    private fun enableLocation() {
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        }else{
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun initialiseLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine?.apply{
            interval = 5000 //every 5 seconds
            fastestInterval = 1000 //at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine?.lastLocation
        if(lastLocation != null){
            originLocation = lastLocation
            originLatLng= LatLng(lastLocation.latitude, lastLocation.longitude)
            setCameraPosition(originLatLng!!)
        }else{
            locationEngine?.addLocationEngineListener(this)
        }

        displayInitialCoins()   //display coins if mapbox map and coin list available
    }

    private fun setCameraPosition(location: LatLng) {
        map?.animateCamera(CameraUpdateFactory.newLatLng(location))
    }

    private fun initialiseLocationLayer() {
        if (mapView == null) { Log.d(tag, "mapView is null") }
        else{
            if (map == null) { Log.d(tag, "map is null") }
            else{
                locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
                locationLayerPlugin.apply{
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
            }
        }

        //SIGSEGV
        lifecycle.addObserver(locationLayerPlugin)
    }

    override fun onLocationChanged(location: Location?) {
        if(location == null){
            Log.d(tag, "[onLocationChanged] location is null")
        }else{
            originLocation = location
            originLatLng = LatLng(location.latitude, location.longitude)
            setCameraPosition(originLatLng!!)
            //check if the map is ready and coins are downloaded. If there are no coins, then nothing to do either
            if(map != null && coins.size > 0) {
                displayCoinsInVisionRange(originLatLng!!)
                if(autocollection)  //check if autocollection enabled and there is space in the wallet
                    collectCoinsInRange(originLatLng!!)       //collect coins automatically when user in range
            }
        }
    }

    //adds markers of the Bank, Shop, Appleton Tower
    private fun addPlacesMarkers() {
        for(place in places) {
            val icon: Icon = IconFactory.getInstance(this).fromResource(place.placeMarkerResource) //icon of the marker
            //add marker to the map
            val marker = map?.addMarker(MarkerOptions()
                    .position(place.coordinates)
                    .title(place.placeName)
                    .icon(icon))

            if(marker == null) {
                Log.d(tag, "[addPlacesMarkers] marker is null")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()

        //SIGSEGV
        try {
            locationEngine?.requestLocationUpdates()
        } catch(ignored: SecurityException) {}
        locationEngine?.addLocationEngineListener(this)

        //current date
        val curDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        dateFormatted = curDate.format(formatter)   //current date

        //check if map for a given day already downloaded, else download it
        getGeoJsonMap()
        //updateDrawerHeader()    //updates header data
    }

    //runs after the user data is downloaded
    private fun userDataDownloaded() {
        updateUserData()
        getCoinsFromJson()
        //updateDrawerHeader()
    }

    //check the date of the last downloaded map stored in shared preferences
    //download a new map if necessary
    private fun getGeoJsonMap() {
        downloadDate = Utils.getLastDownloadDateFromSharedPrefs(this)
        if(dateFormatted == downloadDate) {
            mapJson = Utils.getMapFromSharedPrefs(this)
            getCoinsFromJson()
        } else {
            downloadDate = dateFormatted
            val url = "http://homepages.inf.ed.ac.uk/stg/coinz/$dateFormatted/coinzmap.geojson"
            DownloadFileTask(this).execute(url)     //downloads the map
        }
    }

    //check if last played date is today.
    //If not then reset collected coins, number of coins paid into the bank and set the current play date
    private fun updateUserData() {
        if(User.getLastPlayDate() != dateFormatted){
            User.setLastPlayDate(firestore, dateFormatted)
            User.clearCollectedCoins(firestore)
            User.clearNPaidInCoins(firestore)
        }
    }

    //runs after geo-JSON map is downloaded
    override fun downloadComplete(result: String) {
        mapJson = result    //for storage in shared preferences
        Utils.saveMapToSharedPrefs(this, downloadDate, mapJson)
        Log.d(tag, "[downloadComplete] downloaded new map, date: $downloadDate")
        getCoinsFromJson()
    }

    //parses the json file, creates Coin objects and adds them to the coins list
    //only adds coins that are still available for collection
    //called twice after user data finish downloading and after geoJson map is downloaded
    private fun getCoinsFromJson(){
        //check if both user data and geoJson map downloaded
        Log.d(tag, "[getCoinsFromJson] cc size: ${User.getCollectedCoins()?.size} mapJson ${mapJson.length} end")
        //check if resources ready or coins already set up
        if(User.getCollectedCoins() == null || mapJson == "" || coins.size > 0)
            return

        val fc = FeatureCollection.fromJson(mapJson)
        if(fc.features() != null) {
            //reading coin attributes from Json and creating a Coin object
            for (f: Feature in fc.features()!!) {
                if(f.properties() != null) {
                    val j: JsonObject = f.properties()!!
                    val id = j.get("id").asString
                    val value = j.get("value").asDouble
                    val currency = j.get("currency").asString
                    val markerSymbol = j.get("marker-symbol").asInt
                    val markerColor = j.get("marker-color").asString
                    val g: Geometry? = f.geometry()
                    val coordinates: LatLng
                    if (g is Point) {
                        coordinates = LatLng(g.latitude(), g.longitude())
                        //add coin to the list only if it hasn't been collected already
                        if (!User.getCollectedCoins()!!.contains(id))  //null checked already
                            coins.add(Coin(id, value, currency, markerSymbol, markerColor, coordinates))
                    }
                }
            }
        }

        displayInitialCoins()
    }

    //this funcion is run once after all asynctasks are done,
    //when map and coins are downloaded and location determined
    private fun displayInitialCoins() {
        if(originLocation!=null && coins.size>0) {
            displayCoinsInVisionRange(originLatLng!!)
        }
        Log.d(tag, "[displayInitialCoins] map $map, location: $originLocation, coins: ${coins.size}")
    }

    private fun displayCoinsInVisionRange(location: LatLng) {
        val towerMultiplier = towerBuff(location)                 //check if tower buff applies
        for(coin in coins) {
            val markerId = coinsMarkersMap[coin.id]
            val range = (User.getVisionRange() * towerMultiplier).toInt()
            if(coin.inRange(location, range)) {
                if(markerId == null)
                    drawMarker(coin)
            } else {
                if(markerId != null) {
                    removeMarker(coin)
                }
            }
        }
    }

    //check if user in the Tower, if yes then returns the vision multiplier (x2)
    private fun towerBuff(location: LatLng): Double {
        return if(Tower.userNearPlace(location)) {
            Tower.visionAmplifier
        } else {
            1.0
        }
    }

    //adds markers to the map, adds (coin id, marker id) to the coinsMarkersMap
    private fun drawMarker(coin: Coin){
        val iconResource = Utils.selectIcon(coin.currency, coin.value.toInt().toString())        //find coin resource file
        val icon: Icon = IconFactory.getInstance(this).fromResource(iconResource)       //icon of the marker
        //add marker to the map
        val marker: Marker? = map?.addMarker(MarkerOptions()
                .position(coin.coordinates)
                .title(coin.id)
                .icon(icon))
        if(marker == null) {
            Log.d(tag, "[drawMarkers] marker is null")
        } else {
            coinsMarkersMap[coin.id] = marker.id
        }
        Log.d(tag, "[drawMarkers] number of markers on the map ${map?.markers?.size}")
    }

    //removes marker representing the coin
    private fun removeMarker(coin:Coin) {
        val markerId: Long? = coinsMarkersMap[coin.id]
        if(markerId == null || map == null) {
            Log.d(tag, "[removeMarker]: map or marker id is null")
        } else {
            for (marker in map!!.markers) {
                if(markerId == marker.id) {
                    marker.remove()
                    break
                }
            }
        }
        coinsMarkersMap.remove(coin.id)
    }

    //collect the coin given it's id
    private fun collectCoin(coinId: String) {
        val coinsIterator = coins.iterator()
        for(coin in coinsIterator) {
            if (coin.id == coinId) {
                User.addCollectedCoin(firestore, coin)
                removeMarker(coin)          //remove the marker
                coinsIterator.remove()      //remove from coins list
                break                       //break, there is only one coin with given id
            }
        }
    }

    //detecting coins in range for automatic collection
    private fun collectCoinsInRange(location: LatLng){
        val coinsIterator = coins.iterator()
        for(coin in coinsIterator) {
            if(!checkSpaceInWallet())   //wallet full cant collect more coins
                break
            if (coin.inRange(location, collectRange)) {
                User.addCollectedCoin(firestore, coin)
                removeMarker(coin)
                coinsIterator.remove()
                Toast.makeText(this, "Coin collected!", Toast.LENGTH_SHORT).show()
            }
        }
        //Log.d(tag, "[checkCoinsInRange]: ${coins.size}")
    }

    private fun checkSpaceInWallet(): Boolean {
        return User.getWallet().size < User.getWalletCapacity()
    }


    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine?.requestLocationUpdates()
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Log.d(tag, "Permissions: $permissionsToExplain")
        // Present popup message or dialog
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        //check if user enabled autocollection which might have been changed in settings
        autocollection = Utils.getAutocollectionState(this)
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()

        //SIGSEGV
        locationEngine?.removeLocationEngineListener(this)
        locationEngine?.removeLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        Log.d(tag, "[onDestroy] called")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
        Log.d(tag, "[onLowMemory] called")
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if(outState != null) {
            mapView?.onSaveInstanceState(outState)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
