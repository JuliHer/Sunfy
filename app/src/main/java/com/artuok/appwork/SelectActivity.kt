package com.artuok.appwork

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
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


class SelectActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatAdapter
    private var elements: ArrayList<Item> = ArrayList()
    private lateinit var task: AverageAsync
    private lateinit var realTask: AverageAsync
    private lateinit var skeleton: Skeleton

    private val db = FirebaseFirestore.getInstance()
    private var numberPhones: ArrayList<String> = ArrayList()
    private var contactsCount = 0
    private var contactsDetailed = 0


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
                startActivity(i)
                finish()
            }

        }

        val manager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recycler = findViewById(R.id.recycler)
        recycler.setHasFixedSize(true)
        recycler.layoutManager = manager
        recycler.adapter = adapter

        skeleton = recycler.applySkeleton(R.layout.skeleton_chat_layout, 20)

        val ta = obtainStyledAttributes(R.styleable.AppWidgetAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppWidgetAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppWidgetAttrs_maskSkeleton, Color.LTGRAY)

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        skeleton.maskCornerRadius = 150f

        realTask = AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {

            }

            override fun onExecute(b: Boolean) {
                getContacts()
            }

            override fun onPostExecute(b: Boolean) {
                Log.d("CattoEnd", "PROCCESS ENDED")
            }

        })

        task = AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {
                skeleton.showSkeleton()
            }

            override fun onExecute(b: Boolean) {
                getAllContactsSaved()
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                skeleton.showOriginal()
                realTask.exec(true)
            }
        })

        task.exec(false)
    }

    override fun onDestroy() {
        realTask.stop(true)
        super.onDestroy()
    }

    fun getContacts(): Int {
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
            try {
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
                                        "",
                                        false
                                    )

                                    contactsCount++

                                    updateByNumberPhone(name, number)
                                    uploadContactToDB(chatElement)
                                    getUserDetails(chatElement)
                                    numberPhones.add(number)
                                }
                            }

                        } catch (e: NumberParseException) {
                            e.printStackTrace()
                        }

                    } while (cur.moveToNext())

                    Log.d("CattoContacts", "Contacts added: $contactsCount")
                }
            } finally {
                cur.close();
            }

        }

        return 0
    }

    private fun getUserDetails(chatElement: ChatElement) {
        val userDB = FirebaseDatabase.getInstance().reference.child("user")
        val query = userDB.orderByChild("phone").equalTo(chatElement.number)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contactsDetailed++
                if (snapshot.exists()) {
                    var phone = ""
                    var name = ""
                    for (child in snapshot.children) {
                        if (child.child("phone").value != null)
                            phone = child.child("phone").value.toString()
                        if (child.child("name").value != null)
                            name = child.child("name").value.toString()

                        val imageKey = child.key!!

                        updateContactImg(phone, imageKey)
                        updateContactLog(phone)
                        return
                    }
                }

                if (contactsCount == contactsDetailed) {
                    getAllContactsSaved()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun getAllContactsSaved() {
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

                val chatelement = ChatElement(
                    id,
                    name,
                    number,
                    chat,
                    number,
                    regionIso,
                    image,
                    log
                )
                elements.add(Item(chatelement, 0))
            } while (query.moveToNext())
            adapter.notifyDataSetChanged()
        }

        query.close()
    }

    private fun uploadContactToDB(chat: ChatElement) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val name = chat.name
        val number = chat.number
        var iso = chat.numberInternational
        val chatId = chat.chat
        val values = ContentValues()

        if (iso == null)
            iso = "MX"

        values.put("name", name)
        values.put("phone", number)
        values.put("regionISO", iso)
        values.put("chatId", chatId)
        values.put("img", "")
        values.put("isLog", false)
        db.insert(DbChat.T_CHATS_LOGGED, null, values)
    }

    private fun updateContactImg(p: String, img: String) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val cv = ContentValues()
        cv.put("img", img)
        db.update(DbChat.T_CHATS_LOGGED, cv, "phone = '$p'", null)
    }

    private fun updateByNumberPhone(name: String, number: String) {
        val dbchat = DbChat(this)
        val db = dbchat.writableDatabase

        val values = ContentValues()

        values.put("name", name)
        db.update(DbChat.T_CHATS_LOGGED, values, "phone = '$number'", null)
    }

    private fun updateContactLog(p: String) {
        val dbHelper = DbChat(this)
        val db = dbHelper.writableDatabase

        val cv = ContentValues()
        cv.put("isLog", 1)
        db.update(DbChat.T_CHATS_LOGGED, cv, "phone = '$p'", null)
    }
}