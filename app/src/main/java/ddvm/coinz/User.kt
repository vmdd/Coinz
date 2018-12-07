package ddvm.coinz

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object User {
    private var username: String? = null
    private var collectedCoins: List<*>? = null
    private var gold: Double? = null
    private var lastPlayDate: String? = null
    private var nPaidInCoins: Int? = null
    private var userId: String? = null

    fun downloadUserData(mAuth: FirebaseAuth?, firestore: FirebaseFirestore?, completeListener: () -> Unit) {
        val mUser = mAuth?.currentUser
        userId = mUser!!.uid
        //Cloud firestore
        val firestoreUser = firestore?.collection("users")?.document(userId!!)  //after login mUser shouldn't be null

        firestoreUser?.get()?.addOnSuccessListener {document ->
            username = document.getString("username")
            collectedCoins = document.data?.get("collected_coins") as? MutableList<*>
            gold = document.getDouble("gold")
            lastPlayDate = document.getString("last_play_date")
            nPaidInCoins = document.getDouble("n_paid_in_coins")?.toInt()
            completeListener()
        }
    }

    fun getUsername() = username
    fun getLastPlayDate() = lastPlayDate
    fun getGold() = gold
    fun getCollectedCoins() = collectedCoins
    fun getNPaidInCoins() = nPaidInCoins

    fun setLastPlayDate(firestore: FirebaseFirestore?, date: String) {
        lastPlayDate = date
        firestore?.document("users/$userId")
                ?.update("last_play_date", date)
    }

    fun setCollectedCoins(firestore: FirebaseFirestore?, collectedList: List<*>) {
        collectedCoins = collectedList
        firestore?.document("users/$userId")
                ?.update("collected_coins", collectedList)
    }

    fun setNPaidInCoins(firestore: FirebaseFirestore?, numberPaidIn: Int){
        nPaidInCoins = numberPaidIn
        firestore?.document("users/$userId")
                ?.update("n_paid_in_coins", numberPaidIn)
    }
}