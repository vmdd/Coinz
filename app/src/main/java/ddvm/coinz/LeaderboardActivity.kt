package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_leaderboard.*

class LeaderboardActivity : AppCompatActivity() {

    private var firestore: FirebaseFirestore? = null
    private var firestoreUsers: CollectionReference? = null     //reference to collection with all users documents

    private val leaderboardList = mutableListOf<Pair<String,Double>>()  //list of usernames and user's gold collected

    private lateinit var viewAdapter: LeaderboardAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    companion object {
        const val tag = "LeaderboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        //cloud firestore
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings
        firestoreUsers = firestore?.collection(User.USERS_COLLECTION_KEY)

        viewManager = LinearLayoutManager(this)
        viewAdapter = LeaderboardAdapter(this, leaderboardList)

        leaderboard_recycler_view.apply {
            setHasFixedSize(true)

            //use a linear layout
            layoutManager = viewManager

            adapter = viewAdapter
        }

        getLeaderboard()
    }

    //query firestore for 10 documents with highest gold
    private fun getLeaderboard() {
        leaderboardList.clear()
        firestoreUsers?.orderBy("gold", Query.Direction.DESCENDING)?.limit(10)
                ?.get()
                ?.addOnSuccessListener { documents ->
                    for(document in documents) {
                        val username = document.getString("username")
                        val gold = document.getDouble("gold")
                        if(username != null && gold != null)
                            leaderboardList.add(Pair(username,gold))
                        viewAdapter.notifyItemInserted(leaderboardList.size - 1)  //updates the recycler view with new user entry
                    }
                    Log.d(tag, "[getLeaderboard] leaderboard $leaderboardList")
                }
                ?.addOnFailureListener { e -> Log.d(tag, "[getLeaderborad] failure getting leaderboard ", e) }
    }
}
