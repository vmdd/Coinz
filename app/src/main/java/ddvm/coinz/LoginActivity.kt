package ddvm.coinz


import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity

import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_login.*

//class for logging in
class LoginActivity : AppCompatActivity(){

    private lateinit var mAuth: FirebaseAuth

    companion object {
        const val tag = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        //go to MainActivity if user already logged it
        if(mAuth.currentUser != null) {
            goToMain()
        }

        setContentView(R.layout.activity_login)

        email_sign_in_button.setOnClickListener {
            //check if there is network connection
            if(!checkNetworkConnection()){
                Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val email = fieldEmail.text.toString()
            val password = fieldPassword.text.toString()
            signIn(email, password)
        }
        go_to_register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        Log.d(tag, "[onCreate] called")
    }

    private fun signIn(email: String, password: String) {
        if(!validateForm(email, password))
            return

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this){ task ->
            if(task.isSuccessful){
                //sign in success
                //go to MainActivity
                Log.d(tag,"[signIn] login successful")
                goToMain()
            } else {
                //sign in unsuccessful
                Log.d(tag,"[signIn] login failed ", task.exception)
                fieldEmail.error  = getString(R.string.invalid_email_pass)
                fieldPassword.error = getString(R.string.invalid_email_pass)
            }
        }
    }

    private fun validateForm(email: String, password: String): Boolean{
        if(email.isBlank()){
            fieldEmail.error = getString(R.string.required)
            return false
        }

        if(password.isBlank()){
            fieldPassword.error = getString(R.string.required)
            return false
        }

        return true
    }

    //starts the MainActivity and kills the current Login
    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    //checks if network connection is available
    private fun checkNetworkConnection(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    override fun onDestroy() {
        super.onStop()
        Log.d(tag, "[onDestroy] login act")
    }

}
