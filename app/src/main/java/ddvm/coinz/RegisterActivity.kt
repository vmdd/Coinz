package ddvm.coinz


import android.content.Intent
import android.support.v7.app.AppCompatActivity

import android.os.Bundle
import android.util.Log

import android.widget.Toast
import com.google.firebase.auth.*

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

import kotlinx.android.synthetic.main.activity_register.*

//class for logging in
class RegisterActivity : AppCompatActivity(){

    private lateinit var mAuth: FirebaseAuth
    private var firestore: FirebaseFirestore? = null
    private var firestoreUser: DocumentReference? = null        //user document
    private var username = ""                                   //user's username
    private var email = ""                                      //user's email
    private var password = ""                                   //user's password

    companion object {
        const val tag = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        //firebase
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        //create new account button click listener
        create_new_acc.setOnClickListener {
            email = fieldEmail.text.toString()
            password = fieldPassword.text.toString()
            username = fieldUserName.text.toString()
            validateForm()
        }
        go_to_sign_in.setOnClickListener {
            finish()
        }
    }

    //create user with given email password
    private fun register(){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this){ task ->
                    if(task.isSuccessful){
                        //sign up successful
                        val mUser = mAuth.currentUser
                        if(mUser!=null)
                            createUserDocument(mUser)       //creating user document and storing user's fata
                        else
                            Log.d(tag, "[register] mUser is null")
                    } else {
                        //sign up unsuccessful
                        try {
                            throw task.exception as Throwable
                        } catch(e: FirebaseAuthWeakPasswordException) {
                            fieldPassword.error = getString(R.string.weak_password_error)       //password too short
                        } catch(e: FirebaseAuthInvalidCredentialsException) {
                            fieldEmail.error = getString(R.string.invalid_email_format)         //not valid email format
                        } catch(e: FirebaseAuthUserCollisionException) {
                            fieldEmail.error = getString(R.string.email_collision)              //account already exists
                        } catch(e: Exception) {
                            Toast.makeText(baseContext, e.message, Toast.LENGTH_SHORT).show()   //other
                        }
                        Log.d(tag,"[register]create user failed ", task.exception)
                    }

                }
    }

    //checks if the data given by the user is valid
    private fun validateForm() {
        //length of username must be 12 or shorter
        if(username.length>12) {
            fieldUserName.error = getString(R.string.long_username)
            return
        }

        //username field cannot be blank
        if(username.isBlank()) {
            fieldUserName.error = getString(R.string.required)
            return
        }

        //username allows only alphanumeric characters
        if(!username.matches("[A-Za-z0-9]+".toRegex())) {
            fieldUserName.error = getString(R.string.invalid_username_format)
            return
        }

        //email field cannot be blank
        if(email.isBlank()){
            fieldEmail.error = getString(R.string.required)
            return
        }

        //password field cannot be blank
        if(password.isBlank()){
            fieldPassword.error = getString(R.string.required)
            return
        }

        checkUserNameAvailable()

    }

    //checks if username is available as usernames need to be unique case insensitive
    private fun checkUserNameAvailable() {
        Utils.checkUserExists(firestore,username) { userExists, _ ->
            if(userExists) {
                //username already taken by someone else
                fieldUserName.error = getString(R.string.username_not_available)
            } else {
                register()  //register user with given data
            }

        }
    }

    //creates an user document using user's id and stores username and lowercase username for comparison purposes
    private fun createUserDocument(mUser: FirebaseUser) {
        firestoreUser = firestore?.collection(User.USERS_COLLECTION_KEY)?.document(mUser.uid)     //document id is user's id
        //add username
        firestoreUser?.set(mapOf(User.USERNAME_FIELD_KEY to username,
                User.LOWERCASE_USERNAME_FIELD_KEY to username.toLowerCase(),
                User.N_PAY_IN_FIELD_KEY to 0,
                User.LAST_PLAY_FIELD_KEY to "",
                User.GOLD_FIELD_KEY to 0,
                User.COLLECTED_COINS_FIELD_KEY to emptyList<String>(),
                User.BINOCULARS_FIELD_KEY to false,
                User.BAG_FIELD_KEY to false,
                User.GLASSES_FIELD_KEY to false))
                ?.addOnSuccessListener {
                    Log.d(tag, "[createUserDocument] user document successfully created")
                    goToMain()
                }
                ?.addOnFailureListener { e ->
                    Log.d(tag, "[createUserDocument] Error writing document", e)
                }
    }

    //starts the MainActivity, finishes login and register activities
    private fun goToMain() {
        finishAffinity()    //finish register and login activities
        startActivity(Intent(this, MainActivity::class.java))
    }


}
