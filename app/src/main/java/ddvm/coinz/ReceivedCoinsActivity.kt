package ddvm.coinz


import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_received_coins.*

class ReceivedCoinsActivity : AppCompatActivity() {

    private var tag = "ReceivedCoinsActivity"

    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null

    private var checkedAll = false


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
        viewAdapter = ReceivedCoinsAdapter(this, User.getReceivedCoins(), exchangeRates)

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
        Log.d(tag, "[storeCoinsInBank] $itemsStates")
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
        User.removeCoinFromCollection(firestore,User.RECEIVED_COLLECTION_KEY,position)
        viewAdapter.notifyItemRemoved(position)
    }

    override fun onStart() {
        super.onStart()
        User.downloadReceivedCoins(firestore) {viewAdapter.notifyDataSetChanged()}
        //read shared preferences file and exchange rates for coins
        exchangeRates.putAll(Utils.getExchangeRates(this))
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
                Utils.sortCoinsByGold(User.getReceivedCoins(), exchangeRates)
                viewAdapter.notifyDataSetChanged()
                true
            }
            R.id.sort_currency -> {
                Utils.sortCoinsByCurrency(User.getReceivedCoins(), exchangeRates)
                viewAdapter.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
