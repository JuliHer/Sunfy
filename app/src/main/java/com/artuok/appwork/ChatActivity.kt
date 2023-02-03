package com.artuok.appwork

import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.MessageAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.MessageElement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar

    private lateinit var adapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView
    private var elements: ArrayList<Item> = ArrayList()
    private var messages: ArrayList<String> = ArrayList()
    private lateinit var textInput: EditText
    private lateinit var number: String
    private lateinit var name: String
    private lateinit var chat: String
    private var auth = FirebaseAuth.getInstance()
    private lateinit var listener: ChildEventListener
    private var loadesdMsgFromDB = false

    private lateinit var mChat: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        name = intent.extras?.getString("name")!!
        number = intent.extras?.getString("phone")!!
        chat = intent.extras?.getString("chat")!!

        toolbar = findViewById(R.id.toolbar2)
        (toolbar.findViewById<View>(R.id.title) as TextView).text = name
        toolbar.navigationIcon = getDrawable(R.drawable.ic_arrow_left)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        loadChat(number)

        adapter = MessageAdapter(this, elements)
        val manager = LinearLayoutManager(this, RecyclerView.VERTICAL, true)

        recyclerView = findViewById(R.id.recyclerView)
        textInput = findViewById(R.id.textInput)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        loadMessages(number)


        val sendBtn = findViewById<ImageButton>(R.id.sendMessage)



        sendBtn.setOnClickListener {
            val text = textInput.text.toString()

            if (text != "") {
                textInput.setText("")
                checkMessage(text)
            }
        }
    }

    fun checkMessage(text: String) {

        var length = text.length

        if (length > 0) {
            var last = text.substring(length - 1, length)

            while (last == " " && length > 0) {
                length--
                last = text.substring(length - 1, length)
            }
            if (length > 0) {
                val msg = text.substring(0, length)
                sendMessage(msg)
            }
        }

    }

    fun loadMessages(number: String) {
        if (!loadesdMsgFromDB) {
            val dbChat = DbChat(this)
            val db = dbChat.readableDatabase

            val query = db.rawQuery(
                "SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE number = '$number' ORDER BY timeSend DESC",
                null
            )

            if (query.moveToFirst()) {
                loadesdMsgFromDB = true
                do {
                    val msg = query.getString(1)
                    val ts = query.getLong(3)
                    val id = query.getString(7)

                    var tm = 0;
                    if (query.getInt(2) == 1) {
                        tm = 1
                    }
                    val ms = MessageElement(id, msg, tm, ts, 0, 0)
                    elements.add(Item(ms, tm))
                    messages.add(id)


                } while (query.moveToNext())
                adapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(0)
            }
        }
    }

    fun cancelListener() {
        if (::mChat.isInitialized && ::listener.isInitialized) {
            mChat.removeEventListener(listener)
        }

    }

    fun activateListener() {
        listener = mChat.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.exists()) {
                    if (snapshot.child("userId").value == null)
                        return
                    val key = snapshot.key!!
                    val currentUser = snapshot.child("userId").value.toString()

                    if (!messages.contains(key)) {
                        messages.add(key)

                        var msg = ""
                        var timestamp = 0L
                        if (snapshot.child("message").value != null)
                            msg = snapshot.child("message").value.toString()
                        if (snapshot.child("timestamp").value != null)
                            timestamp = snapshot.child("timestamp").value as Long

                        var tm = 0

                        if (currentUser != auth.currentUser!!.uid) {
                            tm = 1
                        }

                        val Msg = MessageElement(key, msg, tm, timestamp, 0, 0)

                        addMessage(msg, key, tm, timestamp)


                        elements.add(0, Item(Msg, tm))
                        adapter.notifyDataSetChanged()
                        recyclerView.scrollToPosition(0)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    fun addMessage(msg: String, id: String, me: Int, timestamp: Long) {
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase

        val content = ContentValues()

        content.put("MSG", msg)
        content.put("me", me)
        content.put("name", name)
        content.put("chat", chat)
        content.put("number", number)
        content.put("mid", id)
        content.put("timeSend", timestamp)
        content.put("status", 0)

        db.insert(DbChat.T_CHATS_MSG, null, content)
    }


    fun sendMessage(msg: String) {
        createChat(number, msg)
    }

    fun loadChat(number: String) {
        val db = FirebaseDatabase.getInstance().reference.child("user")
        val query = db.orderByChild("phone").equalTo(number)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var name = ""
                    for (user in snapshot.children) {
                        name = user.key!!

                        val usersArr = arrayOf(
                            name,
                            FirebaseAuth.getInstance().currentUser!!.uid
                        )
                        usersArr.sort()

                        val users = usersArr[0] + usersArr[1]

                        val nochat = FirebaseDatabase.getInstance().reference.child("chat")
                            .orderByChild("users").equalTo(users)
                        nochat.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (childes in snapshot.children) {
                                        if (childes.key != null)
                                            chat = childes.key!!
                                        mChat =
                                            FirebaseDatabase.getInstance().reference.child("chat")
                                                .child(chat).child("messages")
                                        activateListener()
                                        break
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }

                        })

                        return
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    fun sendToDatabase(chat: String, msg: String, user: String): String {
        val db =
            FirebaseDatabase.getInstance().reference.child("chat").child(chat).child("messages")
                .push()
        val time = Calendar.getInstance().timeInMillis

        val hash = mapOf(
            "message" to msg,
            "userId" to user,
            "timestamp" to time
        )

        db.updateChildren(hash)

        return db.key!!
    }

    override fun onDestroy() {
        cancelListener()
        super.onDestroy()
    }


    fun createChat(number: String, msg: String) {
        val key = FirebaseDatabase.getInstance().reference.child("chat").push().key!!

        val db = FirebaseDatabase.getInstance().reference.child("user")

        val query = db.orderByChild("phone").equalTo(number)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var name = ""
                    for (user in snapshot.children) {
                        name = user.key!!

                        val usersArr = arrayOf(
                            name,
                            FirebaseAuth.getInstance().currentUser!!.uid
                        )
                        usersArr.sort()

                        val users = usersArr[0] + usersArr[1]

                        val nochat = FirebaseDatabase.getInstance().reference.child("chat")
                            .orderByChild("users").equalTo(users)
                        nochat.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                cancelListener()
                                if (snapshot.exists()) {
                                    for (childes in snapshot.children) {
                                        if (childes.key != null)
                                            chat = childes.key!!
                                        mChat =
                                            FirebaseDatabase.getInstance().reference.child("chat")
                                                .child(chat).child("messages")
                                        break
                                    }
                                } else {
                                    chat = key
                                    mChat = FirebaseDatabase.getInstance().reference.child("chat")
                                        .child(chat).child("messages")

                                    val hash = mapOf(
                                        "users" to users,
                                        "messages" to true,
                                        "type" to 0
                                    )

                                    FirebaseDatabase.getInstance().reference.child("chat")
                                        .child(key).setValue(hash)
                                    db.child(name).child("chat").child(key).setValue(true)
                                    db.child(FirebaseAuth.getInstance().currentUser!!.uid)
                                        .child("chat").child(key).setValue(true)
                                }
                                activateListener()
                                sendToDatabase(chat, msg, auth.currentUser!!.uid)
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }

                        })


                        return
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })


    }
}