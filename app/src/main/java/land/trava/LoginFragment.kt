package land.trava

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginFragment : Fragment(R.layout.login_fragment) {

    companion object {
        private const val TAG = "EmailPassword"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        email = view.findViewById(R.id.email)
        password = view.findViewById(R.id.password)
        val login = view.findViewById<Button>(R.id.login)
        val signUp = view.findViewById<TextView>(R.id.signup)

        login.setOnClickListener {
            val emailTx = email.text.toString()
            val passwordTx = password.text.toString()

            if (validateForm()) {
                signIn(emailTx, passwordTx)
            }
        }

        signUp.setOnClickListener {
            val emailTx = email.text.toString()
            val passwordTx = password.text.toString()

            if (validateForm()) {
                createAccount(emailTx, passwordTx)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            updateUI()
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val fieldNotEmpty = resources.getString(R.string.field_not_empty)
        val corrEmail = resources.getString(R.string.correct_email)

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
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    updateUI()
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(context, "Account creation failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    updateUI()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUI() {
        val intent = Intent(requireActivity(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}