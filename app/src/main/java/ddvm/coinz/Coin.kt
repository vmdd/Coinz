package ddvm.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

// data class for coins
data class Coin(val id:String,
                val value:Double,
                val currency:String,
                val markerSymbol:Int,
                val markerColor:String,
                val coordinates:LatLng){

    // function checks if the coin is in certain range from some other point given it's coordinates
    fun inRange(latLng: LatLng, range: Int) :Boolean {
        return (coordinates.distanceTo(latLng) <= range)
    }
}