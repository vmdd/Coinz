package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_settings.*


class SettingsActivity : AppCompatActivity() {

    private val tag = "SettingsActivity"

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
        firestoreUser = firestore?.collection("users")?.document(mUser!!.uid)     //document id is user's id

        autocollection_switch.isChecked = Utils.getAutocollectionState(this)    //read autocollection switch state
        autocollection_switch.setOnCheckedChangeListener { _, isChecked ->
            Utils.saveAutocollectionState(this, isChecked)      //save the new state of autocollection switch
        }

        //change username switch
        change_username.setOnClickListener {
            val newUsername = new_username.text.toString()
            if(newUsername.isBlank()) {
                new_username.error = getString(R.string.prompt_choose_new_name)
            } else {
                checkUserNameAvailable(newUsername)
            }
        }
    }

    private fun checkUserNameAvailable(username: String) {
        firestore?.collection("users")
                ?.whereEqualTo("lowercase_username", username.toLowerCase())        //querying for documents with same username
                ?.get()
                ?.addOnSuccessListener { documents ->
                    //if documents is empty then username is available
                    if(documents.isEmpty) {
                        changeUsername(username)
                    } else {
                        //username already taken by someone else
                        new_username.error = getString(R.string.username_not_available)
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.d(tag, "[checkUserNameAvailable] error getting documents ", e)
                }
    }

    private fun changeUsername(username: String) {
        firestoreUser?.update("username", username)
                ?.addOnSuccessListener {
                    Toast.makeText(this, "Username succesfully changed to $username", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener {
                    Toast.makeText(this, "Error changing username", Toast.LENGTH_SHORT).show()
                }
    }
}
