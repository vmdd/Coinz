package ddvm.coinz


import android.support.v7.app.AppCompatActivity

import android.os.Bundle

import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(){

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        email_sign_in_button.setOnClickListener {
            val email = email.text.toString()
            val password = password.text.toString()
            signIn(email, password)
        }
        create_new_acc.setOnClickListener {

        }
    }

    private fun signIn(email: String, password: String) {

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this){ task ->
            if(task.isSuccessful){
                //sign in success
                //go back to Map
                finish()
            } else {
                //sign in unsuccessful
                Toast.makeText(baseContext, "Authentication failed",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun register(email: String, password: String){
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


}
