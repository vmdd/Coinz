package ddvm.coinz

import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_coin.view.*
import kotlin.math.roundToInt

open class CoinsAdapter(private val coins:MutableList<Coin>): RecyclerView.Adapter<CoinsAdapter.CoinViewHolder>() {

    private val itemStateArray = SparseBooleanArray()   //array storing position of selected coins

    fun getItemsStates() = itemStateArray
    fun clearItemsStates() { itemStateArray.clear() }

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

    inner class CoinViewHolder(v: View): RecyclerView.ViewHolder(v) {
        var view: View = v
        private var coin: Coin? = null


        //binding coin to the CoinViewHolder to display the item
        fun bindCoin(coin: Coin) {
            this.coin = coin
            view.coin_currency.text = coin.currency
            view.coin_value.text = coin.value.roundToInt().toString()   //rounds the value of the coin for display
            view.item_checkBox.isChecked = itemStateArray.get(adapterPosition)  //set the textbox to the correct state
            view.setOnClickListener { v -> coinItemClicked(v) }
            view.item_checkBox.setOnClickListener { v -> coinItemClicked(v)}

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