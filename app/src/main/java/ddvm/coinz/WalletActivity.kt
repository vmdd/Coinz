package ddvm.coinz

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import kotlinx.android.synthetic.main.activity_wallet.*

class WalletActivity : AppCompatActivity() {

    private var tag = "WalletActivity"

    private var wallet = mutableListOf<Coin>()                  //coins collected by the user currently in the wallet
    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var firestoreWallet: CollectionReference? = null    //collection storing user's coins in the wallet
    private var firestoreUser: DocumentReference? = null        //user document

    private val preferencesFile = "MyPrefsFile"
    private var mapJson = ""

    private var username = ""
    private var currentGold = 0.0                               //to store user's current gold
    private var nPaidInCoins = 0                                //to keep track of number of coins paid in today

    private val dailyLimit = 25                                 //daily limit of coins to pay in

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
        firestoreUser = firestore?.collection("users")
                ?.document(mUser!!.uid)  //after login mUser shouldn't be null
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

        getUserData()   //download user data from firestore
        fetchWallet()   //get the content of the wallet from cloud firestore

        discard_coin_button.setOnClickListener { discardSelectedCoins() }
        pay_in_button.setOnClickListener {
            if(checkPayInLimit())
                storeCoinsInBank()
            else {
                val toast = Toast.makeText(this,
                        "You can only pay in ${dailyLimit - nPaidInCoins} coin(s) more",
                        Toast.LENGTH_SHORT)
                toast.show()
            }
        }
        send_coins_button.setOnClickListener {
            if(nPaidInCoins<25) {
                Toast.makeText(this, "You can only send coins after storing 25 coins in the bank on a given day",
                        Toast.LENGTH_SHORT)
                        .show()
            } else {
                checkRecipientValid()
            }
        }
    }

    //get user's current gold and number of coins paid in today
    private fun getUserData() {
        firestoreUser?.get()
                ?.addOnSuccessListener { document ->
                    if(document.getDouble("gold")!=null) {
                        currentGold = document.getDouble("gold")!!
                    }
                    if(document.getDouble("n_paid_in_coins")!=null){
                        nPaidInCoins = document.getDouble("n_paid_in_coins")!!.toInt()
                    }
                    if(document.getString("username")!=null) {
                        username = document.getString("username")!!

                    }
                }
                ?.addOnFailureListener { e -> Log.d(tag, "[storeCoinsInBank] get failed with ", e) }
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

    private fun convertToGold(coin: Coin): Double {
        return if(exchangeRates[coin.currency]!=null) {
            coin.value * exchangeRates[coin.currency]!!
        } else {
            Log.d(tag, "[convertToGold()] unknown currency")
            0.0
        }
    }

    private fun discardSelectedCoins() {
        val itemsStates = viewAdapter.getItemsStates()  //get which items are selected

        //iterates from the end of the array, to remove items with higher index first
        for(i in itemsStates.size()-1 downTo 0) {
            //if the item is selected it is removed
            if(itemsStates.valueAt(i)) {
                val position = itemsStates.keyAt(i)    //index of the coin
                removeCoin(position)
            }
        }
        viewAdapter.clearItemsStates()  //clears the states since no items are selected now
    }

    //deletes the item from the view and firestore, and increases amount of user gold accordingly
    private fun storeCoinsInBank() {
        val itemsStates = viewAdapter.getItemsStates()  //get which items are selected
        var gold = 0.0    //for storing gold gained from coin conversion
        var nStoredCoins = 0    //counter for stored coins
        //iterates from the end of the array, to remove items with higher index first
        for(i in itemsStates.size()-1 downTo 0) {
            if(itemsStates.valueAt(i)) {
                val position = itemsStates.keyAt(i)    //index of the coin
                gold += convertToGold(wallet[position])
                removeCoin(position)
                nStoredCoins += 1
            }
        }
        currentGold += gold             //amount of user's gold after converting coins to gold
        nPaidInCoins += nStoredCoins    //updating number of coins paid in today

        firestoreUser?.update("gold", currentGold,
                "n_paid_in_coins", nPaidInCoins)
                ?.addOnSuccessListener { Log.d(tag, "[storeCoinsInBank] document updated successfully") }
                ?.addOnFailureListener { e -> Log.d(tag, "[storeCoinsInBank] error updating document", e) }

        viewAdapter.clearItemsStates()
    }

    //check if the limit allows the user to pay in selected coins
    private fun checkPayInLimit(): Boolean {
        val itemsStates = viewAdapter.getItemsStates()
        var nCoinsToPayIn = 0   //counter for number of coins selected
        for(i in 0 until itemsStates.size()){
            if(itemsStates.valueAt(i))
                nCoinsToPayIn++
        }

        return (dailyLimit - nPaidInCoins - nCoinsToPayIn >= 0)
    }

    private fun removeCoin(position: Int) {
        val coinId = wallet[position].id    //id of the selected coin
        wallet.removeAt(position)
        viewAdapter.notifyItemRemoved(position)

        //remove the coin from firestore
        firestoreWallet?.document(coinId)
                ?.delete()
                ?.addOnSuccessListener { Log.d(tag, "[removeCoin] coin removed from database") }
                ?.addOnFailureListener { e -> Log.d(tag, "[removeCoin] error deleting coin document", e) }
    }

    private fun checkRecipientValid() {

        val recipientUsername = field_recipient.text.toString().toLowerCase()
        //check if the user tries to send coins to themselves
        if(username.toLowerCase() == recipientUsername) {
            field_recipient.error = "You can't send coins to yourself!"
            return
        }
        //check if user with given username exists
        firestore?.collection("users")
                ?.whereEqualTo("lowercase_username", recipientUsername)        //querying for documents with same username
                ?.get()
                ?.addOnSuccessListener { documents ->
                    //if documents is empty then recipient with given username does not exist
                    if(documents.isEmpty) {
                        field_recipient.error = "Recipient does not exist"
                        Log.d(tag, "[checkRecipientExists] $recipientUsername")
                    } else {
                        for(document in documents) {
                            sendCoins(document.id)                     //recipient exists, send coins and pass recipients uid
                        }
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.d(tag, "[checkRecipientExists] error getting documents ", e)
                }
    }

    private fun sendCoins(recipientUid: String) {
        val firestoreRecipient = firestore?.collection("users/$recipientUid/received_coins")

        val itemsStates = viewAdapter.getItemsStates()  //get which items are selected

        //iterates from the end of the array, to remove items with higher index first
        for(i in itemsStates.size()-1 downTo 0) {
            //if the item is selected it is removed
            if(itemsStates.valueAt(i)) {
                val position = itemsStates.keyAt(i)    //index of the coin
                val coin = wallet[position]
                //store coin in recipient's collection, set document id as coin id and sender's username
                firestoreRecipient?.document(coin.id + username)?.set(coin)
                removeCoin(position)
            }
        }
        viewAdapter.clearItemsStates()
    }

    override fun onStart() {
        super.onStart()

        //read shared preferences file and exchange rates for coins
        val prefsSettings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        mapJson = prefsSettings.getString("mapJson","")
        getExchangeRates(mapJson)
    }
}
