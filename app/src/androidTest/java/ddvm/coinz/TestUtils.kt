package ddvm.coinz

import android.support.test.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TestUtils {

    //setting up clean firestore and logging the user in
    fun setUp() {

        val context = InstrumentationRegistry.getTargetContext()

        //get test map from assets
        val input: InputStream = context.assets.open("test.geojson")
        val testMap = input.bufferedReader().use {it.readText()}
        val curDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val dateFormatted = curDate.format(formatter)   //current date
        //save test map to saved preferences and fake that it's from current date
        Utils.saveMapToSharedPrefs(context, dateFormatted, testMap)

        //login the user
        FirebaseAuth.getInstance().signInWithEmailAndPassword("e@d.com", "123456").addOnSuccessListener {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                    .setTimestampsInSnapshotsEnabled(true)
                    .build()
            firestore.firestoreSettings = settings
            val firestoreUser = firestore
                    .collection(User.USERS_COLLECTION_KEY)
                    .document(FirebaseAuth.getInstance().uid!!)
            firestoreUser.set(mapOf(
                    User.USERNAME_FIELD_KEY to "espressso",
                    User.LOWERCASE_USERNAME_FIELD_KEY to "espressso",
                    User.N_PAY_IN_FIELD_KEY to 0,
                    User.LAST_PLAY_FIELD_KEY to dateFormatted,
                    User.GOLD_FIELD_KEY to 0,
                    User.COLLECTED_COINS_FIELD_KEY to emptyList<String>(),
                    User.BINOCULARS_FIELD_KEY to false,
                    User.BAG_FIELD_KEY to false,
                    User.GLASSES_FIELD_KEY to false))

            val coinsToRemove = mutableListOf<String>()

            firestoreUser.collection(User.WALLET_COLLECTION_KEY).get().addOnSuccessListener { result ->
                for(document in result) {
                    coinsToRemove.add(document.id)
                }
                deleteCoinsFromWallet(coinsToRemove)
            }

            val receivedCoinsToRemove = mutableListOf<String>()
            firestoreUser.collection(User.RECEIVED_COLLECTION_KEY).get().addOnSuccessListener { result ->
                for(document in result) {
                    receivedCoinsToRemove.add(document.id)
                }
                deleteCoinsFromReceived(receivedCoinsToRemove)
            }
        }

        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //deletes all coins from the wallet
    private fun deleteCoinsFromWallet(coinIds: MutableList<String>) {
        val firestoreWallet = FirebaseFirestore.getInstance()
                .collection(User.USERS_COLLECTION_KEY)
                .document(FirebaseAuth.getInstance().uid!!)
                .collection(User.WALLET_COLLECTION_KEY)
        for(id in coinIds) {
            firestoreWallet.document(id).delete()
        }
    }

    //delete all coins in received coins wallet
    private fun deleteCoinsFromReceived(coinIds: MutableList<String>) {
        val firestoreReceived = FirebaseFirestore.getInstance()
                .collection(User.USERS_COLLECTION_KEY)
                .document(FirebaseAuth.getInstance().uid!!)
                .collection(User.RECEIVED_COLLECTION_KEY)
        for(id in coinIds) {
            firestoreReceived.document(id).delete()
        }
    }
}