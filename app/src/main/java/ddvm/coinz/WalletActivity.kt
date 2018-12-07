package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*

import kotlinx.android.synthetic.main.activity_wallet.*

class WalletActivity : AppCompatActivity() {

    private var tag = "WalletActivity"

    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null

    private val dailyLimit = 25                                 //daily limit of coins to pay in

    private lateinit var viewAdapter: CoinsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        mAuth = FirebaseAuth.getInstance()
        mUser = mAuth?.currentUser

        //cloud firestore
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        viewManager = LinearLayoutManager(this)
        viewAdapter = CoinsAdapter(this, User.getWallet())

        coins_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }

        discard_coin_button.setOnClickListener { discardSelectedCoins() }
        pay_in_button.setOnClickListener {
            if(checkPayInLimit())
                storeCoinsInBank()
            else {
                val toast = Toast.makeText(this,
                        "You can only pay in ${dailyLimit - User.getNPaidInCoins()} coin(s) more",
                        Toast.LENGTH_SHORT)
                toast.show()
            }
        }
        send_coins_button.setOnClickListener {
            if(User.getNPaidInCoins()<25) {
                Toast.makeText(this, "You can only send coins after storing 25 coins in the bank on a given day",
                        Toast.LENGTH_SHORT)
                        .show()
            } else {
                checkRecipientValid()
            }
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
                gold += User.getWallet()[position].toGold(exchangeRates)
                removeCoin(position)
                nStoredCoins += 1
            }
        }
        User.addGold(firestore, gold)            //amount of user's gold after converting coins to gold
        User.addPaidInCoins(firestore, nStoredCoins)

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

        return (dailyLimit - User.getNPaidInCoins() - nCoinsToPayIn >= 0)
    }

    private fun removeCoin(position: Int) {
        User.removeCoinFromWallet(firestore,position)
        viewAdapter.notifyItemRemoved(position)
    }

    private fun checkRecipientValid() {

        val recipientUsername = field_recipient.text.toString().toLowerCase()
        //check if the user tries to send coins to themselves
        if(User.getUsername().toLowerCase() == recipientUsername) {
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
                val coin = User.getWallet()[position]
                //store coin in recipient's collection, set document id as coin id and sender's username
                firestoreRecipient?.document(coin.id + User.getUsername())?.set(coin)
                removeCoin(position)
            }
        }
        viewAdapter.clearItemsStates()
    }

    override fun onStart() {
        super.onStart()

        //read shared preferences file and exchange rates for coins
        exchangeRates.putAll(Utils.getExchangeRates(this))
    }
}
