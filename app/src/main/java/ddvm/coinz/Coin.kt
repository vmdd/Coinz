package ddvm.coinz

data class Coin(val id:String,
                val value:Double,
                val currency:String,
                val markerSymbol:Int,
                val markerColor:String,
                val coordinates:List<Double>){
}