package ddvm.coinz

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_received_coin.view.*

//inherited CoinsAdapter, modified to bind sender field
class ReceivedCoinsAdapter(context: Context, private val coins:MutableList<Coin>,
                           private val senders:MutableList<String>): CoinsAdapter(context, coins) {

    //overrided to include another item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_received_coin, parent, false)
        return CoinViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        val itemCoin = coins[position]
        holder.bindCoin(itemCoin)
        holder.bindSender(senders)  //displays the sender of each coin received
    }

    //extended CoinViewHolder to bind coin sender's username
    private fun CoinViewHolder.bindSender(senders: MutableList<String>) {
        val view = getView()        //get the view from CoinViewHolder
        view.sender.text = senders[adapterPosition]
    }
}