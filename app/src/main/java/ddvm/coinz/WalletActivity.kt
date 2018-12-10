package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import com.google.firebase.firestore.*
import com.mapbox.mapboxsdk.geometry.LatLng

import kotlinx.android.synthetic.main.activity_wallet.*

class WalletActivity : AppCompatActivity() {

    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var firestore: FirebaseFirestore? = null

    private val dailyLimit = 25                                 //daily limit of coins to pay in

    private lateinit var userLastLocation: LatLng            //last location passed from MainActivity

    private lateinit var viewAdapter: CoinsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        //get data passed in an Intent
        userLastLocation = intent.getParcelableExtra(MainActivity.EXTRA_LOCATION)

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
            when {
                !Bank.userNearPlace(userLastLocation) -> Toast.makeText(this, getString(R.string.not_in_bank),
                        Toast.LENGTH_SHORT).show()
                !checkPayInLimit() -> Toast.makeText(this, getString(R.string.limit_exhausted),
                        Toast.LENGTH_SHORT).show()
                else -> storeCoinsInBank()
            }
        }
    }

    //delete selected coins from the wallet
    private fun discardSelectedCoins() {
        val itemsStates = viewAdapter.getItemsStates()  //get which items are selected

        //iterates from the end of the array, to remove items with higher index first
        for(i in itemsStates.size()-1 downTo 0) {
            //if the item is selected it is removed
            if(itemsStates.valueAt(i)) {
                val position = itemsStates.keyAt(i)    //index of the coin
                User.removeCoinFromCollection(firestore,"wallet",position)  //removes coin from the wallet
                viewAdapter.notifyItemRemoved(position)
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
                //convert coin at given index in the wallet to gold and add to the gold count
                gold += User.getWallet()[position].toGold(exchangeRates)
                nStoredCoins += 1   //increase number od stored coins
                //remove coin from the wallet
                User.removeCoinFromCollection(firestore,"wallet",position)
                viewAdapter.notifyItemRemoved(position)
            }
        }

        if(nStoredCoins == 0) {
            //no coins selected
            Toast.makeText(this, getString(R.string.no_coins_to_pay_in_selected),
                    Toast.LENGTH_SHORT).show()
        } else {
            //pay in successful
            Toast.makeText(this, getString(R.string.transaction_successful), Toast.LENGTH_SHORT).show()
            User.addGold(firestore, gold)                   //update user's gold after transaction
            User.addPaidInCoins(firestore, nStoredCoins)    //update the number of stored coins in the bank today

        }

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

        //check if the transaction is within the daily limit
        return (dailyLimit - User.getNPaidInCoins() - nCoinsToPayIn >= 0)
    }

    override fun onStart() {
        super.onStart()

        //read shared preferences file and exchange rates for coins
        exchangeRates.putAll(Utils.getExchangeRates(this))
    }
}
