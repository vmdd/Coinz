package ddvm.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

//class for special places on the map
abstract class Place {
    abstract val placeName: String                      //name of the place
    abstract val coordinates: LatLng                    //coordinates of the place on the map
    abstract val placeMarkerResource: Int               //R.drawable resource of the icon of the place

    //checks if the user is near the place
    fun userNearPlace(location: LatLng): Boolean {
        return (location.distanceTo(coordinates) <= MainActivity.collectRange)
    }
}

//Santander bank on campus
object Bank: Place() {
    override val placeName = "Bank"
    override val coordinates = LatLng(55.945935, -3.188312)
    override val placeMarkerResource = R.drawable.bank
}

//Sainsbury's near campus
object Shop: Place() {
    override val placeName = "Shop"
    override val coordinates = LatLng(55.943891, -3.191785)
    override val placeMarkerResource = R.drawable.shop
}

// Appleton Tower
object Tower: Place() {
    override val placeName = "Tower"
    override val coordinates = LatLng(55.944418, -3.186842)
    override val placeMarkerResource = R.drawable.tower
    const val visionAmplifier: Double = 1.2
}