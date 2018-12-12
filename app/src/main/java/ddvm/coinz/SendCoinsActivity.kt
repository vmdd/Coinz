package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.android.synthetic.main.activity_send_coins.*

class SendCoinsActivity : AppCompatActivity() {

    private val tag = "SendCoinsActivity"

    private var firestore: FirebaseFirestore? = null

    private var userLastLocation: LatLng? = null

    private val exchangeRates = mutableMapOf<String,Double>()   //map storing exchange rates for coins
    private var checkedAll = false

    private lateinit var viewAdapter: CoinsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_coins)
        //don't show keyboard when activity is opened
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        userLastLocation = intent.getParcelableExtra(MainActivity.EXTRA_LOCATION)

        //cloud firestore
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        exchangeRates.putAll(Utils.getExchangeRates(this))

        viewManager = LinearLayoutManager(this)
        viewAdapter = CoinsAdapter(this, User.getWallet(), exchangeRates)

        coins_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }

        send_coins_button.setOnClickListener {
            when {
                userLastLocation == null -> Toast.makeText(this, getString(R.string.no_location), Toast.LENGTH_SHORT).show()
                //must be in bank to send coins
                !Bank.userNearPlace(userLastLocation!!) -> Toast.makeText(this,
                        getString(R.string.not_in_bank_for_send_coins), Toast.LENGTH_SHORT).show()
                //no spare change to send (daily limit of paid in coins not exhausted)
                User.getNPaidInCoins()<25 -> Toast.makeText(this,
                        getString(R.string.no_spare_change), Toast.LENGTH_SHORT).show()
                else -> checkRecipientValid()       //check if recipient valid and send coins
            }
        }
    }

    private fun checkRecipientValid() {
        val recipientUsername = field_recipient.text.toString().toLowerCase()
        //check if the user tries to send coins to themselves
        if(User.getUsername().toLowerCase() == recipientUsername) {
            field_recipient.error = "You can't send coins to yourself!"
            return
        }
        //check if user with given username exists
        Utils.checkUserExists(firestore,recipientUsername) { userExists, recipientDocument ->
            if(userExists) {
                for (document in recipientDocument!!)   //if user exists, then document exists as well
                    sendCoins(document.id, recipientUsername)      //send coin to recipient
            } else {
                field_recipient.error = "Recipient does not exist"
                Log.d(tag, "[checkRecipientExists] $recipientUsername")
            }
        }
    }

    private fun sendCoins(recipientUid: String, recipientUsername: String) {
        val firestoreRecipient = firestore
                ?.collection("${User.USERS_COLLECTION_KEY}/$recipientUid/${User.RECEIVED_COLLECTION_KEY}")

        val itemsStates = viewAdapter.getItemsStates()  //get which items are selected

        var nSent = 0 //number of coins sent

        //iterates from the end of the array, to remove items with higher index first
        for(i in itemsStates.size()-1 downTo 0) {
            //if the item is selected it is removed
            if(itemsStates.valueAt(i)) {
                val position = itemsStates.keyAt(i)    //index of the coin
                val coin = User.getWallet()[position]
                val giftId = coin.id + User.getUsername()           //modify coin id to include senders username
                val giftCoin = Coin(giftId, coin.value, coin.currency)     //coin with modified id
                //store coin in recipient's collection, set document id as coin id and sender's username
                firestoreRecipient?.document(giftCoin.id)?.set(giftCoin)
                User.removeCoinFromCollection(firestore,User.WALLET_COLLECTION_KEY,position)  //remove coin from senders wallet
                viewAdapter.notifyItemRemoved(position)

                nSent += 1 //increase number of coins sent by one
            }
        }
        if(nSent==0) {
            Toast.makeText(this, getString(R.string.no_coins_to_send),
                    Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this,"Sent $nSent coin(s) to $recipientUsername", Toast.LENGTH_SHORT).show()
        }
        viewAdapter.clearItemsStates()
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
                Utils.sortCoinsByGold(User.getWallet(), exchangeRates)
                viewAdapter.notifyDataSetChanged()
                true
            }
            R.id.sort_currency -> {
                Utils.sortCoinsByCurrency(User.getWallet(), exchangeRates)
                viewAdapter.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
