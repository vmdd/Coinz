package ddvm.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

data class Coin(val id:String,
                val value:Double,
                val currency:String,
                val markerSymbol:Int,
                val markerColor:String,
                val coordinates:LatLng){
}