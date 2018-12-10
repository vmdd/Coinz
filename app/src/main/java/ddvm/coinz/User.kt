package ddvm.coinz

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object User {
    private const val tag = "User"

    private var username: String = ""                               //user's username chosen during registration
    private var collectedCoins: MutableList<*>? = null              //id of coins collected by the user on last play date
    private var gold: Double = 0.0                                  //user's gold amount
    private var lastPlayDate: String? = null                        //last date the player played the game
    private var nPaidInCoins: Int = 0                               //number of coins paid into the bank on the last play date
    private var userId: String? = null                              //user id in firebase auth
    private val wallet = mutableListOf<Coin>()                      //list of coins currently in user's wallet
    private val receivedCoins = mutableListOf<Coin>()               //coins currently in user's receivedCoins account waiting for pay into the bank
    private var binoculars = false                                  //if user bought and owns binoculars
    private var bag = false
    private var walletCapacity = 10                                 //user's wallet capacity (max number of coins in the wallet)
    private var visionRange = 100                                   //range in which user can see coins on the map

    //downloads user's data from firestore
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

            if(document.getDouble("gold")!= null)   //get gold if field not null
                gold = document.getDouble("gold")!!

            if(document.getDouble("n_paid_in_coins")?.toInt()!=null)    //get nPaidInCoins if field not null
                nPaidInCoins = document.getDouble("n_paid_in_coins")?.toInt()!!

            //if the user has binoculars, increase his vision range
            if(document.getBoolean("binoculars") == true) {
                visionRange += Binoculars().additionalVisionRange
                binoculars = true   //note that user already has binoculars
            }



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

    //downloads coins in user's received coins collection from firestore
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
                    completeListener()  //download successfully completed, call completeListener passed by the caller
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

    fun hasItem(itemName: String): Boolean {
        return when(itemName) {
            "Binoculars" -> binoculars
            "Bag" -> bag
            else -> false
        }
    }

    //sets the lastplay date and updates firestore
    fun setLastPlayDate(firestore: FirebaseFirestore?, date: String) {
        lastPlayDate = date
        firestore?.document("users/$userId")
                ?.update("last_play_date", date)
    }

    //clears list of collected coins and updates firestore
    fun clearCollectedCoins(firestore: FirebaseFirestore?) {
        collectedCoins = mutableListOf<String>()
        firestore?.document("users/$userId")
                ?.update("collected_coins", collectedCoins)
    }

    //adds collected coin to the wallet and adds it's id to collected_coins in firestore
    fun addCollectedCoin(firestore: FirebaseFirestore?, coin:Coin) {
        val firestoreWallet = firestore?.collection("users/${userId!!}/wallet")
        firestoreWallet?.document(coin.id)?.set(coin)       //storing collected coins in wallet in firestore
        wallet.add(coin)    //store coin in wallet list
        //add coin to the list of coins collected from the map (no need to update collected coins locally)
        firestore?.document("users/$userId")
                ?.update("collected_coins", FieldValue.arrayUnion(coin.id))

    }

    //sets nPaidInCoins to 0 and updates firestore document
    fun clearNPaidInCoins(firestore: FirebaseFirestore?){
        nPaidInCoins = 0
        firestore?.document("users/$userId")
                ?.update("n_paid_in_coins", nPaidInCoins)
    }

    //adds amount of gold to user's gold and updates firestore
    fun addGold(firestore: FirebaseFirestore?, goldAmount:Double) {
        gold += goldAmount
        firestore?.document("users/$userId")
                ?.update("gold", gold)
    }

    //add the number of coins paid in to nPaidInCoins and updates firestore
    fun addPaidInCoins(firestore: FirebaseFirestore?, nPaidCoins: Int) {
        nPaidInCoins += nPaidCoins
        firestore?.document("users/$userId")
                ?.update("n_paid_in_coins", nPaidInCoins)
    }

    //removes coin from specified collection (wallet/received_coins) and updates firestore
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

    fun setBinoculars(firestore:FirebaseFirestore?, value: Boolean) {
        binoculars = value
        firestore?.document("users/$userId")
                ?.update("binoculars", binoculars)
    }

    fun increaseVisionRange(range:Int) {
        visionRange += range
    }

    fun decreaseGold(firestore: FirebaseFirestore?,goldAmount: Double) {
        gold -= goldAmount
        firestore?.document("users/$userId")
                ?.update("gold", gold)
    }
}