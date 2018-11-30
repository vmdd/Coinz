package ddvm.coinz


import android.content.Intent
import android.support.v7.app.AppCompatActivity

import android.os.Bundle

import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_login.*

//class for logging in
class LoginActivity : AppCompatActivity(){

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        //go to MainActivity if user already logged it
        if(mAuth.currentUser != null) {
            goToMain()
        }

        setContentView(R.layout.activity_login)

        email_sign_in_button.setOnClickListener {
            val email = fieldEmail.text.toString()
            val password = fieldPassword.text.toString()
            signIn(email, password)
        }
        create_new_acc.setOnClickListener {

        }
    }

    private fun signIn(email: String, password: String) {
        if(!validateForm(email, password))
            return

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this){ task ->
            if(task.isSuccessful){
                //sign in success
                //go to MainActivity
                goToMain()
            } else {
                //sign in unsuccessful
                Toast.makeText(baseContext, "Authentication failed",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun register(email: String, password: String){
        if(!validateForm(email, password))
            return

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this){ task ->
                    if(task.isSuccessful){
                        //sign up successful
                        finish()
                    } else {
                        Toast.makeText(baseContext, "Authentication failed",Toast.LENGTH_SHORT).show()
                    }

        }
    }

    private fun validateForm(email: String, password: String): Boolean{
        if(email.isEmpty()){
            fieldEmail.error = "Required"
            return false
        }

        if(password.isEmpty()){
            fieldPassword.error = "Required"
            return false
        }

        return true
    }

    //starts the MainActivity and kills the current Login
    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }


}
