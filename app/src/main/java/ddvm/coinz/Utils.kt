package ddvm.coinz

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object Utils {
    //gets exchange rates and stores them in the map

    fun getExchangeRates(context: Context, preferencesFile: String): MutableMap<String,Double> {

        val prefsSettings = context.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val mapJson = prefsSettings.getString("mapJson","")

        val exchangeRates = mutableMapOf<String,Double>()
        val j: JsonObject = JsonParser().parse(mapJson).asJsonObject
        val rates = j.get("rates").asJsonObject
        exchangeRates["SHIL"] = rates.get("SHIL").asDouble
        exchangeRates["DOLR"] = rates.get("DOLR").asDouble
        exchangeRates["QUID"] = rates.get("QUID").asDouble
        exchangeRates["PENY"] = rates.get("PENY").asDouble

        return exchangeRates
    }

    //select icon representing the coin currency and value
    //if value of the coin is not passed, returns icon without number
    fun selectIcon(currency: String, displayValue: String = ""): Int {
        val select = currency + displayValue
        val icons = mutableMapOf<String, Int>()
        icons["PENY"] = R.drawable.red
        icons["PENY0"] = R.drawable.red_0
        icons["PENY1"] = R.drawable.red_1
        icons["PENY2"] = R.drawable.red_2
        icons["PENY3"] = R.drawable.red_3
        icons["PENY4"] = R.drawable.red_4
        icons["PENY5"] = R.drawable.red_5
        icons["PENY6"] = R.drawable.red_6
        icons["PENY7"] = R.drawable.red_7
        icons["PENY8"] = R.drawable.red_8
        icons["PENY9"] = R.drawable.red_9
        icons["DOLR"] = R.drawable.green
        icons["DOLR0"] = R.drawable.green_0
        icons["DOLR1"] = R.drawable.green_1
        icons["DOLR2"] = R.drawable.green_2
        icons["DOLR3"] = R.drawable.green_3
        icons["DOLR4"] = R.drawable.green_4
        icons["DOLR5"] = R.drawable.green_5
        icons["DOLR6"] = R.drawable.green_6
        icons["DOLR7"] = R.drawable.green_7
        icons["DOLR8"] = R.drawable.green_8
        icons["DOLR9"] = R.drawable.green_9
        icons["QUID"] = R.drawable.yellow
        icons["QUID0"] = R.drawable.yellow_0
        icons["QUID1"] = R.drawable.yellow_1
        icons["QUID2"] = R.drawable.yellow_2
        icons["QUID3"] = R.drawable.yellow_3
        icons["QUID4"] = R.drawable.yellow_4
        icons["QUID5"] = R.drawable.yellow_5
        icons["QUID6"] = R.drawable.yellow_6
        icons["QUID7"] = R.drawable.yellow_7
        icons["QUID8"] = R.drawable.yellow_8
        icons["QUID9"] = R.drawable.yellow_9
        icons["SHIL"] = R.drawable.blue
        icons["SHIL0"] = R.drawable.blue_0
        icons["SHIL1"] = R.drawable.blue_1
        icons["SHIL2"] = R.drawable.blue_2
        icons["SHIL3"] = R.drawable.blue_3
        icons["SHIL4"] = R.drawable.blue_4
        icons["SHIL5"] = R.drawable.blue_5
        icons["SHIL6"] = R.drawable.blue_6
        icons["SHIL7"] = R.drawable.blue_7
        icons["SHIL8"] = R.drawable.blue_8
        icons["SHIL9"] = R.drawable.blue_9
        return icons[select]!!
    }
}