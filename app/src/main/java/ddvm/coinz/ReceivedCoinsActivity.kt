package ddvm.coinz


import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_received_coins.*

class ReceivedCoinsActivity : AppCompatActivity() {

    private var tag = "WalletActivity"

    private var receivedCoins = mutableListOf<Coin>()           //coins received from other users
    private val senders = mutableListOf<String>()
    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var firestoreReceived: CollectionReference? = null    //collection storing received coins
    private var firestoreUser: DocumentReference? = null        //user document

    private var currentGold = 0.0                               //to store user's current gold

    private val preferencesFile = "MyPrefsFile"

    private lateinit var viewAdapter: CoinsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_received_coins)

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
        firestoreReceived = firestore?.collection("users")
                ?.document(mUser!!.uid)
                ?.collection("received_coins")

        viewManager = LinearLayoutManager(this)
        viewAdapter = ReceivedCoinsAdapter(this, receivedCoins, senders)

        coins_received_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }

        getUserData()
        fetchReceivedCoins()

        discard_coin_button.setOnClickListener { discardSelectedCoins() }
        pay_in_button.setOnClickListener {
                storeCoinsInBank()
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
        //iterates from the end of the array, to remove items with higher index first
        for(i in itemsStates.size()-1 downTo 0) {
            if(itemsStates.valueAt(i)) {
                val position = itemsStates.keyAt(i)    //index of the coin
                gold += receivedCoins[position].toGold(exchangeRates)
                removeCoin(position)
            }
        }
        currentGold += gold             //amount of user's gold after converting coins to gold

        firestoreUser?.update("gold", currentGold)
                ?.addOnSuccessListener { Log.d(tag, "[storeCoinsInBank] document updated successfully") }
                ?.addOnFailureListener { e -> Log.d(tag, "[storeCoinsInBank] error updating document", e) }

        viewAdapter.clearItemsStates()
    }

    private fun removeCoin(position: Int) {
        val coinId = receivedCoins[position].id    //id of the selected coin
        receivedCoins.removeAt(position)
        senders.removeAt(position)
        viewAdapter.notifyItemRemoved(position)

        //remove the coin from firestore
        firestoreReceived?.document(coinId)
                ?.delete()
                ?.addOnSuccessListener { Log.d(tag, "[removeCoin] coin removed from database") }
                ?.addOnFailureListener { e -> Log.d(tag, "[removeCoin] error deleting coin document", e) }
    }

    //get user's current gold and username
    private fun getUserData() {
        firestoreUser?.get()
                ?.addOnSuccessListener { document ->
                    if(document.getDouble("gold")!=null) {
                        currentGold = document.getDouble("gold")!!
                    }
                }
                ?.addOnFailureListener { e -> Log.d(tag, "[storeCoinsInBank] get failed with ", e) }
    }

    //gets the content of the wallet from firestore and stores it in the wallet list
    private fun fetchReceivedCoins() {
        receivedCoins.clear()
        firestoreReceived
                ?.get()
                ?.addOnSuccessListener {result ->
                    for(document in result) {
                        val coin = document.toObject(Coin::class.java)
                        val senderId = document.id.substring(29)
                        senders.add(senderId)
                        receivedCoins.add(coin)
                        viewAdapter.notifyItemInserted(receivedCoins.size - 1)  //updates the recycler view with new coin
                    }
                }
                ?.addOnFailureListener {exception ->
                    Log.w(tag, "[fetchWallet] Error getting documents: ", exception)
                }
    }

    override fun onStart() {
        super.onStart()

        //read shared preferences file and exchange rates for coins
        exchangeRates.putAll(Utils.getExchangeRates(this, preferencesFile))
    }
}
