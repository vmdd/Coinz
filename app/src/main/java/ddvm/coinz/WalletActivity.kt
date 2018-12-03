package ddvm.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import kotlinx.android.synthetic.main.activity_wallet.*

class WalletActivity : AppCompatActivity() {

    private var tag = "WalletActivity"

    private var wallet = mutableListOf<Coin>()   //coins collected by the user currently in the wallet
    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var firestoreWallet: CollectionReference? = null

    private val preferencesFile = "MyPrefsFile"
    private var mapJson = ""

    private lateinit var viewAdapter: CoinsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        //dummy coins for testing
        //coins.add(Coin("123",2.0 , "GOLD" , 5, "doesnt batter", LatLng(0.0,0.0)))
        //coins.add(Coin("133",3.0 , "PPP" , 5, "doesnt batter", LatLng(0.0,0.0)))

        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth?.currentUser

        //cloud firestore
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings
        firestoreWallet = firestore?.collection("users")
                ?.document(mUser!!.uid)
                ?.collection("wallet")

        viewManager = LinearLayoutManager(this)
        viewAdapter = CoinsAdapter(wallet)

        coins_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }

        fetchWallet()   //get the content of the wallet from cloud firestore

        discard_coin_button.setOnClickListener { discardSelectedCoins() }
    }

    //gets the content of the wallet from firestore and stores it in the wallet list
    private fun fetchWallet() {
        firestoreWallet
                ?.get()
                ?.addOnSuccessListener {result ->
                    for(document in result) {
                        val coin = document.toObject(Coin::class.java)
                        wallet.add(coin)
                        viewAdapter.notifyItemInserted(wallet.size - 1)  //updates the recycler view with new coin
                    }
                }
                ?.addOnFailureListener {exception ->
                    Log.w(tag, "[fetchWallet] Error getting documents: ", exception)
                }
    }

    //gets exchange rates and stores them in the map
    private fun getExchangeRates(json: String) {
        val j: JsonObject = JsonParser().parse(json).asJsonObject
        val rates = j.get("rates").asJsonObject
        exchangeRates["SHIL"] = rates.get("SHIL").asDouble
        exchangeRates["DOLR"] = rates.get("DOLR").asDouble
        exchangeRates["QUID"] = rates.get("QUID").asDouble
        exchangeRates["PENY"] = rates.get("PENY").asDouble
    }

    private fun discardSelectedCoins() {
        val itemsStates = viewAdapter.getItemsStates()  //get which items are selected

        //iterates from the end of the array, to remove items with higher index first
        for(i in itemsStates.size()-1 downTo 0) {
            //if the item is selected it is removed
            if(itemsStates.valueAt(i)) {
                val position = itemsStates.keyAt(i)    //index of the coin
                val coinId = wallet[position].id    //id of the selected coin
                wallet.removeAt(position)
                viewAdapter.notifyItemRemoved(position)

                //remove the coin from firestore
                firestoreWallet?.document(coinId)
                        ?.delete()
                        ?.addOnSuccessListener { Log.d(tag, "[discardSelectedCoins] coin deleted") }
                        ?.addOnFailureListener { e -> Log.d(tag, "[discardSelectedCoins] error deleting document", e) }
            }
        }
        viewAdapter.clearItemsStates()  //clears the states since no items are selected now
    }

    override fun onStart() {
        super.onStart()

        //read shared preferences file and exchange rates for coins
        val prefsSettings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        mapJson = prefsSettings.getString("mapJson","")
        getExchangeRates(mapJson)
    }
}
