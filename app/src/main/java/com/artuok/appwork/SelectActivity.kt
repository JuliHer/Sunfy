package com.artuok.appwork

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.AbsListView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.ChatAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.fragmets.AverageAsync
import com.artuok.appwork.objects.ChatElement
import com.artuok.appwork.objects.Item
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.thekhaeng.pushdownanim.PushDownAnim
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.*


class SelectActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var lManager: LinearLayoutManager
    private var elements: ArrayList<Item> = ArrayList()
    private lateinit var task: AverageAsync
    private lateinit var realTask: AverageAsync
    private lateinit var skeleton: Skeleton

    private val db = FirebaseFirestore.getInstance()
    private var numberPhones: ArrayList<String> = ArrayList()
    private var contactsCount = 0
    private var contactsDetailed = 0
    private var index = 0
    private var isScrolling = false
    private var currentItems = 0
    private var totalItems = 0
    private var scrollOutItems = 0
    private var isLoading = false
    private var last = false
    private var loggedUsers = 0
    private var isDetach = false

    private lateinit var backButton : ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)


        adapter = ChatAdapter(this, elements) { view, pos ->
            if (elements[pos].type == 0) {
                val c = elements[pos].`object` as ChatElement
                if (c.isLog) {
                    val i = Intent(this, ChatActivity::class.java)
                    val id = getChatId(c.number)

                    if (id >= 0) {
                        i.putExtra("id", id)
                        i.putExtra("first", false)
                    } else {
                        i.putExtra("id", c.id.toInt())
                        i.putExtra("first", true)
                    }

                    val returnIntent = Intent()
                    returnIntent.putExtra("requestCode", 2)
                    setResult(RESULT_OK, returnIntent)
                    startActivity(i)
                    finish()
                }
            } else if (elements[pos].type == 2) {

                val i = Intent(this, CreateGroupActivity::class.java)
                startActivity(i)
                finish()
            }
        }

        backButton = findViewById(R.id.back_button)
        lManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recycler = findViewById(R.id.recycler)
        recycler.setHasFixedSize(true)
        recycler.layoutManager = lManager
        recycler.adapter = adapter

        PushDownAnim.setPushDownAnimTo(backButton)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener{
                finish()
            }

        skeleton = recycler.applySkeleton(R.layout.skeleton_chat_layout, 20)

        val ta = obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        skeleton.maskCornerRadius = 150f



        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                currentItems = lManager.childCount
                totalItems = lManager.itemCount - 10
                scrollOutItems = lManager.findFirstVisibleItemPosition()
                if (isScrolling && currentItems + scrollOutItems >= totalItems) {
                    isScrolling = false
                    if(!isLoading){
                        isLoading = true
                        if(!last){
                            AverageAsync(object : AverageAsync.ListenerOnEvent {
                                override fun onPreExecute() {

                                }

                                override fun onExecute(b: Boolean) {
                                    getInDataContacts(index)
                                }

                                override fun onPostExecute(b: Boolean) {
                                    adapter.notifyDataSetChanged()
                                    index += 50
                                    isLoading = false
                                }
                            }).exec(false)
                        }
                    }

                }
            }
        })

        val b = BitmapFactory.decodeResource(resources, R.drawable.ic_users)
        val chatElement = ChatElement("", "Create Group", "", "", "", "", false, 0)
        chatElement.image = b
        elements.add(Item(chatElement, 2))
        realTask = AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {

            }

            override fun onExecute(b: Boolean) {
                getInDataContacts(index)
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyItemRangeInserted(index, 50)
                index += 50
                isLoading = false
                if(index == 50){
                    skeleton.showOriginal()
                }
            }
        })

        task = AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {
                skeleton.showSkeleton()
            }

            override fun onExecute(b: Boolean) {
                getContacts()
            }

            override fun onPostExecute(b: Boolean) {

                if (!isDetach) {
                    realTask.exec(true)
                }
            }
        })
        skeleton.showSkeleton()
        task.exec(false)
    }

    private fun getChatId(number: String): Int {
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase
        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS} WHERE contact = '$number' AND type = '0'",
            null
        )

        if (query.moveToFirst()) {
            val i = query.getInt(0)
            query.close()
            return i
        }
        query.close()
        return -1
    }

    private fun isSaverModeActive(): Boolean {
        val s = getSharedPreferences("settings", Context.MODE_PRIVATE)

        return s.getBoolean("datasaver", true)
    }

    override fun onDestroy() {
        isDetach = true
        realTask.stop(true)
        task.stop(true)
        super.onDestroy()
    }

    private fun getInDataContacts(from: Int, to: Int) {
        val dbHelper = DbChat(this)
        val db = dbHelper.readableDatabase

        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE added = '1' ORDER BY log DESC, name COLLATE NOCASE ASC LIMIT $from, $to",
            null
        )

        if (query.moveToFirst()) {
            do {
                val id = "${query.getInt(0)}"
                val name = query.getString(1)
                val number = query.getString(2)
                val log = query.getInt(4) == 1

                val chatelement = ChatElement(
                    id,
                    name,
                    number,
                    "",
                    number,
                    "",
                    log,
                    0
                )

                if (log) {
                    loggedUsers++
                }

                elements.add(Item(chatelement, 0))
            } while (query.moveToNext())
        }

        if (query.count == 0)
            last = true

        query.close()
    }

    private fun getInDataContacts(i: Int) {
        val dbHelper = DbChat(this)
        val db = dbHelper.readableDatabase

        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE added = '1' ORDER BY log DESC, name COLLATE NOCASE ASC LIMIT $i, 50",
            null
        )

        if (query.moveToFirst()) {
            do {
                val id = "${query.getInt(0)}"
                val name = query.getString(1)
                val number = query.getString(2)
                val log = query.getInt(4) == 1

                val chatelement = ChatElement(
                    id,
                    name,
                    number,
                    "",
                    number,
                    "",
                    log,
                    0
                )

                if(log){
                    loggedUsers++
                }

                elements.add(Item(chatelement, 0))
            } while (query.moveToNext())
        }

        if(query.count == 0)
            last = true

        query.close()
    }

    private fun getContacts() {
        val cr = contentResolver
        val table = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val selection = "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > ?"
        val arguments = arrayOf("0")

        val cur = cr.query(
            table,
            null,
            selection,
            arguments,
            "${ContactsContract.Contacts.DISPLAY_NAME} COLLATE NOCASE ASC"
        )

        val dbChat = DbChat(this)
        val dbr = dbChat.readableDatabase
        val dbw = dbChat.writableDatabase
        var cursor: Cursor

        val myNumber = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        val now = Calendar.getInstance().timeInMillis
        if (cur != null) {
            if (cur.moveToFirst()) {
                val idIndex =
                    cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val numberIndex =
                    cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                var id: String
                var name: String
                var number: String
                val shared: SharedPreferences =
                    getSharedPreferences("chat", Context.MODE_PRIVATE)
                val code = shared.getString("regionCode", "ZZ")
                var codeNa: String
                val phoneUtil = PhoneNumberUtil.createInstance(this)
                do {
                    id = cur.getString(idIndex)
                    name = cur.getString(nameIndex)
                    number = cur.getString(numberIndex)
                    codeNa = code.toString()
                    try {
                        val phone = phoneUtil.parse(number, codeNa)
                        val numberp =
                            phoneUtil.format(phone, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                        val re = Regex("[^0-9+]")
                        number = re.replace(numberp, "")

                        if (phoneUtil.isValidNumber(phone)) {
                            if (!numberPhones.contains(number) && myNumber != number) {
                                val chat = ChatElement(
                                    id,
                                    name,
                                    numberp,
                                    "",
                                    number,
                                    codeNa,
                                    false,
                                    0
                                )
                                contactsCount++

                                cursor = dbr.query(
                                    DbChat.T_CHATS_LOGGED,
                                    null,
                                    "number = ?",
                                    arrayOf(number),
                                    "",
                                    "",
                                    ""
                                )
                                if (cursor.moveToFirst()) {
                                    val lastname = cursor.getString(1)
                                    if (lastname != name) {
                                        val values = ContentValues()
                                        values.put("name", name)
                                        dbw.update(
                                            DbChat.T_CHATS_LOGGED,
                                            values,
                                            "number = ?",
                                            arrayOf(number)
                                        )
                                    }
                                } else {
                                    val values = ContentValues()
                                    values.put("name", name)
                                    values.put("number", number)
                                    values.put("ISO", codeNa)
                                    values.put("image", "")
                                    values.put("log", false)
                                    values.put("publicKey", "noKey")
                                    values.put("userId", "noUser")
                                    values.put("updated", now)
                                    values.put("added", true)
                                    dbw.insert(DbChat.T_CHATS_LOGGED, null, values)
                                }
                                if (!isMovileDataActive() || !isSaverModeActive())
                                    getUserDetails(chat)
                                numberPhones.add(number)
                                cursor.close()
                            }
                        }

                    } catch (e: NumberParseException) {
                        e.printStackTrace()
                    }


                } while (cur.moveToNext())

            }
            cur.close()
        }
    }

    private fun getUserDetails(chatElement: ChatElement) {
        val userDB = FirebaseDatabase.getInstance().reference
        val query = userDB.child("user").orderByChild("phone").equalTo(chatElement.number)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contactsDetailed++
                if (snapshot.exists()) {
                    var phone = ""
                    var publicKey = ""
                    var updated = 0L
                    for (child in snapshot.children) {
                        if (child.child("phone").value != null)
                            phone = child.child("phone").value.toString()
                        if (child.child("publicKey").value != null)
                            publicKey = child.child("publicKey").value.toString()
                        if (child.child("updated").value != null)
                            updated = child.child("updated").value.toString().toLong()

                        val imageKey = child.key!!

                        updateContactPublicKey(publicKey, phone)
                        updateContactUser(phone, imageKey)
                        updateContactLog(phone, true)
                        updateContactInfo(updated, phone)
                        return
                    }
                } else {
                    updateContactPublicKey("", chatElement.number)
                    updateContactUser(chatElement.number, "noUser")
                    updateContactLog(chatElement.number, false)
                }

                if (contactsCount == contactsDetailed) {

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun isMovileDataActive() : Boolean{
        var mobileDataEnable = false
        try{
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val cmClass = Class.forName(cm.javaClass.name)
            val method = cmClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true
            mobileDataEnable = method.invoke(cm) as Boolean
        }catch( e : Exception){
            e.printStackTrace()
        }

        return mobileDataEnable
    }


    private fun updateContactUser(p: String, user: String) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val cv = ContentValues()
        cv.put("userId", user)
        db.update(DbChat.T_CHATS_LOGGED, cv, "number = '$p'", null)
    }


    private fun updateContactInfo(time: Long, phone: String) {
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase
        val values = ContentValues()
        values.put("updated", time)

        db.update(DbChat.T_CHATS_LOGGED, values, "number = '$phone'", null)
    }


    private fun updateContactLog(p: String, b: Boolean) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val cv = ContentValues()
        cv.put("log", if (b) 1 else 0)
        db.update(DbChat.T_CHATS_LOGGED, cv, "number = '$p'", null)
    }


    private fun updateContactPublicKey(publicKey: String, p: String) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val cv = ContentValues()
        cv.put("publicKey", publicKey)
        db.update(DbChat.T_CHATS_LOGGED, cv, "number = '$p'", null)
    }


}