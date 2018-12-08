package ddvm.coinz


import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_received_coins.*

class ReceivedCoinsActivity : AppCompatActivity() {

    //private var tag = "WalletActivity"

    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null


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

        viewManager = LinearLayoutManager(this)
        viewAdapter = ReceivedCoinsAdapter(this, User.getReceivedCoins())

        coins_received_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }

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
                gold += User.getReceivedCoins()[position].toGold(exchangeRates)
                removeCoin(position)
            }
        }
        User.addGold(firestore, gold)             //amount of user's gold after converting coins to gold

        viewAdapter.clearItemsStates()
    }

    private fun removeCoin(position: Int) {
        //remove the coin from firestore and User received coins
        User.removeCoinFromCollection(firestore,"received_coins",position)
        viewAdapter.notifyItemRemoved(position)
    }

    /*
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
    }*/

    override fun onStart() {
        super.onStart()
        User.downloadReceivedCoins(firestore) {viewAdapter.notifyDataSetChanged()}
        //read shared preferences file and exchange rates for coins
        exchangeRates.putAll(Utils.getExchangeRates(this))
    }

    private fun downloadComplete() {}
}
