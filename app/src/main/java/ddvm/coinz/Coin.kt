package ddvm.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

// data class for coins
data class Coin(val id:String = "",
                val value:Double = 0.0,
                val currency:String = "",
                val coordinates:LatLng = LatLng(0.0,0.0)){

    // function checks if the coin is in certain range from some other point given it's coordinates
    fun inRange(latLng: LatLng, range: Int) :Boolean {
        return (coordinates.distanceTo(latLng) <= range)
    }

    //given the exchange rates calculates the value of coin in gold
    fun toGold(exchangeRates: MutableMap<String,Double>): Double {
        return if(exchangeRates[currency]!=null) {
            value * exchangeRates[currency]!!
        } else {
            0.0
        }
    }
}