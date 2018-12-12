package ddvm.coinz

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

//class storing USER_COLLECTION_KEY data and managing the data on firestore
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
    private var glasses = false
    private var statsChanged = false                                //sets to true right after user buys and item to inform main activity about the change

    //firestore collection, documents and field keys
    const val USERS_COLLECTION_KEY = "users"
    const val WALLET_COLLECTION_KEY = "wallet"
    const val RECEIVED_COLLECTION_KEY = "received_coins"
    const val USERNAME_FIELD_KEY = "username"
    const val COLLECTED_COINS_FIELD_KEY = "collected_coins"
    const val LAST_PLAY_FIELD_KEY = "last_play_date"
    const val GOLD_FIELD_KEY = "gold"
    const val N_PAY_IN_FIELD_KEY = "n_paid_in_coins"
    const val BINOCULARS_FIELD_KEY = "binoculars"
    const val BAG_FIELD_KEY = "bag"
    const val GLASSES_FIELD_KEY = "glasses"
    const val LOWERCASE_USERNAME_FIELD_KEY = "lowercase_username"

    //downloads user's data from firestore
    fun downloadUserData(mAuth: FirebaseAuth?, firestore: FirebaseFirestore?, completeListener: () -> Unit) {
        val mUser = mAuth?.currentUser
        userId = mUser!!.uid
        //Cloud firestore
        val firestoreUser = firestore?.collection(USERS_COLLECTION_KEY)?.document(userId!!)  //after login mUser shouldn't be null

        firestoreUser?.get()?.addOnSuccessListener {document ->
            if(document.getString(USERNAME_FIELD_KEY)!=null)
                username = document.getString(USERNAME_FIELD_KEY)!!
            collectedCoins = document.data?.get(COLLECTED_COINS_FIELD_KEY) as? MutableList<*>
            Log.d(tag, "[downloadUserData] collected coins: $collectedCoins")
            lastPlayDate = document.getString(LAST_PLAY_FIELD_KEY)

            gold =
                    if(document.getDouble(GOLD_FIELD_KEY)!= null)
                        document.getDouble(GOLD_FIELD_KEY)!!
                    else
                        0.0

            nPaidInCoins =
                    if(document.getDouble(N_PAY_IN_FIELD_KEY)?.toInt()!=null)    //get nPaidInCoins if field not null
                        document.getDouble(N_PAY_IN_FIELD_KEY)?.toInt()!!
                    else
                        0

            //check if user has binoculars
            binoculars =
                    if(document.getBoolean(BINOCULARS_FIELD_KEY)!=null)
                        document.getBoolean(BINOCULARS_FIELD_KEY)!!
                    else
                        false

            //check if user has bag
            bag =
                    if(document.getBoolean(BAG_FIELD_KEY)!=null)
                        document.getBoolean(BAG_FIELD_KEY)!!
                    else
                        false

            //check if user has glasses
            glasses =
                    if(document.getBoolean(GLASSES_FIELD_KEY)!=null)
                        document.getBoolean(GLASSES_FIELD_KEY)!!
                    else
                        false

            //download user wallet
            downloadUserWallet(firestore, completeListener)
        }
    }

    //download user wallet
    private fun downloadUserWallet(firestore: FirebaseFirestore?, completeListener: () -> Unit) {
        wallet.clear()
        val firestoreWallet = firestore
                ?.collection("$USERS_COLLECTION_KEY/${userId!!}/$WALLET_COLLECTION_KEY")
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
        val firestoreReceived = firestore
                ?.collection("$USERS_COLLECTION_KEY/$userId/$RECEIVED_COLLECTION_KEY")

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

    fun getId() = userId

    fun getLastPlayDate() = lastPlayDate

    fun getGold() = gold

    fun getCollectedCoins() = collectedCoins

    fun getNPaidInCoins() = nPaidInCoins

    fun getWallet() = wallet

    fun getReceivedCoins() = receivedCoins

    fun getStatsChanged() = statsChanged

    fun hasItem(itemName: String): Boolean {
        return when(itemName) {
            Binoculars.itemName -> binoculars
            Bag.itemName -> bag
            Glasses.itemName -> glasses
            else -> false
        }
    }

    //sets the lastplay date and updates firestore
    fun setLastPlayDate(firestore: FirebaseFirestore?, date: String) {
        lastPlayDate = date
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(LAST_PLAY_FIELD_KEY, date)
    }

    //clears list of collected coins and updates firestore
    fun clearCollectedCoins(firestore: FirebaseFirestore?) {
        collectedCoins = mutableListOf<String>()
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(COLLECTED_COINS_FIELD_KEY, collectedCoins)
    }

    //adds collected coin to the wallet and adds it's id to collected_coins in firestore
    fun addCollectedCoin(firestore: FirebaseFirestore?, coin:Coin) {
        val firestoreWallet = firestore
                ?.collection("$USERS_COLLECTION_KEY/${userId!!}/$WALLET_COLLECTION_KEY")
        firestoreWallet?.document(coin.id)?.set(coin)       //storing collected coins in wallet in firestore
        wallet.add(coin)    //store coin in wallet list
        //add coin to the list of coins collected from the map (no need to update collected coins locally)
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(COLLECTED_COINS_FIELD_KEY, FieldValue.arrayUnion(coin.id))

    }

    //sets nPaidInCoins to 0 and updates firestore document
    fun clearNPaidInCoins(firestore: FirebaseFirestore?){
        nPaidInCoins = 0
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(N_PAY_IN_FIELD_KEY, nPaidInCoins)
    }

    //adds amount of gold to user's gold and updates firestore
    fun addGold(firestore: FirebaseFirestore?, goldAmount:Double) {
        gold += goldAmount
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(GOLD_FIELD_KEY, gold)
    }

    //add the number of coins paid in to nPaidInCoins and updates firestore
    fun addPaidInCoins(firestore: FirebaseFirestore?, nPaidCoins: Int) {
        nPaidInCoins += nPaidCoins
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(N_PAY_IN_FIELD_KEY, nPaidInCoins)
    }

    //removes coin from specified collection (wallet/received_coins) and updates firestore
    fun removeCoinFromCollection(firestore: FirebaseFirestore?, collection: String, position: Int) {
        Log.d(tag,"[removeCoinsFromCollection] received_coins: ${receivedCoins.size}, position: $position")
        var coinId = ""
        when(collection){
            WALLET_COLLECTION_KEY -> {
                coinId = wallet[position].id
                wallet.removeAt(position)
            }
            RECEIVED_COLLECTION_KEY -> {
                Log.d(tag, "[removeCoinsFromCollection] $receivedCoins")
                coinId = receivedCoins[position].id
                receivedCoins.removeAt(position)
            }
        }

        firestore?.document("$USERS_COLLECTION_KEY/$userId/$collection/$coinId")
                ?.delete()
                ?.addOnSuccessListener { Log.d(tag, "[removeCoinFromCollection] coin removed from database") }
                ?.addOnFailureListener { e -> Log.d(tag, "[removeCoinFromCollection] error deleting coin document", e) }
    }

    fun setBinoculars(firestore:FirebaseFirestore?, value: Boolean) {
        binoculars = value
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(BINOCULARS_FIELD_KEY, binoculars)
    }

    fun setBag(firestore: FirebaseFirestore?, value: Boolean) {
        bag = value
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(BAG_FIELD_KEY, bag)
    }

    fun setGlasses(firestore: FirebaseFirestore?, value: Boolean) {
        glasses = value
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(GLASSES_FIELD_KEY, glasses)
    }

    fun setStatsChanged(value:Boolean) {
        statsChanged = value
    }

    fun decreaseGold(firestore: FirebaseFirestore?,goldAmount: Double) {
        gold -= goldAmount
        firestore?.document("$USERS_COLLECTION_KEY/$userId")
                ?.update(GOLD_FIELD_KEY, gold)
    }

    //changes the user's username and update firestore
    fun changeUserName(firestore: FirebaseFirestore?, newUsername:String, completeListener: (Boolean) -> Unit) {
        username = newUsername
        firestore?.document("$USERS_COLLECTION_KEY/${userId!!}")
                ?.update("username", username,
                LOWERCASE_USERNAME_FIELD_KEY, username.toLowerCase())
                ?.addOnSuccessListener {
                    completeListener(true)
                }
                ?.addOnFailureListener {
                    completeListener(false)
                }
    }

    //used for log out
    fun clearData() {
        username = ""                               //user's username chosen during registration
        collectedCoins = null              //id of coins collected by the user on last play date
        gold = 0.0                                  //user's gold amount
        lastPlayDate = null                        //last date the player played the game
        nPaidInCoins = 0                               //number of coins paid into the bank on the last play date
        userId = null                              //user id in firebase auth
        wallet.clear()                     //list of coins currently in user's wallet
        receivedCoins.clear()               //coins currently in user's receivedCoins account waiting for pay into the bank
        binoculars = false                                  //if user bought and owns binoculars
        bag = false
        glasses = false
    }
}