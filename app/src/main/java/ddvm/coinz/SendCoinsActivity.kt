package ddvm.coinz

import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.android.synthetic.main.activity_send_coins.*

class SendCoinsActivity : AppCompatActivity() {

    private val tag = "SendCoinsActivity"

    private var firestore: FirebaseFirestore? = null

    private lateinit var userLastLocation: Location

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

        viewManager = LinearLayoutManager(this)
        viewAdapter = CoinsAdapter(this, User.getWallet())

        coins_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }

        send_coins_button.setOnClickListener {
            when {
                //must be in bank to send coins
                !checkSendCoinsRange() -> Toast.makeText(this,
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

    private fun checkSendCoinsRange(): Boolean {
        val latLng = LatLng(userLastLocation.latitude, userLastLocation.longitude)
        //check if in range of the bank (same as collection range
        return latLng.distanceTo(Bank().coordinates) <= MainActivity.collectRange
    }

    private fun sendCoins(recipientUid: String, recipientUsername: String) {
        val firestoreRecipient = firestore?.collection("users/$recipientUid/received_coins")

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
                User.removeCoinFromCollection(firestore,"wallet",position)  //remove coin from senders wallet
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
}
