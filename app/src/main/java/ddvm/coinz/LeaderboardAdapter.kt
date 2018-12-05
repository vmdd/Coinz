package ddvm.coinz

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_leaderboard.view.*

class LeaderboardAdapter(private val leaderboardList: MutableList<Pair<String,Int>>):
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

    class LeaderboardViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private var userEntry: Pair<String, Int>? = null

        fun bind(userEntry: Pair<String,Int>) {
            this.userEntry = userEntry

            view.position.text = (adapterPosition + 1).toString()
            view.username.text = userEntry.first
            view.user_gold.text = userEntry.second.toString()
        }
    }
}