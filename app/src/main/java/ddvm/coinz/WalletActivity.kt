package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.firestore.*
import com.mapbox.mapboxsdk.geometry.LatLng

import kotlinx.android.synthetic.main.activity_wallet.*

class WalletActivity : AppCompatActivity() {

    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var firestore: FirebaseFirestore? = null

    private val coins = mutableListOf<Coin>()                   //a copy of coins in user wallet
    private var checkedAll = false

    private var userLastLocation: LatLng? = null           //last location passed from MainActivity

    private lateinit var viewAdapter: CoinsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    companion object {
        const val dailyLimit = 25               //daily limit of coins to pay in
    }

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

        coins.addAll(User.getWallet())
        exchangeRates.putAll(Utils.getExchangeRates(this))

        viewManager = LinearLayoutManager(this)
        viewAdapter = CoinsAdapter(this, coins, exchangeRates)

        coins_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }

        discard_coin_button.setOnClickListener { discardSelectedCoins() }
        pay_in_button.setOnClickListener {
            when {
                userLastLocation == null -> Toast.makeText(this,
                        getString(R.string.no_location), Toast.LENGTH_SHORT).show()
                !Bank.userNearPlace(userLastLocation!!) -> Toast.makeText(this, getString(R.string.not_in_bank),
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
                User.removeCoinFromCollection(firestore,User.WALLET_COLLECTION_KEY,position)  //removes coin from the wallet
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
                User.removeCoinFromCollection(firestore,User.WALLET_COLLECTION_KEY,position)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.check_all -> {
                if(!checkedAll) {
                    //items are not all selected
                    viewAdapter.checkAllItems()         //check them all
                    viewAdapter.notifyDataSetChanged()
                    item.setIcon(R.drawable.ic_all_checked)     //change the icon
                }
                else {
                    viewAdapter.clearItemsStates()      //uncheck all
                    viewAdapter.notifyDataSetChanged()
                    item.setIcon(R.drawable.ic_select_all)      //change the icon
                }
                checkedAll=!checkedAll
                true
            }
            R.id.sort_gold -> {
                Utils.sortCoinsByGold(coins, exchangeRates)
                viewAdapter.notifyDataSetChanged()
                true
            }
            R.id.sort_currency -> {
                Utils.sortCoinsByCurrency(coins, exchangeRates)
                viewAdapter.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
