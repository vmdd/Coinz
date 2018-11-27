package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.mapbox.mapboxsdk.geometry.LatLng

import kotlinx.android.synthetic.main.activity_wallet.*

class WalletActivity : AppCompatActivity() {

    private var coins = mutableListOf<Coin>()   //coins collected by the user currently in the wallet
    private lateinit var viewAdapter: CoinsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        //dummy coins for testing
        coins.add(Coin("123",2.0 , "GOLD" , 5, "doesnt batter", LatLng(0.0,0.0)))
        coins.add(Coin("133",3.0 , "PPP" , 5, "doesnt batter", LatLng(0.0,0.0)))

        viewManager = LinearLayoutManager(this)
        viewAdapter = CoinsAdapter(coins)

        coins_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }
    }
}
