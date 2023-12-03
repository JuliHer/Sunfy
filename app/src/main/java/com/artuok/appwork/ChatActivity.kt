package com.artuok.appwork

import android.app.Dialog
import android.app.NotificationManager
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.MessageAdapter
import com.artuok.appwork.adapters.SubjectAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.library.ChatControler
import com.artuok.appwork.library.Constants
import com.artuok.appwork.library.Message
import com.artuok.appwork.library.Message.ChatType
import com.artuok.appwork.library.MessageSwipeController
import com.artuok.appwork.library.SwipeControllerActions
import com.artuok.appwork.objects.EventMessageElement
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.ItemSubjectElement
import com.artuok.appwork.objects.MessageElement
import com.artuok.appwork.objects.SubjectElement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.util.Calendar


class ChatActivity : AppCompatActivity() {
    private lateinit var title : TextView
    private lateinit var username : String
    private lateinit var id : String
    private lateinit var messageControler: ChatControler
    private lateinit var sendButton : ImageView
    private lateinit var textInput : EditText
    private var chatType : ChatType = ChatType.SEARCH
    private lateinit var listenerVerify : Query
    private lateinit var ref : DatabaseReference
    private lateinit var verifyIfExists : ValueEventListener
    private lateinit var childListener : ChildEventListener
    private var exists : Boolean = false

    private lateinit var recyclerMessage : RecyclerView
    private lateinit var adapter : MessageAdapter
    private lateinit var manager : LinearLayoutManager
    private var elements : ArrayList<Item> = ArrayList()

    private lateinit var replyLayout: CardView
    private lateinit var replyMessage : TextView
    private var replyId : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        title = findViewById(R.id.username)
        sendButton = findViewById(R.id.send_btn)
        textInput = findViewById(R.id.text_input)
        recyclerMessage = findViewById(R.id.recyclerView)
        val backButton = findViewById<LinearLayout>(R.id.back_button)
        val closeReply = findViewById<TextView>(R.id.close_reply)
        replyLayout = findViewById(R.id.reply_layout)
        replyMessage = findViewById(R.id.message_replyed)

        adapter = MessageAdapter(this, elements)

        adapter.setOnAddEventListener { view, pos ->
            val element = adapter.getItem(pos)
            if(element.type == 2){
                val msg = element.`object` as MessageElement
                setSelectSubject(msg.task.deadline, msg.task.description, msg.task.user)
            }
        }
        manager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        manager.reverseLayout = true
        recyclerMessage.layoutManager = manager
        recyclerMessage.adapter = adapter
        recyclerMessage.setHasFixedSize(false)

        val extras = intent.extras!!
        username = extras.getString("name")!!
        id = extras.getString("id")!!
        val cachePicture = extras.getString("cachePicture", "")
        if(cachePicture.isNotEmpty()){
            checkAndSetPicture(cachePicture)
        }
        val t = extras.getInt("chatType", 0)
        chatType = if(t == 1){ChatType.CONTACT}else if(t == 2){ ChatType.GROUP}else{ChatType.SEARCH}

        title.text = username

        messageControler = ChatControler(this, id, username, chatType)


        recyclerMessage.itemAnimator = AddItemAnimator()

        verifyExists()
        loadMessages()

        sendButton.setOnClickListener {
            val message = Constants.parseText(textInput.text.toString())
            if(message != ""){
                val msg = Message(message)
                msg.replyId = replyId
                textInput.setText("")
                messageControler.send(msg)
                addMessage(msg, 0)
                recyclerMessage.smoothScrollToPosition(0)
                closingReply()
            }
        }

        val swipeController = object : SwipeControllerActions{
            override fun showReplyUI(position: Int) {
                val messageElement = (adapter.data[position].`object` as MessageElement)
                if(messageElement.user == 0){
                    findViewById<TextView>(R.id.reply_name).text = getString(R.string.you)
                }else{
                    findViewById<TextView>(R.id.reply_name).text = username
                }
                val reply = messageElement.message
                replyMessage.text = reply
                replyId = messageElement.id
                val height = replyMessage.getHeightMeasuered()
                val layoutParams = replyLayout.layoutParams
                layoutParams.height = height
                replyLayout.layoutParams = layoutParams
                replyLayout.requestLayout()
            }
        }

        val controller = MessageSwipeController(this, adapter.data.toCollection(ArrayList<Item>()), true, swipeController)

