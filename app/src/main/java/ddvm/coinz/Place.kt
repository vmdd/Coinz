package ddvm.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

abstract class Place {
    abstract val placeName: String
    abstract val coordinates: LatLng
    abstract val placeMarkerResource: Int
}

//Santander bank on campus
class Bank: Place() {
    override val placeName = "Bank"
    override val coordinates = LatLng(55.945935, -3.188312)
    override val placeMarkerResource = R.drawable.bank
}