package ddvm.coinz

import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_received_coin.view.*

class ReceivedCoinsAdapter(private val coins:MutableList<Coin>, private val senders:MutableList<String>): CoinsAdapter(coins) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_received_coin, parent, false)
        return CoinViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        val itemCoin = coins[position]
        holder.bindCoin(itemCoin)
        holder.bindSender(senders)
    }

    private fun CoinViewHolder.bindSender(senders: MutableList<String>) {
        view.sender.text = senders[adapterPosition]
    }
}