package com.artuok.appwork.fragmets

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.artuok.appwork.ChatActivity
import com.artuok.appwork.MainActivity
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File
import java.io.IOException
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
    private var storedVerificationId: String = ""
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
                signInWithPhoneAuthCredential(credential)
                loginDialog.dismiss()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                (requireActivity() as MainActivity).showSnackbar(e.message)
                loginDialog.dismiss()
            }

            override fun onCodeSent(
                verificationId: String,
                p1: PhoneAuthProvider.ForceResendingToken
            ) {


                loginDialog.dismiss()

                val timeout = Calendar.getInstance().timeInMillis + 120000L
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
            val publicKey = (elements[pos].`object` as ChatElement).publicKey
            val intent = Intent(requireActivity(), ChatActivity::class.java)

            intent.putExtra("name", name)
            intent.putExtra("phone", number)
            intent.putExtra("chat", chat)
            intent.putExtra("publicKey", publicKey)
            resultLauncher.launch(intent)
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

        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)

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
                            (requireActivity() as MainActivity).showSnackbar("Number is not valid")
                        }
                    } catch (e: NumberParseException) {
                        e.printStackTrace()
                    }
                }

                loginDialog.setEnterCode { view, text, region ->
                    verifyCode(timeRestant)
                    loginDialog.dismiss()
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

        if(e == 0L){
            verifyDialog.setTimeOut(e)
        }

        verifyDialog.setOnPositiveResponeseListener { view, code ->

            if (storedVerificationId.isEmpty() || storedVerificationId == "") {
                val shared = requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
                storedVerificationId = shared.getString("VerificationId", "")!!
            }

            if(!storedVerificationId.isEmpty() && storedVerificationId != ""){
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
        }
        verifyDialog.show(requireActivity().supportFragmentManager, "verify")

        Handler().postDelayed(object : Runnable {
            override fun run() {
                val n = Calendar.getInstance().timeInMillis
                if (e >= n) {
                    verifyDialog.startTimeOut()
                    Handler().postDelayed(this, 900)
                }
            }
        }, 1000)
    }

    private fun loginUser(userId: String, number : String) {
        val userBase = FirebaseDatabase.getInstance().reference.child("user").child(userId)
        FirebaseDatabase.getInstance().reference.orderByChild("user").equalTo(number)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userHash = mapOf(
                        "name" to "",
                        "phone" to auth.currentUser?.phoneNumber,
                        "region" to userCodePhoneNumber
                    )

                    userBase.updateChildren(userHash)
                    val sharedPreferences: SharedPreferences =
                        requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    editor.putBoolean("logged", true)
                    editor.putLong("verifyTimeOut", 0)
                    editor.putString("regionCode", userCodePhoneNumber)
                    editor.apply()
                    validateUserAndContacts()
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
            .setTimeout(120L, TimeUnit.SECONDS)
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
                    val phone = user?.phoneNumber
                    if (phone != null) {
                        loginUser(user!!.uid, phone)
                    }
                }
            }
    }

    private fun loadChats() {
        val dbChat = DbChat(requireActivity())
        val db = dbChat.readableDatabase
        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_MSG} GROUP BY number ORDER BY timeSend DESC",
            null
        )

        if (query.moveToFirst()) {
            do {
                val i = "${query.getInt(0)}"
                val name = query.getString(4)
                val msg = query.getString(1)
                val chatNumber = query.getString(6)
                val chatC = query.getString(5)
                val timestamp = query.getLong(3)
                val status = query.getInt(8)
                val me = query.getInt(2)

                val q = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE phone = 'chatNumber' ", null)

                val chat = ChatElement(i, name, msg, chatC, chatNumber, "", true, timestamp)

                if(msg == " 1"){
                    chat.desc = getString(R.string.task)
                    chat.contentIcon = requireActivity().getDrawable(R.drawable.ic_book)
                }

                if(me == 0){
                    chat.status = status
                }

                if(q.moveToFirst()){
                    val userId = q.getString(8)
                    if(userId != "noUser"){
                        chat.image = getImagePhoto(userId)
                        searchImageByPhone(chatNumber, i.toLong())
                    }
                }

                elements.add(Item(chat, 0))
            } while (query.moveToNext())
            adapter.notifyDataSetChanged()

        }

        query.close()
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

    public fun loadChatsMessage(){
        elements.clear()
        loadChats()
    }

    fun reloadChats(){
        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
        val login = sharedPreferences.getBoolean("logged", false)
        loginView.visibility = VISIBLE
        if(login){
            validateUserAndContacts()
        }
    }

    public fun onDataChange(){
        elements.clear()
        reloadChats()
    }

    var resultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data!!.getIntExtra("requestCode", 0) == 3) {
            } else if (data.getIntExtra("requestCode", 0) == 2) {
                loadChatsMessage()
            }

            if(data.getIntExtra("awaitingCode", 0) == 2){
                (requireActivity() as MainActivity).notifyAllChanged()
            }
        }
    }


    private fun searchImageByPhone(p : String, n : Long){
        val db = FirebaseDatabase.getInstance().reference.child("user")

        val user = db.orderByChild("phone").equalTo(p)
        user.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    for(us in snapshot.children){
                        val key = us.key!!

                        getImageAndDownload(key, n)

                        return
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getImageAndDownload(u : String, n : Long){
        val ref = FirebaseStorage.getInstance().reference

        val image = ref.child("usericon/$u/profile-photo.jpg")

        image.getFile(getOutputFile(u)).addOnSuccessListener {

            val d = findById("$n")
            (elements[d].`object` as ChatElement).image = getImagePhoto(u)

            adapter.notifyDataSetChanged()
        }
    }

    private fun findById(id : String) : Int{
        var i = 0
        for(x in elements){
            if(x.type == 0){
                val e = x.`object` as ChatElement
                if(e.id == id){
                    return i
                }
            }
            i++
        }

        return -1
    }

    private fun getImagePhoto(u : String) : Bitmap?{
        val root = requireActivity().getExternalFilesDir("Media")
        val appname = getString(R.string.app_name).uppercase()
        val myDir = File(root, ".Profiles")

        var map : Bitmap? = null

        if (myDir.exists())
        {
            val fname = "USER-$u-$appname.jpg"
            val file = File(myDir, fname)
            if (file.exists()) {
                map = BitmapFactory.decodeFile(file.path)
            }
        }

        return map
    }

    private fun getOutputFile(u : String): File {
        val root = requireActivity().getExternalFilesDir("Media")
        val appname = getString(R.string.app_name)
        val myDir = File(root, ".Profiles")
        if (!myDir.exists()) {
            val m = myDir.mkdirs()
            if (m) {
                val nomedia = File(myDir, ".nomedia")
                try {
                    nomedia.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        val fname = "${appname.uppercase()}-$u-IMG.jpg"
        val file = File(myDir, fname)
        try {
            file.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file

    }
}