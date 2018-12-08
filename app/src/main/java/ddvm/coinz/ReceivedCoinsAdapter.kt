package ddvm.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_received_coin.view.*

//inherited CoinsAdapter, modified to bind sender field
class ReceivedCoinsAdapter(context: Context, private val coins:MutableList<Coin>): CoinsAdapter(context, coins) {

    //override to include another item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_received_coin, parent, false)
        return CoinViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        val itemCoin = coins[position]
        holder.bindCoin(itemCoin)
        holder.bindSender(itemCoin)  //bind the sender of each coin received
    }

    //extended CoinViewHolder to bind coin sender's username
    private fun CoinViewHolder.bindSender(coin: Coin) {
        val view = getView()        //get the view from CoinViewHolder
        view.sender.text = coin.id.substring(29)    //get the sender username included in the coin id
    }
}