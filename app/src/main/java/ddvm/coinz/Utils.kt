package ddvm.coinz

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Utils {
    //gets exchange rates and stores them in the map
    private val tag = "Utils"
    private const val preferencesFile = "MyPrefsFile"

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
        firestore?.collection(User.USERS_COLLECTION_KEY)
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

    //select colors resources for icon in the lists
    fun selectColorForIcon(currency: String): Int {
        return when(currency) {
            "PENY" -> android.R.color.holo_red_light
            "DOLR" -> android.R.color.holo_green_light
            "QUID" -> android.R.color.holo_orange_light
            "SHIL" -> android.R.color.holo_blue_bright
            else   -> android.R.color.transparent
        }
    }

    //select icon representing the coin currency and value
    //if value of the coin is not passed, returns icon without number
    fun selectIcon(currency: String, displayValue: String = ""): Int {
        val select = currency + displayValue
        return when (select) {
            "PENY" -> R.drawable.red
            "PENY0" -> R.drawable.red_0
            "PENY1" -> R.drawable.red_1
            "PENY2" -> R.drawable.red_2
            "PENY3" -> R.drawable.red_3
            "PENY4" -> R.drawable.red_4
            "PENY5" -> R.drawable.red_5
            "PENY6" -> R.drawable.red_6
            "PENY7" -> R.drawable.red_7
            "PENY8" -> R.drawable.red_8
            "PENY9" -> R.drawable.red_9
            "DOLR" -> R.drawable.green
            "DOLR0" -> R.drawable.green_0
            "DOLR1" -> R.drawable.green_1
            "DOLR2" -> R.drawable.green_2
            "DOLR3" -> R.drawable.green_3
            "DOLR4" -> R.drawable.green_4
            "DOLR5" -> R.drawable.green_5
            "DOLR6" -> R.drawable.green_6
            "DOLR7" -> R.drawable.green_7
            "DOLR8" -> R.drawable.green_8
            "DOLR9" -> R.drawable.green_9
            "QUID" -> R.drawable.yellow
            "QUID0" -> R.drawable.yellow_0
            "QUID1" -> R.drawable.yellow_1
            "QUID2" -> R.drawable.yellow_2
            "QUID3" -> R.drawable.yellow_3
            "QUID4" -> R.drawable.yellow_4
            "QUID5" -> R.drawable.yellow_5
            "QUID6" -> R.drawable.yellow_6
            "QUID7" -> R.drawable.yellow_7
            "QUID8" -> R.drawable.yellow_8
            "QUID9" -> R.drawable.yellow_9
            "SHIL" -> R.drawable.blue
            "SHIL0" -> R.drawable.blue_0
            "SHIL1" -> R.drawable.blue_1
            "SHIL2" -> R.drawable.blue_2
            "SHIL3" -> R.drawable.blue_3
            "SHIL4" -> R.drawable.blue_4
            "SHIL5" -> R.drawable.blue_5
            "SHIL6" -> R.drawable.blue_6
            "SHIL7" -> R.drawable.blue_7
            "SHIL8" -> R.drawable.blue_8
            "SHIL9" -> R.drawable.blue_9
            else -> R.drawable.blue     //most likely never end up here
        }
    }

    //formats gold to fit on the screen
    fun formatGold(gold:Double) : String {
        return when {
            gold >= 1000000000 -> String.format("%.1fB", gold.div(1000000000))
            gold >= 10000000 -> gold.div(1000000).toInt().toString() + 'M'
            gold >= 10000 -> gold.div(1000).toInt().toString() + 'k'
            else -> String.format("%.0f", gold)
        }
    }

    fun sortCoinsByGold(coins: MutableList<Coin>, exchangeRates: MutableMap<String, Double>) {
        coins.sortWith(compareByDescending { it.toGold(exchangeRates) })
    }

    fun sortCoinsByCurrency(coins: MutableList<Coin>, exchangeRates: MutableMap<String, Double>) {
        coins.sortWith(compareBy<Coin>{it.currency}.thenByDescending {it.toGold(exchangeRates)})
    }

    //check if day changed (midnight)
    fun checkDayChanged(curDate: LocalDate): Boolean {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val dateFormatted = curDate.format(formatter)   //current date
        Log.d(tag,"[checkDayChanged] user last play date ${User.getLastPlayDate()}")
        Log.d(tag,"[checkDayChanged] current date $dateFormatted")
        return (User.getLastPlayDate()!=dateFormatted)
    }
}