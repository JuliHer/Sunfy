package com.artuok.appwork

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.fragmets.SettingsFragment
import com.artuok.appwork.library.TextViewLetterAnimator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.thekhaeng.pushdownanim.PushDownAnim
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.text.Normalizer
import java.util.Base64


class LoginActivity : AppCompatActivity() {

    private lateinit var weareText : TextView
    private lateinit var background : LinearLayout
    private lateinit var emailInput : EditText
    private lateinit var passwordInput : EditText
    private lateinit var signinBtn : TextView
    private lateinit var signupBtn : TextView

    private lateinit var signupEmail : EditText
    private lateinit var signupPassword : EditText
    private lateinit var signupVerifyPassword : EditText
    private lateinit var signupSignupBtn : TextView
    private lateinit var signupSigninBtn : TextView

    private lateinit var usernameInput : EditText
    private lateinit var createAccount : TextView
    private lateinit var back : TextView

    private lateinit var signinLayout : LinearLayout
    private lateinit var signupLayout : LinearLayout
    private lateinit var usernameLayout : LinearLayout

    private val auth = FirebaseAuth.getInstance()


    private var i = 0
    val colors = arrayOf("#52FF77", "#D152FF", "#2FD5FF", "#FFFC52")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if(SettingsFragment.isLogged(this)){
            finish()
        }
        initializateVars()
        setupAnimations()
        show(0)
        PushDownAnim.setPushDownAnimTo(signinBtn)
            .setScale(0.95f)
            .setOnClickListener {
                val email = emailInput.text.toString()
                val password = passwordInput.text.toString()
                if(email.isNotEmpty() && password.isNotEmpty())
                    login(email, password)
            }
        PushDownAnim.setPushDownAnimTo(signupBtn)
            .setScale(0.95f)
            .setOnClickListener {
                show(1)
            }