        val itemTouch = ItemTouchHelper(controller)

        itemTouch.attachToRecyclerView(recyclerMessage)

        closeReply.setOnClickListener {
            closingReply()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun checkAndSetPicture(picture : String){
        val root = externalCacheDir.toString()
        val fName = "$picture.jpg"
        val file = File(root, fName)
        if (file.exists()) {
            val photo = BitmapFactory.decodeFile(file.path)
            findViewById<ImageView>(R.id.usericon).setImageBitmap(photo)
        }
    }


    private fun closingReply(){
        val layoutParams = replyLayout.layoutParams
        layoutParams.height = 0
        replyLayout.layoutParams = layoutParams
        replyLayout.requestLayout()
        replyId = ""
    }

    fun TextView.getHeightMeasuered(): Int {
        val lineHeight = this.lineHeight
        return lineHeight * (lineCount + 2)
    }

    private fun addMessage(msg : Message, user : Int){
        val message = MessageElement(msg.id, msg.message, msg.timestamp, user, msg.user, 0)
        message.reply = getMsgById(msg.replyId)
        if(message.task == null)
            if(message.reply == null)
                adapter.addMessage(Item(message, 0))
            else
                adapter.addMessage(Item(message, 1))
        else
            adapter.addMessage(Item(message, 2))
        adapter.notifyItemInserted(0)
    }

    private fun verifyExists(){
        listenerVerify = FirebaseDatabase.getInstance().reference.child("chat")
            .orderByChild("code")
            .equalTo(messageControler.chatCode)
        verifyIfExists = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    for (child in snapshot.children){
                        if(!exists){
                            messageControler.chat = child.key!!
                            if(!ChatControler.existChat(this@ChatActivity, messageControler.chat)){
                                val u = ChatControler.createChat("", messageControler.chat, messageControler.chatCode, "", "", this@ChatActivity)
                                ChatControler.setNameChat(this@ChatActivity, messageControler.chat)
                                messageControler.chatId = u.toInt()
                            }

                            setListener(messageControler.chat)

                            listenerVerify.removeEventListener(verifyIfExists)
                            exists = true
                        }
                        break
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        }

        listenerVerify.addValueEventListener(verifyIfExists)
    }

    private fun getPositionByKey(key : String) : Int {
        for ((x, item) in adapter.data.withIndex()){
            if(item.type != 3){
                val msg = item.`object` as MessageElement
                if (msg.id == key){
                    return x
                }
            }
        }
        return -1
    }

    private fun setListener(key : String){
        childListener = object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if(snapshot.exists()){
                    val mKey = snapshot.key!!
                    val user = snapshot.child("userId").value.toString()
                    val status = snapshot.child("status").value.toString().toInt()
                    val messageText = snapshot.child("message").value.toString()
                    var replyId = ""
                    if(snapshot.child("reply").exists())
                        replyId = snapshot.child("reply").value.toString()
                    val timestamp = Calendar.getInstance().timeInMillis
                    if(messageControler.checkIfExists(mKey) && getPositionByKey(mKey) != -1){
                        val p = getPositionByKey(mKey)
                        if(p >= 0){
                            messageControler.updateStatus(mKey, status)
                            adapter.modifyStatus(p, status)
                            adapter.notifyItemChanged(p)
                        }
                        val manager =
                            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)!!
                        manager.cancel(8000+messageControler.chatId)
                    }else if(user != FirebaseAuth.getInstance().currentUser?.uid!! && status < 2){
                        val message = Message.Builder(messageText)
                            .setId(mKey)
                            .setStatus(3)
                            .setTimestamp(timestamp)
                            .setUser(user)
                            .setReplyId(replyId)
                        var event : EventMessageElement? = null
                        val element = MessageElement(mKey, messageText, timestamp, 1, user, status)
                        if(snapshot.child("task").exists()){
                            val taskDesc = snapshot.child("task").child("description").value.toString()
                            val taskUser = snapshot.child("task").child("user").value.toString()
                            val taskDeadline = snapshot.child("task").child("deadline").value.toString().toLong()

                            event = EventMessageElement(taskDeadline, taskDesc, taskUser)
                            message.setTask(event)
                            element.task = event
                        }
                        element.reply = getMsgById(replyId)
                        adapter.addMessage(Item(element, if(event == null) if (element.reply == null) 0 else 1 else  2))
                        adapter.notifyItemInserted(0)
                        messageControler.insertMessage(message.build())
                        recyclerMessage.smoothScrollToPosition(0)

                        FirebaseDatabase.getInstance().reference.child("chat").child(key)
                            .child("messages").child(mKey).child("status").setValue(3)
                            .addOnCompleteListener {
                                if(it.isSuccessful){
                                    messageControler.removeMessagesViewed()
                                }
                            }
                    }

                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                if(snapshot.exists()){
                    val keym = snapshot.key!!
                    val stat = snapshot.child("status").value.toString().toInt()

                    if(messageControler.checkIfExists(keym)){
                        val p = getPositionByKey(keym)
                        if(p >= 0){
                            messageControler.updateStatus(keym, stat)
                            adapter.modifyStatus(p, stat)
                            adapter.notifyItemChanged(p)
                        }
                    }
                    messageControler.removeMessagesViewed()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }
        }
        ref = FirebaseDatabase.getInstance().reference.child("chat").child(key).child("messages")

        ref.addChildEventListener(childListener)
    }

