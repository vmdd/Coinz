package ddvm.coinz

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object Utils {
    //gets exchange rates and stores them in the map
    private val tag = "Utils"
    private val preferencesFile = "MyPrefsFile"

    //gets exchange rates from geojson stored in shared preferences
    fun getExchangeRates(context: Context): MutableMap<String,Double> {

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

    //saves geojson to the shared prefs
    fun saveMapToSharedPrefs(context: Context, downloadDate: String, mapJson: String) {
        Log.d(tag, "[saveMapToSharedPrefs] Storing lastDownloadDate of $downloadDate")
        //saving download date and mapJson in shared preferences
        val settings = context.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("mapJson", mapJson)
        editor.apply()
    }

    //gets last download date of the map stored in shared prefs
    fun getLastDownloadDateFromSharedPrefs (context: Context): String {
        val settings = context.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val downloadDate = settings.getString("lastDownloadDate", "")  //last download date
        return downloadDate
    }

    //gets the map in shared prefs
    fun getMapFromSharedPrefs (context: Context): String {
        val settings = context.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val mapJson = settings.getString("mapJson","")
        return mapJson
    }

    fun saveAutocollectionState(context: Context, autocollection: Boolean) {
        val settings = context.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putBoolean("autocollection", autocollection)
        editor.apply()
    }

    fun getAutocollectionState(context: Context): Boolean {
        val settings = context.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        return settings.getBoolean("autocollection",false)
    }

    fun checkUserExists(firestore: FirebaseFirestore?, username: String, checkComplete: (Boolean, QuerySnapshot?) -> Unit) {
        firestore?.collection("users")
                ?.whereEqualTo("lowercase_username", username.toLowerCase())        //querying for documents with same username
                ?.get()
                ?.addOnSuccessListener { documents ->
                    //if documents is empty then user with given username does not exists
                    if(documents.isEmpty) {
                        checkComplete(false, null)
                    } else {
                        //user with given username exists
                        checkComplete(true, documents)
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.d(tag, "[checkUserExists] error getting documents ", e)
                }
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