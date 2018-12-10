package ddvm.coinz

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_leaderboard.view.*

class LeaderboardAdapter(private val context: Context,
                         private val leaderboardList: MutableList<Pair<String,Double>>):
        RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    override fun getItemCount(): Int = leaderboardList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaderboard, parent, false)
        return LeaderboardViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val userEntry = leaderboardList[position]
        holder.bind(userEntry)
    }

    inner class LeaderboardViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private var userEntry: Pair<String, Double>? = null

        fun bind(userEntry: Pair<String,Double>) {
            this.userEntry = userEntry

            view.username.text = userEntry.first
            view.user_gold.text = Utils.formatGold(userEntry.second)

            //set the position, with special icons for first 3 positions
            when (adapterPosition) {
                0 -> {
                    val firstIcon = ContextCompat.getDrawable(context, R.drawable.first)
                    view.position.setCompoundDrawablesWithIntrinsicBounds(firstIcon, null, null, null)
                }
                1 -> {
                    val secondIcon = ContextCompat.getDrawable(context, R.drawable.second)
                    view.position.setCompoundDrawablesWithIntrinsicBounds(secondIcon, null, null, null)
                }
                2 -> {
                    val thirdIcon = ContextCompat.getDrawable(context, R.drawable.third)
                    view.position.setCompoundDrawablesWithIntrinsicBounds(thirdIcon, null, null, null)
                }
                else -> view.position.text = (adapterPosition + 1).toString()
            }
        }
    }
}