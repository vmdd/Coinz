package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.android.synthetic.main.activity_shop.*

class ShopActivity : AppCompatActivity() {

    private var items = listOf(Binoculars, Bag, Glasses)            //list of items available in the shop

    private var firestore: FirebaseFirestore? = null

    private var userLastLocation: LatLng? = null                    //last location of the user passed from the main activity

    private lateinit var viewAdapter: ItemsAdapter                  //recyclerview adapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        //get data passed in an Intent
        userLastLocation = intent.getParcelableExtra(MainActivity.EXTRA_LOCATION)

        //cloud firestore
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        viewManager = LinearLayoutManager(this)
        //pass context, and item list for the adapter to display as well as click listener
        viewAdapter = ItemsAdapter(this, items) { item, position ->
            //click listener for listening to "buy" button
            when {
                //location not available
                userLastLocation == null -> Toast.makeText(this,
                        getString(R.string.no_location), Toast.LENGTH_SHORT).show()
                //not in the shop
                !Shop.userNearPlace(userLastLocation!!) -> Toast.makeText(this,
                        getString(R.string.not_in_shop),
                        Toast.LENGTH_SHORT).show()
                //not enough gold to buy item
                !checkEnoughGold(item) -> Toast.makeText(this,
                        getString(R.string.not_enough_gold),
                        Toast.LENGTH_SHORT).show()
                //conditions to buy passed, buy the item
                else -> {
                    item.buy(firestore)   //equip item
                    viewAdapter.notifyItemChanged(position) //update the data at the item's position
                    Toast.makeText(this, "Bought ${item.itemName}!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        items_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }
    }

    //check if the user has enough gold to buy the item
    private fun checkEnoughGold(item:Item): Boolean {
        return item.price <= User.getGold()
    }
}