    private fun getMsgById(id: String): MessageElement? {
        val chat = DbChat(this)
        val db = chat.readableDatabase
        val c = db.rawQuery(
            "SELECT * FROM " + DbChat.T_CHATS_MSG + " WHERE mid = '" + id + "'",
            null
        )
        if (c.moveToFirst()) {
            val mid = c.getString(4)
            val message = c.getString(1)
            val timestamp = c.getLong(3)
            val type = c.getInt(2)
            val status = c.getInt(5)
            c.close()
            return MessageElement(mid, message, timestamp, type, username, status)
        }
        c.close()
        return null
    }

    private fun loadMessages(){
        adapter.changeMessage(messageControler.getChatRecycler())
        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::childListener.isInitialized){
            ref.removeEventListener(childListener)
        }
    }

    private fun createTask(deadline:Long, task : String, subject : Int, user : String){
        val dbHelper = DbHelper(this)
        val db = dbHelper.writableDatabase

        val values = ContentValues()
        values.put("date", Calendar.getInstance().timeInMillis)
        values.put("deadline", deadline)
        values.put("subject", subject)
        values.put("process_date", 0)
        values.put("description", task)
        values.put("status", 0)
        values.put("user", user)
        values.put("favorite", 0)
        values.put("complete_date", 0L)

        db.insert(DbHelper.T_TASK, null, values)
    }

    private fun setSelectSubject(deadline:Long, task : String, user : String) {
        val subjectDialog = Dialog(this)
        subjectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        subjectDialog.setContentView(R.layout.bottom_sheet_layout)
        subjectDialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        subjectDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        subjectDialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        subjectDialog.window!!.setGravity(Gravity.BOTTOM)
        val recyclerView = subjectDialog.findViewById<RecyclerView>(R.id.subjects_recycler)
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val elements: List<ItemSubjectElement> = getSubjects()
        val adapter = SubjectAdapter(this, elements) { view: View?, position: Int ->
            if(elements[position].type == 2){
                val subject = (elements[position].`object` as SubjectElement)
                createTask(deadline, task, subject.id, user)
            }

            subjectDialog.dismiss()
        }
        val add = subjectDialog.findViewById<LinearLayout>(R.id.add_subject)
        add.visibility = View.GONE
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        subjectDialog.show()
    }

    private fun getSubjects(): List<ItemSubjectElement> {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val elements: MutableList<ItemSubjectElement> = java.util.ArrayList()
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_TAG} ORDER BY name DESC", null)
        if (cursor.moveToFirst()) {
            do {
                elements.add(
                    ItemSubjectElement(
                        SubjectElement(
                            cursor.getInt(0),
                            cursor.getString(1),
                            "",
                            cursor.getInt(2)
                        ), 2
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return elements
    }



    class AddItemAnimator : DefaultItemAnimator() {
        override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
            dispatchRemoveFinished(holder)
            return false
        }

        override fun animateMove(
            holder: RecyclerView.ViewHolder,
            fromX: Int,
            fromY: Int,
            toX: Int,
            toY: Int
        ): Boolean {
            dispatchMoveFinished(holder)
            return false
        }

        override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder,
            fromLeft: Int,
            fromTop: Int,
            toLeft: Int,
            toTop: Int
        ): Boolean {
            dispatchChangeFinished(oldHolder, true)
            dispatchChangeFinished(newHolder, false)
            return false
        }
    }
}