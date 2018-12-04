package ddvm.coinz


import android.content.Intent
import android.support.v7.app.AppCompatActivity

import android.os.Bundle
import android.util.Log

import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

import kotlinx.android.synthetic.main.activity_register.*

//class for logging in
class RegisterActivity : AppCompatActivity(){

    private val tag = "RegisterActivity"
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()

        create_new_acc.setOnClickListener {
            val email = fieldEmail.text.toString()
            val password = fieldPassword.text.toString()
            val username = fieldUserName.text.toString()
            register(email, password, username)
        }
        go_to_sign_in.setOnClickListener {
            finish()
        }
    }

    private fun register(email: String, password: String, username: String){
        if(!validateForm(email, password, username))
            return

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this){ task ->
                    if(task.isSuccessful){
                        //sign up successful
                        goToMain()
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

    private fun validateForm(email: String, password: String, username: String): Boolean{
        if(username.length>15) {
            fieldUserName.error = getString(R.string.long_username)
            return false
        }
        if(username.isBlank()) {
            fieldUserName.error = getString(R.string.required)
            return false
        }
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
        finishAffinity()    //also finishes the parent activity - LoginActivity
    }


}