        PushDownAnim.setPushDownAnimTo(signupSignupBtn)
            .setScale(0.95f)
            .setOnClickListener {
                val email = signupEmail.text.toString().trim()
                val password = signupPassword.text.toString().trim()
                val verifyPassword = signupVerifyPassword.text.toString().trim()

                if(email.isNotEmpty() && password.isNotEmpty() && verifyPassword.isNotEmpty())
                    if(password == verifyPassword)
                        show(2)
                    else
                        Toast.makeText(this@LoginActivity, "No coincide", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this@LoginActivity, "Uno de los campos esta vacio", Toast.LENGTH_SHORT).show()

            }
        PushDownAnim.setPushDownAnimTo(signupSigninBtn)
            .setScale(0.95f)
            .setOnClickListener {
                show(0)
            }
        PushDownAnim.setPushDownAnimTo(createAccount)
            .setScale(0.95f)
            .setOnClickListener {
                val email = signupEmail.text.toString().trim()
                val password = signupPassword.text.toString().trim()
                val username = usernameInput.text.toString().trim()

                if(email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty())
                    signup(username, email, password)
                else
                    Toast.makeText(this@LoginActivity, "Uno de los campos esta vacio", Toast.LENGTH_SHORT).show()
            }
        PushDownAnim.setPushDownAnimTo(back)
            .setScale(0.95f)
            .setOnClickListener {
                show(1)
            }

    }

    private fun initializateVars(){
        weareText = findViewById(R.id.weare)
        background = findViewById(R.id.background)
        emailInput = findViewById(R.id.email)
        passwordInput = findViewById(R.id.password)
        signinBtn = findViewById(R.id.login_btn)
        signupBtn = findViewById(R.id.signup_btn)
        signinLayout = findViewById(R.id.signin_layout)
        signupLayout = findViewById(R.id.signup_layout)
        usernameLayout = findViewById(R.id.username_layout)
        usernameInput = findViewById(R.id.username_input)
        createAccount = findViewById(R.id.create)
        back = findViewById(R.id.back_button)

        signupEmail = findViewById(R.id.email_su)
        signupPassword = findViewById(R.id.password_su)
        signupVerifyPassword = findViewById(R.id.password_verify_su)
        signupSignupBtn = findViewById(R.id.signup_su_btn)
        signupSigninBtn = findViewById(R.id.login_su_btn)
    }

    private fun show(u : Int){
        signinLayout.visibility = View.GONE
        signupLayout.visibility = View.GONE
        usernameLayout.visibility = View.GONE

        when(u){
            0 -> {signinLayout.visibility = View.VISIBLE}
            1 -> {signupLayout.visibility = View.VISIBLE}
            2 -> {usernameLayout.visibility = View.VISIBLE}
            3 -> {}
        }
    }

    private fun signup(username : String, email: String, password: String){
        val newName = username
        val normalized = Normalizer.normalize(newName.lowercase(), Normalizer.Form.NFD)
        val searchTerm = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if(it.isSuccessful){
                val usersReference = FirebaseDatabase.getInstance().reference.child("user")
                val uid = auth.currentUser!!.uid
                usersReference.orderByChild("lowername").equalTo(searchTerm)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val code = snapshot.childrenCount + 1
                            usersReference.child(uid).child("name").setValue(newName)
                            usersReference.child(uid).child("lowername").setValue(searchTerm)
                            usersReference.child(uid).child("search").setValue("$searchTerm-$code")
                            usersReference.child(uid).child("code").setValue(code)
                            auth.currentUser!!.sendEmailVerification()
                            auth.signOut()
                            almostThere()
                            show(0)
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
            }else{
                try {
                    throw it.exception!!
                } catch (e: FirebaseAuthWeakPasswordException) {
                    signupPassword.error = "La contraseña es debil"
                } catch (e: FirebaseAuthInvalidCredentialsException) {
                    signupEmail.error = "El email esta mal escrito"
                } catch (e: FirebaseAuthUserCollisionException) {
                    signupEmail.error = "El usuario ya existe"
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    if(auth.currentUser!!.isEmailVerified){
                        val preferences: SharedPreferences = getSharedPreferences("chat", MODE_PRIVATE)
                        val edit = preferences.edit()
                        edit.putBoolean("logged", true)
                        edit.apply()
                        finish()
                    }else{
                        almostThere()
                        auth.signOut()
                    }
                }else{
                    try {
                        throw it.exception!!
                    } catch (e: FirebaseAuthInvalidUserException) {
                        emailInput.error = "The user doesn't exists"
                    } catch (e: FirebaseAuthInvalidCredentialsException) {
                        passwordInput.error = "the password is incorrect"
                        // Puedes mostrar un mensaje de error en pantalla
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }



    private fun almostThere(){
        val dialog = PermissionDialog()
        dialog.setDrawable(R.drawable.ic_mail)
        dialog.setTitleDialog(getString(R.string.almost_there))
        dialog.setTextDialog(getString(R.string.verify_email))
        dialog.setPositiveText(getString(R.string.Accept_M))
        dialog.setPositive { _, _ ->
            dialog.dismiss()
        }
        dialog.show(supportFragmentManager, "Verify Email")
    }


    private fun setupAnimations(){
        val wearray = arrayOf(
            getString(R.string.we_are_organization),
            getString(R.string.we_are_efficiency),
            getString(R.string.we_are_productivity),
            getString(R.string.we_are_simplification),
            getString(R.string.we_are_focus),
            getString(R.string.we_are_management),
            getString(R.string.we_are_prioritization),
            getString(R.string.we_are_effectiveness),
            getString(R.string.we_are_planning),
            getString(R.string.we_are_coordination),
            getString(R.string.we_are_monitoring),
            getString(R.string.we_are_progress),
            getString(R.string.we_are_reminders),
            getString(R.string.we_are_tasks),
            getString(R.string.we_are_accomplishment)
        )


        weareText.text = ""

        val generateText = TextViewLetterAnimator(weareText, 2000)

        generateText.setOnFinishListener(object : TextViewLetterAnimator.OnFinishListener{
            override fun onFinish() {
                if(weareText.text.isNotEmpty()){
                    generateText.startDeleteAnimation(1500)
                }else{
                    if(i + 1 < wearray.size){
                        i++
                    }else{
                        i = 0
                    }
                    generateText.startGenerateAnimation(wearray[i])
                }
            }
        })


        // Duración de la animación en milisegundos
        val animationDuration = 10000L

        // Crea el objeto ValueAnimator para controlar la animación
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), Color.parseColor(colors[0]), Color.parseColor(colors[1]), Color.parseColor(colors[2]), Color.parseColor(colors[3]))

        // Configura la duración de la animación
        colorAnimator.duration = animationDuration

        // Agrega un listener para actualizar el color de fondo de la vista en cada fotograma
        colorAnimator.addUpdateListener { animator ->
            val animatedValue = animator.animatedValue as Int
            background.setBackgroundColor(animatedValue)

        }
        colorAnimator.repeatCount = ValueAnimator.INFINITE
        colorAnimator.repeatMode = ValueAnimator.REVERSE
        // Inicia la animación
        colorAnimator.start()
        generateText.startGenerateAnimation(wearray[i])
    }
}