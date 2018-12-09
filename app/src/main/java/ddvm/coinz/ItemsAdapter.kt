package ddvm.coinz

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_shop_item.view.*

class ItemsAdapter(private val context: Context, private val items: List<Item>):
        RecyclerView.Adapter<ItemsAdapter.ItemsViewHolder>() {
    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemsViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shop_item, parent, false)
        return ItemsViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ItemsViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    inner class ItemsViewHolder(private val view: View): RecyclerView.ViewHolder(view) {

        fun bind(item: Item) {
            view.item_name.text = item.itemName
            view.item_description.text = item.itemDescription

            if(User.hasBinoculars()) {
                view.buy_item.text = context.getString(R.string.item_owned)
            } else {
                view.buy_item.text = context.getString(R.string.buy_item)
            }
        }
    }
}