package ddvm.coinz

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object User {
    private const val tag = "User"
    private var username: String = ""
    private var collectedCoins: MutableList<*>? = null
    private var gold: Double = 0.0
    private var lastPlayDate: String? = null
    private var nPaidInCoins: Int = 0
    private var userId: String? = null
    private val wallet = mutableListOf<Coin>()
    private val receivedCoins = mutableListOf<Coin>()
    private var binoculars = false      //if user has binoculars
    private var walletCapacity = 10
    private var visionRange = 100

    fun downloadUserData(mAuth: FirebaseAuth?, firestore: FirebaseFirestore?, completeListener: () -> Unit) {
        val mUser = mAuth?.currentUser
        userId = mUser!!.uid
        //Cloud firestore
        val firestoreUser = firestore?.collection("users")?.document(userId!!)  //after login mUser shouldn't be null

        firestoreUser?.get()?.addOnSuccessListener {document ->
            if(document.getString("username")!=null)
                username = document.getString("username")!!
            collectedCoins = document.data?.get("collected_coins") as? MutableList<*>
            lastPlayDate = document.getString("last_play_date")

            if(document.getDouble("gold")!= null)
                gold = document.getDouble("gold")!!

            if(document.getDouble("n_paid_in_coins")?.toInt()!=null)
                nPaidInCoins = document.getDouble("n_paid_in_coins")?.toInt()!!

            //download user wallet
            downloadUserWallet(firestore, completeListener)
        }
    }

    //download user wallet
    private fun downloadUserWallet(firestore: FirebaseFirestore?, completeListener: () -> Unit) {
        wallet.clear()
        val firestoreWallet = firestore?.collection("users/${userId!!}/wallet")
        firestoreWallet
                ?.get()
                ?.addOnSuccessListener {result ->
                    for(document in result) {
                        val coin = document.toObject(Coin::class.java)
                        wallet.add(coin)
                    }
                    //download complete, go back to caller
                    completeListener()
                }
                ?.addOnFailureListener { exception ->
                    Log.d(tag, "[downloadUserWallet] download failed", exception)
                }
    }

    fun downloadReceivedCoins(firestore: FirebaseFirestore?, completeListener: () -> Unit) {
        receivedCoins.clear()
        val firestoreReceived = firestore?.collection("users/$userId/received_coins")

        firestoreReceived
                ?.get()
                ?.addOnSuccessListener {result ->
                    for(document in result) {
                        val coin = document.toObject(Coin::class.java)
                        receivedCoins.add(coin)
                    }
                    completeListener()
                }
                ?.addOnFailureListener {exception ->
                    Log.w(tag, "[fetchWallet] Error getting documents: ", exception)
                }

    }

    fun getUsername() = username

    fun getLastPlayDate() = lastPlayDate

    fun getGold() = gold

    fun getCollectedCoins() = collectedCoins

    fun getNPaidInCoins() = nPaidInCoins

    fun getWallet() = wallet

    fun getReceivedCoins() = receivedCoins

    fun getWalletCapacity() = walletCapacity

    fun getVisionRange() = visionRange

    fun hasBinoculars() = binoculars

    fun setLastPlayDate(firestore: FirebaseFirestore?, date: String) {
        lastPlayDate = date
        firestore?.document("users/$userId")
                ?.update("last_play_date", date)
    }

    fun clearCollectedCoins(firestore: FirebaseFirestore?) {
        collectedCoins = mutableListOf<String>()
        firestore?.document("users/$userId")
                ?.update("collected_coins", collectedCoins)
    }

    fun addCollectedCoin(firestore: FirebaseFirestore?, coin:Coin) {
        val firestoreWallet = firestore?.collection("users/${userId!!}/wallet")
        firestoreWallet?.document(coin.id)?.set(coin)       //storing collected coins in wallet in firestore
        wallet.add(coin)    //store coin in wallet list
        //add coin to the list of coins collected from the map (no need to update collected coins locally)
        firestore?.document("users/$userId")
                ?.update("collected_coins", FieldValue.arrayUnion(coin.id))

    }

    fun clearNPaidInCoins(firestore: FirebaseFirestore?){
        nPaidInCoins = 0
        firestore?.document("users/$userId")
                ?.update("n_paid_in_coins", nPaidInCoins)
    }

    fun addGold(firestore: FirebaseFirestore?, goldAmount:Double) {
        gold += goldAmount
        firestore?.document("users/$userId")
                ?.update("gold", gold)
    }

    fun addPaidInCoins(firestore: FirebaseFirestore?, nPaidCoins: Int) {
        nPaidInCoins += nPaidCoins
        firestore?.document("users/$userId")
                ?.update("n_paid_in_coins", nPaidInCoins)
    }

    fun removeCoinFromCollection(firestore: FirebaseFirestore?, collection: String, position: Int) {
        Log.d(tag,"[removeCoinsFromCollection] received_coins: ${receivedCoins.size}, position: $position")
        var coinId = ""
        when(collection){
            "wallet" -> {
                coinId = wallet[position].id
                wallet.removeAt(position)
            }
            "received_coins" -> {
                Log.d(tag, "[removeCoinsFromCollection] $receivedCoins")
                coinId = receivedCoins[position].id
                receivedCoins.removeAt(position)
            }
        }

        firestore?.document("users/$userId/$collection/$coinId")
                ?.delete()
                ?.addOnSuccessListener { Log.d(tag, "[removeCoinFromCollection] coin removed from database") }
                ?.addOnFailureListener { e -> Log.d(tag, "[removeCoinFromCollection] error deleting coin document", e) }
    }

    fun equipItem(firestore: FirebaseFirestore?, item: Item) {
        if(item.addVisionRange!=0) {
            visionRange += item.addVisionRange
            binoculars = true
        }
        firestore?.document("users/$userId")
                ?.update("binoculars", binoculars)
    }
}