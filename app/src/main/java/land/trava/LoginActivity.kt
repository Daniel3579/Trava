package land.trava

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class LoginActivity: AppCompatActivity() {

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        replaceFragment(LoginFragment())

        val navbar = findViewById<BottomNavigationView>(R.id.navbar)

        navbar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.account -> replaceFragment(LoginFragment())
                R.id.about -> replaceFragment(InfoFragment())
            }
            true
        }
    }
}