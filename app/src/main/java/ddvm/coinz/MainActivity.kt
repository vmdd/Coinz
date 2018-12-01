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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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
    private var firestoreWallet: CollectionReference? = null

    private var downloadDate = ""   //date of last downloaded map, format yyyy/MM/dd
    private val preferencesFile = "MyPrefsFile"
    private var mapJson = ""

    private val coins = mutableListOf<Coin>()  //list storing coins available for collection on the map
    //private val wallet = mutableListOf<Coin>()  //list storing collected coins
    private val coinsMarkersMap = mutableMapOf<String, Long>()  //map matching coins id with their marker's id

    private val collectRange: Int = 25         //range to collect coin in meters

    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //MapBox
        Mapbox.getInstance(this, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        //Firebase authentication
        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth?.currentUser

        //Cloud firestore
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings
        firestoreWallet = firestore?.collection("users")
                ?.document(mUser!!.uid)
                ?.collection("wallet")  //after login mUser shouldn't be null

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
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_map -> {
                startActivity(Intent(this, MainActivity::class.java))
            }
            R.id.nav_wallet -> {
                startActivity(Intent(this, WalletActivity::class.java))
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    //runs after geo-JSON map is downloaded
    override fun downloadComplete(result: String) {
        getCoinsFromJson(result)
        drawMarkers()
        mapJson = result    //for storage in shared preferences
    }

    //parses the json file, creates Coin objects and adds them to the coins list
    private fun getCoinsFromJson(json: String){
        val fc = FeatureCollection.fromJson(json)
        if(fc.features() != null) {
            //reading coin attributes from Json and creating a Coin object
            for (f: Feature in fc.features()!!) {
                val j: JsonObject = f.properties()!! //null?
                val id = j.get("id").asString
                val value = j.get("value").asDouble
                val currency = j.get("currency").asString
                val markerSymbol = j.get("marker-symbol").asInt
                val markerColor = j.get("marker-color").asString
                val g: Geometry? = f.geometry()
                val coordinates: LatLng
                if (g is Point) {
                    coordinates = LatLng(g.latitude(),g.longitude())
                    coins.add(Coin(id, value, currency, markerSymbol, markerColor, coordinates))
                }
            }
        }
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
            drawMarkers()
        }
    }

    //adds markers to the map, adds (coin id, marker id) to the coinsMarkersMap
    private fun drawMarkers(){
        for(coin in coins){
            val marker: Marker? = map?.addMarker(MarkerOptions().position(coin.coordinates).title(coin.id))
            if(marker == null) {
                Log.d(tag, "[drawMarkers] marker is null")
            } else {
                coinsMarkersMap[coin.id] = marker.id
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
        locationEngine.apply{
            interval = 5000 //every 5 seconds
            fastestInterval = 1000 //at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine.lastLocation
        if(lastLocation != null){
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        }else{
            locationEngine.addLocationEngineListener(this)
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
    }

    override fun onLocationChanged(location: Location?) {
        if(location == null){
            Log.d(tag, "[onLocationChanged] location is null")
        }else{
            originLocation = location
            setCameraPosition(originLocation)
            checkCoinsInRange(location)

        }
    }

    //detecting coins in range for collection
    private fun checkCoinsInRange(location: Location){
        val latLng = LatLng(location.latitude, location.longitude)  //latlng of user location
        val coinsIterator = coins.iterator()
        for(coin in coinsIterator) {
            if (coin.inRange(latLng, collectRange)) {
                firestoreWallet?.document(coin.id)?.set(coin)       //storing collected coins in firestore
                removeMarker(coin)
                coinsIterator.remove()
            }
        }
        //Log.d(tag, "[checkCoinsInRange]: ${coins.size}")
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
    }

    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
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

        //download geo-json map or read from shared preferences
        val curDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val dateFormatted = curDate.format(formatter)   //current date
        val prefsSettings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        downloadDate = prefsSettings.getString("lastDownloadDate", "")  //last download date
        //check if map for a given day already downloaded, else download it
        if(dateFormatted == downloadDate){
            mapJson = prefsSettings.getString("mapJson","")
            getCoinsFromJson(mapJson)
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

        Log.d(tag, "[onStop] Storing lastDownloadDate of $downloadDate")
        //saving download date and mapJson in shared preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("mapJson", mapJson)
        editor.apply()
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
