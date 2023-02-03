package com.artuok.appwork.fragmets

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.artuok.appwork.ChatActivity
import com.artuok.appwork.R
import com.artuok.appwork.adapters.ChatAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.dialogs.LoginDialog
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.dialogs.VerifyDialog
import com.artuok.appwork.objects.ChatElement
import com.artuok.appwork.objects.Item
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.thekhaeng.pushdownanim.PushDownAnim
import java.util.*
import java.util.concurrent.TimeUnit


class ChatFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatAdapter
    private var elements: ArrayList<Item> = ArrayList()
    private lateinit var task: AverageAsync
    private lateinit var skeleton: Skeleton
    private lateinit var contactsPermission: LinearLayout
    private lateinit var chatRecycler: LinearLayout
    private lateinit var loginView: LinearLayout
    private lateinit var btnAllowPermissions: TextView
    private lateinit var btnLogin: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var storedVerificationId: String
    private lateinit var userPhoneNumber: String
    private lateinit var userCodePhoneNumber: String
    private lateinit var loginDialog: LoginDialog

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_chat, container, false)

        auth = Firebase.auth

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("CattoPhoneComplete", "onVerificationCompleted:$credential")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.d("CattoPhoneError", "Error: $e")
                if (e is FirebaseAuthInvalidCredentialsException) {
                    Log.d("CattoPhoneError", "Invalid: $e")
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Log.d("CattoPhoneError", "To many request: $e")
                }

            }

            override fun onCodeSent(
                verificationId: String,
                p1: PhoneAuthProvider.ForceResendingToken
            ) {

                Log.d("CattoPhoneCode", "onCodeSent:$verificationId")
                loginDialog.dismiss()

                val timeout = Calendar.getInstance().timeInMillis + 600000L
                val sharedPreferences: SharedPreferences =
                    requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                verifyCode(timeout)
                editor.putLong("verifyTimeOut", timeout)
                editor.putString("VerificationId", verificationId)
                editor.apply()

                storedVerificationId = verificationId

            }
        }

        adapter = ChatAdapter(requireActivity(), elements) { view, pos ->
            val name = (elements[pos].`object` as ChatElement).name
            val number = (elements[pos].`object` as ChatElement).number
            val chat = (elements[pos].`object` as ChatElement).chat
            val intent = Intent(requireActivity(), ChatActivity::class.java)

            intent.putExtra("name", name)
            intent.putExtra("phone", number)
            intent.putExtra("chat", chat)
            startActivity(intent)
        }

        var manager = LinearLayoutManager(requireActivity(), VERTICAL, false)
        contactsPermission = root.findViewById(R.id.contactsPermissions)
        chatRecycler = root.findViewById(R.id.chatRecycler)
        loginView = root.findViewById(R.id.login)
        recycler = root.findViewById(R.id.recycler_chats)
        btnAllowPermissions = root.findViewById(R.id.contactAllowPermissions)
        btnLogin = root.findViewById(R.id.loginButon)

        PushDownAnim.setPushDownAnimTo(btnAllowPermissions)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        PushDownAnim.setPushDownAnimTo(btnLogin)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                loginWithNumberPhone()
            }




        recycler.setHasFixedSize(true)
        recycler.layoutManager = manager
        recycler.adapter = adapter

        if (loginWithNumberPhone()) {
            validateUserAndContacts()
        }

        skeleton = recycler.applySkeleton(R.layout.skeleton_chat_layout, 20)

        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppWidgetAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppWidgetAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppWidgetAttrs_maskSkeleton, Color.LTGRAY)

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        skeleton.maskCornerRadius = 150f

        //task.exec(false)
        return root
    }

    private fun loginWithNumberPhone(): Boolean {

        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
        val login = sharedPreferences.getBoolean("logged", false)

        if (login) {
            return true
        } else {
            val timeRestant = sharedPreferences.getLong("verifyTimeOut", 0)
            if (Calendar.getInstance().timeInMillis >= timeRestant) {
                loginDialog = LoginDialog()

                loginDialog.setPositive { view, text, region ->
                    val phoneUtil = PhoneNumberUtil.getInstance()
                    try {
                        val phoneNumber = phoneUtil.parse(text, region)
                        if (phoneUtil.isValidNumber(phoneNumber)) {
                            var phone = phoneUtil.format(
                                phoneNumber,
                                PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                            )
                            val re = Regex("[^0-9+]")
                            phone = re.replace(phone, "")

                            val phoneNational = phoneUtil.format(
                                phoneNumber,
                                PhoneNumberUtil.PhoneNumberFormat.NATIONAL
                            )
                            userPhoneNumber = re.replace(phoneNational, "")
                            userCodePhoneNumber = region

                            startPhoneNumberVerification(phone)
                            loginDialog.isCancelable = false
                            loginDialog.setWaiting(true)
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "Number is not valid",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberParseException) {
                        e.printStackTrace()
                    }
                }
                loginDialog.show(requireActivity().supportFragmentManager, "Login")
            } else {
                verifyCode(timeRestant)
            }
        }

        return false
    }

    private fun verifyCode(e: Long) {
        val verifyDialog = VerifyDialog()

        verifyDialog.setTimeOut(e)
        verifyDialog.setOnPositiveResponeseListener { view, code ->

            if (storedVerificationId.isEmpty() || storedVerificationId.equals("")) {
                val shared = requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
                storedVerificationId = shared.getString("VerificationId", "")!!
            }

            val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity()) {
                    if (it.isSuccessful) {
                        val user = it.result?.user
                        val phone = user?.phoneNumber
                        if (phone != null) {
                            loginUser(user!!.uid, phone)
                        }
                    }

                    verifyDialog.dismiss()
                }
        }
        verifyDialog.show(requireActivity().supportFragmentManager, "verify")

        verifyDialog.startTimeOut()
    }

    private fun loginUser(userId: String, phoneNumber: String) {

        db.collection("users").document(userId)
            .get().addOnSuccessListener {
                val sharedPreferences: SharedPreferences =
                    requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                if (it != null && it.data != null && it.exists()) {
                    editor.putBoolean("logged", true)
                    editor.putLong("verifyTimeOut", 0)
                } else {
                    siginUser(userId, phoneNumber)
                }
                editor.putString("regionCode", userCodePhoneNumber)
                editor.apply()

                validateUserAndContacts()
            }
        val userBase = FirebaseDatabase.getInstance().reference.child("user").child(userId)
        userBase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val userHash = mapOf(
                        "name" to "",
                        "phone" to auth.currentUser?.phoneNumber,
                        "region" to userCodePhoneNumber,
                        "user" to auth.currentUser?.uid
                    )

                    userBase.updateChildren(userHash)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun validateUserAndContacts() {
        loginView.visibility = GONE
        contactsPermission.visibility = VISIBLE
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            contactsPermission.visibility = GONE

            chatRecycler.visibility = VISIBLE
            loadChats()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            showInContextUI()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun siginUser(userId: String, phoneNo: String) {
        val hashMap = hashMapOf(
            "name" to "",
            "phoneNo" to auth.currentUser?.phoneNumber,
            "region" to userCodePhoneNumber
        )


        db.collection("users")
            .document(userId).set(hashMap)
            .addOnSuccessListener {
                val sharedPreferences: SharedPreferences =
                    requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("logged", true)
                editor.apply()
            }

    }

    private fun showInContextUI() {
        val dialog = PermissionDialog()
        dialog.setTitleDialog(requireActivity().getString(R.string.required_permissions))
        dialog.setTextDialog(requireActivity().getString(R.string.permissions_read_contacts_description))
        dialog.setPositive { it, i ->
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

        dialog.setNegative { view, which ->
            contactsPermission.visibility = VISIBLE
        }

        dialog.setDrawable(R.drawable.users)
        dialog.show(requireActivity().supportFragmentManager, "permissions")
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .build()
        auth.setLanguageCode(Locale.getDefault().language)
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = it.result?.user
                } else {

                }
            }
    }

    private fun loadChats() {
        val dbChat = DbChat(requireActivity())
        val db = dbChat.readableDatabase
        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_MSG} GROUP BY chat ORDER BY timeSend DESC",
            null
        )

        if (query.moveToFirst()) {
            do {
                val i = "${query.getInt(0)}"
                val name = query.getString(4)
                val msg = query.getString(1)
                val chatNumber = query.getString(6)
                val chatC = query.getString(5)

                val chat = ChatElement(i, name, msg, chatC, chatNumber, "", "", true)

                elements.add(Item(chat, 0))
            } while (query.moveToNext())
            adapter.notifyDataSetChanged()

        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean? ->
        if (isGranted == true) {
            contactsPermission.visibility = GONE
            chatRecycler.visibility = VISIBLE
            loadChats()
        } else {
            contactsPermission.visibility = VISIBLE
        }
    }

}