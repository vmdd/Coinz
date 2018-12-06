package ddvm.coinz

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationEngineListener,
        PermissionsListener, DownloadCompleteListener, NavigationView.OnNavigationItemSelectedListener {

    private val tag = "MainActivity"

    private var mapView: MapView? = null
    private var map: MapboxMap? = null

    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var firestoreUser: DocumentReference? = null        //user document
    private var firestoreWallet: CollectionReference? = null    //collection storing user's coins in the wallet

    private var downloadDate = ""   //date of last downloaded map, format yyyy/MM/dd
    private val preferencesFile = "MyPrefsFile"
    private var mapJson = ""        //downloaded geo-json map
    private var dateFormatted = ""   //current date formated as string

    private val coins = mutableListOf<Coin>()  //list storing coins available for collection on the map
    private var collectedCoins: MutableList<*>? = null  //coins already collected, not available for collection
    private val coinsMarkersMap = mutableMapOf<String, Long>()  //map matching coins id with their marker's id

    private val collectRange: Int = 25         //range to collect coin in meters
    private val visionRange: Int = 50          //renge to see coin

    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private var locationEngine: LocationEngine? = null
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //Firebase authentication
        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth?.currentUser

        //Cloud firestore
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings
        firestoreUser = firestore?.collection("users")?.document(mUser!!.uid)  //after login mUser shouldn't be null
        firestoreWallet = firestoreUser?.collection("wallet")

        //MapBox
        Mapbox.getInstance(this, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        //Navigation drawer
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)  //toggle to open nav drawer
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    //handling item selections from navigation menu
    //starts chosen activity
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_wallet -> {
                startActivity(Intent(this, WalletActivity::class.java))
            }
            R.id.nav_received_coins -> {
                startActivity(Intent(this, ReceivedCoinsActivity::class.java))
            }
            R.id.nav_leaderboard -> {
                startActivity(Intent(this, LeaderboardActivity::class.java))
            }
            //sign the user out
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()    //sign out the user from the current session
                finish()
                startActivity(Intent(this,LoginActivity::class.java))   //go to login screen
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    //runs after geo-JSON map is downloaded
    override fun downloadComplete(result: String) {
        mapJson = result    //for storage in shared preferences
        downloadUserData()
        saveMapToSharedPrefs()
    }

    private fun saveMapToSharedPrefs() {
        Log.d(tag, "[saveMapToSharedPrefs] Storing lastDownloadDate of $downloadDate")
        //saving download date and mapJson in shared preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("mapJson", mapJson)
        editor.apply()
    }

    //downloads id of coins that already have been collected on given day
    //clears out the list of coins collected previously and resets the daily limit of coins to pay in the bank
    private fun downloadUserData() {
        //load user data from firestore
        firestoreUser?.get()
                ?.addOnSuccessListener { document ->
                    if(document != null && document.exists()) {
                        //if firestore stores collected coins from the current day, then get them and store in collectedCoins
                        //else set the day to current and clear the collected_coins array in firestore
                        collectedCoins = if(document.data?.get("last_play_date") == dateFormatted) {
                            document.data?.get("collected_coins") as? MutableList<*>
                        } else {
                            firestoreUser
                                    ?.update("last_play_date", dateFormatted,
                                            "collected_coins", emptyList<String>(), //no coins collected today
                                            "n_paid_in_coins", 0)     //no coins paid in today yet
                            mutableListOf<String>() //setting collectedCoins to empty list since all coins will be available on the map
                        }
                        //generate list of coins available to be collected
                        getCoinsFromJson(mapJson)
                    } else {
                        Log.d(tag, "[onStart] no user document")
                    }
                }
                ?.addOnFailureListener { exception ->
                    Log.d(tag, "[onStart] get failed with ", exception)
                }
    }

    //parses the json file, creates Coin objects and adds them to the coins list
    //only adds coins that are still available for collection
    private fun getCoinsFromJson(json: String){
        if(collectedCoins == null)
            return
        val fc = FeatureCollection.fromJson(json)
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
                        if (!collectedCoins!!.contains(id))  //null checked already
                            coins.add(Coin(id, value, currency, markerSymbol, markerColor, coordinates))
                    }
                }
            }
        }

        //drawMarkers()
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
            //add markers to the map
            //drawMarkers()
        }
    }

    //adds markers to the map, adds (coin id, marker id) to the coinsMarkersMap
    private fun drawMarker(coin: Coin){
        val icons = mutableMapOf<String,Int>()
        icons["PENY"] = R.drawable.circle_red
        icons["DOLR"] = R.drawable.circle_green
        icons["QUID"] = R.drawable.circle_yellow
        icons["SHIL"] = R.drawable.circle_blue
        val icon: Icon = IconFactory.getInstance(this).fromResource(icons[coin.currency]!!)
        val marker: Marker? = map?.addMarker(MarkerOptions().position(coin.coordinates).title(coin.id).icon(icon))
        if(marker == null) {
            Log.d(tag, "[drawMarkers] marker is null")
        } else {
            coinsMarkersMap[coin.id] = marker.id
        }
        Log.d(tag, "[drawMarkers] number of markers on the map ${map?.markers?.size}")
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
            setCameraPosition(lastLocation)
        }else{
            locationEngine?.addLocationEngineListener(this)
        }
    }

    private fun setCameraPosition(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
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
            setCameraPosition(originLocation)
            //check if the map is ready and coins are downloaded. If there are no coins, then nothing to do either
            if(map != null && coins.size > 0) {
                checkCoinsInVisionRange(location)
                checkCoinsInRange(location)
            }
        }
    }

    //detecting coins in range for collection
    private fun checkCoinsInRange(location: Location){
        val latLng = LatLng(location.latitude, location.longitude)  //latlng of user location
        val coinsIterator = coins.iterator()
        for(coin in coinsIterator) {
            if (coin.inRange(latLng, collectRange)) {
                firestoreWallet?.document(coin.id)?.set(coin)       //storing collected coins in wallet
                //note which coins collected already, to not display them in the future
                firestoreUser?.update("collected_coins", FieldValue.arrayUnion(coin.id))
                removeMarker(coin)
                coinsIterator.remove()
            }
        }
        //Log.d(tag, "[checkCoinsInRange]: ${coins.size}")
    }

    private fun checkCoinsInVisionRange(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)  //latlng of user location
        for(coin in coins) {
            val markerId = coinsMarkersMap[coin.id]
            if(coin.inRange(latLng, visionRange)) {
                if(markerId == null)
                    drawMarker(coin)
            } else {
                if(markerId != null) {
                    removeMarker(coin)
                }
            }
        }
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

        //download map or read from shared prefs
        val prefsSettings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        downloadDate = prefsSettings.getString("lastDownloadDate", "")  //last download date
        //check if map for a given day already downloaded, else download it
        if(dateFormatted == downloadDate){
            mapJson = prefsSettings.getString("mapJson","")
            downloadUserData()  //map is already downloaded, so download user data cointaining coins already collected on that day
        } else{
            downloadDate = dateFormatted
            val url = "http://homepages.inf.ed.ac.uk/stg/coinz/$dateFormatted/coinzmap.geojson"
            DownloadFileTask(this).execute(url)
        }


    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
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
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
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
            else -> super.onOptionsItemSelected(item)
        }
    }

}
