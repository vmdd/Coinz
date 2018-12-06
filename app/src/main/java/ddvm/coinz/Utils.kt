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
}