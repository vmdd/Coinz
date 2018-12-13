package ddvm.coinz

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_coin.view.*

open class CoinsAdapter(private val context: Context,
                        private val coins:MutableList<Coin>,
                        private val exchangeRates: MutableMap<String,Double>):
        RecyclerView.Adapter<CoinsAdapter.CoinViewHolder>() {

    private val itemStateArray = SparseBooleanArray()   //array storing position of selected coins

    fun getItemsStates() = itemStateArray
    fun clearItemsStates() { itemStateArray.clear() }   //clear the item state array

    override fun getItemCount() = coins.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_coin, parent, false)
        return CoinViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        val itemCoin = coins[position]
        holder.bindCoin(itemCoin)
    }

    //bind all positions to true to select all items
    fun checkAllItems() {
        for(i in 0 until coins.size)
            itemStateArray.put(i, true)
    }

    inner class CoinViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        private var coin: Coin? = null

        fun getView() = view            //user by ReceivedCoinsAdapter

        //binding coin to the CoinViewHolder to display the item
        fun bindCoin(coin: Coin) {
            this.coin = coin
            view.coin_currency.text = coin.currency
            view.item_checkBox.isChecked = itemStateArray.get(adapterPosition)  //set the textbox to the correct state
            //set the click listener to the list entry
            view.setOnClickListener { v -> coinItemClicked(v) }
            view.item_checkBox.setOnClickListener { v -> coinItemClicked(v)}
            //val iconResource = Utils.selectIcon(coin.currency, coin.value.toInt().toString())
            view.coin_icon.text = coin.value.toInt().toString()     //display coin value

            //select coin's color corresponding to coin's currency
            view.coin_icon.background.setTint(ContextCompat.getColor(context, Utils.selectColorForIcon(coin.currency)))
            view.gold_value.text = Utils.formatGold(coin.toGold(exchangeRates))         //format and display gold value of coin
        }

        //changes the checkbox state and stores it in the itemStateArray
        private fun coinItemClicked(view: View) {
            if(!itemStateArray.get(adapterPosition, false)) {
                view.item_checkBox.isChecked = true
                itemStateArray.put(adapterPosition, true)
            } else {
                view.item_checkBox.isChecked = false
                itemStateArray.put(adapterPosition, false)
            }
        }
    }
}