package ddvm.coinz

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_shop_item.view.*

//adapter for list of items in the ShopActivity
//the clickListener is for listening to "buy" button clicks. Takes the item and adapter position as arguments
class ItemsAdapter(private val context: Context, private val items: List<Item>,
                   val clickListener: (Item,Int) -> Unit):
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

        //binds item's data to recycler view entry
        fun bind(item: Item) {
            view.item_name.text = item.itemName
            view.item_description.text = item.itemDescription
            view.item_icon.setImageDrawable(ContextCompat.getDrawable(context, item.iconResource))
            //check if user already has given item
            if(User.hasItem(item.itemName)) {
                //user already has binoculars, set button text to "bought" and diable the button
                view.buy_item.text = context.getString(R.string.item_owned)
                view.buy_item.isEnabled = false
            } else {
                //set the button text to "buy" and set the click listener
                val goldIcon = ContextCompat.getDrawable(context, R.drawable.gold)
                view.buy_item.setCompoundDrawablesWithIntrinsicBounds(goldIcon, null, null, null)
                view.buy_item.text = Utils.formatGold(item.price)
                view.buy_item.setOnClickListener {clickListener(item, adapterPosition)}
            }


        }
    }
}