package com.artuok.appwork

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.thekhaeng.pushdownanim.PushDownAnim


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

    private lateinit var backButton : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)


        adapter = ChatAdapter(this, elements) { view, pos ->
            val c = elements.get(pos).`object` as ChatElement
            if (c.isLog) {
                val i = Intent(this, ChatActivity::class.java)
                i.putExtra("name", c.name)
                i.putExtra("phone", c.number)
                i.putExtra("chat", c.chat)
                i.putExtra("publicKey", c.publicKey)

                val returnIntent = Intent()
                returnIntent.putExtra("requestCode", 2)
                setResult(RESULT_OK, returnIntent)
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
                totalItems = lManager.itemCount - 4
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

        /*val b = BitmapFactory.decodeResource(resources, R.drawable.users)
        val chatElement = ChatElement("", "Create Group", "", "", "", "", false, 0)
        chatElement.image = b
        elements.add(Item(chatElement, 2))*/
        realTask = AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {

            }

            override fun onExecute(b: Boolean) {
                getInDataContacts(index)
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
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
                getContactsReload()
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                if (elements.size != 0)
                    skeleton.showOriginal()
                realTask.exec(true)
            }
        })
        skeleton.showSkeleton()
        task.exec(false)
    }

    private fun isSaverModeActive() : Boolean{
        val s = getSharedPreferences("settings", Context.MODE_PRIVATE)

        return s.getBoolean("datasaver", true)
    }

    override fun onDestroy() {
        realTask.stop(true)
        super.onDestroy()
    }

    private fun getInDataContacts(i : Int){
        val dbHelper = DbChat(this)
        val db = dbHelper.readableDatabase

        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_LOGGED} ORDER BY isLog DESC, name ASC LIMIT $i, 50",
            null
        )

        if (query.moveToFirst()) {
            do {
                val id = "${query.getInt(0)}"
                val name = query.getString(1)
                val number = query.getString(2)
                val regionIso = query.getString(3)
                val chat = query.getString(4)
                val image = query.getString(6)
                val log = query.getInt(5) == 1
                val timestamp = query.getLong(3)

                val chatelement = ChatElement(
                    id,
                    name,
                    number,
                    chat,
                    number,
                    regionIso,
                    log,
                    timestamp
                )

                if(log){
                    loggedUsers++
                }


                if(!isMovileDataActive() || !isSaverModeActive())
                    getUserDetails(chatelement, elements.size)
                elements.add(Item(chatelement, 0))
            } while (query.moveToNext())
        }

        if(query.count == 0)
            last = true

        query.close()
    }

    private fun getContactsReload() {
        val cr = contentResolver
        val table = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val selection = "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > ?"
        val arguments = arrayOf("0")

        val cur = cr.query(
            table,
            null,
            selection,
            arguments,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )
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
                var codeNa: String

                val shared: SharedPreferences =
                    getSharedPreferences("chat", Context.MODE_PRIVATE)
                val code = shared.getString("regionCode", "ZZ")
                val phoneUtil = PhoneNumberUtil.getInstance()
                do {
                    id = cur.getString(idIndex)
                    name = cur.getString(nameIndex)
                    number = cur.getString(numberIndex)
                    try {
                        val phone = phoneUtil.parse(number, code)
                        number = phoneUtil.format(
                            phone,
                            PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                        )
                        codeNa = code.toString()

                        if (phoneUtil.isValidNumber(phone)) {
                            val re = Regex("[^0-9+]")
                            number = re.replace(number, "")

                            if (!numberPhones.contains(number)) {
                                val chatElement = ChatElement(
                                    id,
                                    name,
                                    number,
                                    "",
                                    number,
                                    codeNa,
                                    false,
                                    0
                                )

                                contactsCount++

                                if(!checkIfIsInDatabase(number))
                                    uploadContactToDB(chatElement)

                                updateByNumberPhone(name, number)
                                numberPhones.add(number)
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

    private fun getUserDetails(chatElement: ChatElement, index : Int) {
        val userDB = FirebaseDatabase.getInstance().reference.child("user")
        val query = userDB.orderByChild("phone").equalTo(chatElement.number)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contactsDetailed++
                if (snapshot.exists()) {
                    var phone = ""
                    var name = ""
                    var publicKey = ""
                    for (child in snapshot.children) {
                        if (child.child("phone").value != null)
                            phone = child.child("phone").value.toString()
                        if (child.child("name").value != null)
                            name = child.child("name").value.toString()
                        if (child.child("publicKey").value != null)
                            publicKey = child.child("publicKey").value.toString()

                        val imageKey = child.key!!

                        if(!chatElement.isLog){
                            loggedUsers++
                            chatElement.isLog = true
                            adapter.notifyItemChanged(index)
                            adapter.notifyItemMoved(index, loggedUsers)
                        }

                        chatElement.isLog = true

                        chatElement.publicKey = publicKey
                        updateContactPublicKey(publicKey, phone)
                        updateContactUser(phone, imageKey)
                        updateContactLog(phone, true)

                        return
                    }
                } else {
                    chatElement.publicKey = ""

                    if(chatElement.isLog){
                        loggedUsers--
                        chatElement.isLog = false
                        adapter.notifyItemChanged(index)
                        adapter.notifyItemMoved(index, elements.size-1)
                    }
                    chatElement.isLog = false
                    updateContactPublicKey("", chatElement.number)
                    updateContactUser(chatElement.number, "noUser")
                    updateContactLog(chatElement.number, false)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getAllContactsSaved(b: Int, i : Int){
        val dbHelper = DbChat(this)
        val db = dbHelper.readableDatabase

        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_LOGGED} ORDER BY isLog DESC, name ASC LIMIT 0, $i",
            null
        )

        if (query.moveToFirst()) {
            do {
                val id = "${query.getInt(0)}"
                val name = query.getString(1)
                val number = query.getString(2)
                val regionIso = query.getString(3)
                val chat = query.getString(4)
                val image = query.getString(6)
                val log = query.getInt(5) == 1
                val timestamp = query.getLong(3)

                val chatelement = ChatElement(
                    id,
                    name,
                    number,
                    chat,
                    number,
                    regionIso,
                    log,
                    timestamp
                )
                elements.add(Item(chatelement, 0))
            } while (query.moveToNext())
            adapter.notifyDataSetChanged()
        }

        query.close()
    }

    private fun getAllContactsSaved(last : Boolean) {
        elements.clear()
        val dbHelper = DbChat(this)
        val db = dbHelper.readableDatabase

        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_LOGGED} ORDER BY isLog DESC, name ASC",
            null
        )

        if (query.moveToFirst()) {
            do {
                val id = "${query.getInt(0)}"
                val name = query.getString(1)
                val number = query.getString(2)
                val regionIso = query.getString(3)
                val chat = query.getString(4)
                val image = query.getString(6)
                val log = query.getInt(5) == 1
                val timestamp = query.getLong(3)

                val chatelement = ChatElement(
                    id,
                    name,
                    number,
                    chat,
                    number,
                    regionIso,
                    log,
                    timestamp
                )
                elements.add(Item(chatelement, 0))
            } while (query.moveToNext())
            adapter.notifyDataSetChanged()
            if(last){
                skeleton.showOriginal()
            }
        }

        query.close()
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

    private fun uploadContactToDB(chat: ChatElement) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val name = chat.name
        val number = chat.number
        var iso = chat.numberInternational
        val chatId = chat.chat
        val publicKey = chat.publicKey
        val values = ContentValues()

        if (iso == null)
            iso = "MX"

        values.put("name", name)
        values.put("phone", number)
        values.put("regionISO", iso)
        values.put("chatId", chatId)
        values.put("img", "")
        values.put("isLog", false)
        values.put("publicKey", "noKey")
        values.put("userId", "noUser")
        db.insert(DbChat.T_CHATS_LOGGED, null, values)
    }

    private fun updateContactUser(p: String, img: String) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val cv = ContentValues()
        cv.put("userId", img)
        db.update(DbChat.T_CHATS_LOGGED, cv, "phone = '$p'", null)
    }

    private fun updateByNumberPhone(name: String, number: String) {
        val dbchat = DbChat(this)
        val db = dbchat.writableDatabase

        val values = ContentValues()

        values.put("name", name)
        db.update(DbChat.T_CHATS_LOGGED, values, "phone = '$number'", null)
    }

    private fun updateContactLog(p: String, b: Boolean) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val cv = ContentValues()
        cv.put("isLog", if (b) 1 else 0)
        db.update(DbChat.T_CHATS_LOGGED, cv, "phone = '$p'", null)
    }

    private fun checkIfIsInDatabase(number: String) : Boolean{
        val dbHelper = DbChat(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE phone = ?", arrayOf(number))

        if(c != null && c.moveToFirst()){
            c.close()
            return true
        }
        c.close()
        return false
    }

    private fun updateContactPublicKey(publicKey: String, p: String) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val cv = ContentValues()
        cv.put("publicKey", publicKey)
        db.update(DbChat.T_CHATS_LOGGED, cv, "phone = '$p'", null)
    }


}