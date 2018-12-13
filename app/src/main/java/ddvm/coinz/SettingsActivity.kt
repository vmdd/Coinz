package ddvm.coinz

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_settings.*


class SettingsActivity : AppCompatActivity() {


    private var firestore: FirebaseFirestore? = null
    private var firestoreUser: DocumentReference? = null        //user document

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //firestore setup
        val mAuth = FirebaseAuth.getInstance()
        val mUser = mAuth.currentUser
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings
        firestoreUser = firestore?.collection(User.USERS_COLLECTION_KEY)?.document(mUser!!.uid)     //document id is user's id

        autocollection_switch.isChecked = Utils.getAutocollectionState(this)    //read autocollection switch state
        autocollection_switch.setOnCheckedChangeListener { _, isChecked ->
            Utils.saveAutocollectionState(this, isChecked)      //save the new state of autocollection switch
        }

        //change username switch
        change_username.setOnClickListener {
            if(!checkNetworkConnection()){
                Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newUsername = new_username.text.toString()
            when {
                newUsername.isBlank() -> new_username.error = getString(R.string.prompt_choose_new_name)
                newUsername.length > 12 -> new_username.error = getString(R.string.long_username)
                else -> checkUserNameAvailable(newUsername)
            }
        }
    }

    //checks if given username is available
    private fun checkUserNameAvailable(username: String) {
        Utils.checkUserExists(firestore, username) { exists, _ ->
            if(exists) {
                //username already taken by someone else
                new_username.error = getString(R.string.username_not_available)
            } else {
                changeUsername(username)
            }
        }
    }

    //changes user's username
    private fun changeUsername(username: String) {
        //call User function to change the username and pass a listener to listen when the action completes
        User.changeUserName(firestore, username) { success ->
            if(success) {
                Toast.makeText(this,
                        getString(R.string.username_change_success) + username,
                        Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this,
                        getString(R.string.username_change_failure), Toast.LENGTH_SHORT).show()
            }
        }
    }

    //checks if network connection is available
    private fun checkNetworkConnection(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }
}
