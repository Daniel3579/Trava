package land.trava

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity: AppCompatActivity() {

    companion object {
        private const val TAG = "EmailPassword"
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        auth = Firebase.auth

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val login = findViewById<Button>(R.id.login)
        val singUp = findViewById<TextView>(R.id.signup)

        login.setOnClickListener {
            val emailTx = email.text.toString()
            val passwordTx = password.text.toString()

            if (validateForm()) {
                signIn(emailTx, passwordTx)
            }
        }

        singUp.setOnClickListener {
            val emailTx = email.text.toString()
            val passwordTx = password.text.toString()

            if (validateForm()) {
                createAccount(emailTx, passwordTx)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            updateUI()
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val fieldNotEmpty = getResources().getString(R.string.field_not_empty)
        val corrEmail = getResources().getString(R.string.correct_email)

        if (email.text.isEmpty()) {
            email.requestFocus()
            email.error = fieldNotEmpty
            valid = false
        } else if (!email.text.contains("@") || !email.text.contains(".")) {
            email.requestFocus()
            email.error = corrEmail
            valid = false
        } else if (password.text.isEmpty()) {
            password.requestFocus()
            password.error = fieldNotEmpty
            valid = false
        } else {
            email.error = null
            password.error = null
        }
        return valid
    }

    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
//                    sendEmailVerification()
                    updateUI()
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    updateUI()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser!!
        user.sendEmailVerification().addOnCompleteListener(this) {}
    }

    private fun updateUI() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}