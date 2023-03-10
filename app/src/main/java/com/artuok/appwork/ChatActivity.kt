package com.artuok.appwork

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.*
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.ColorSelectAdapter
import com.artuok.appwork.adapters.MessageAdapter
import com.artuok.appwork.adapters.SubjectAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.library.MessageSwipeController
import com.artuok.appwork.library.SwipeControllerActions
import com.artuok.appwork.objects.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.thekhaeng.pushdownanim.PushDownAnim
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import javax.crypto.Cipher
import kotlin.collections.ArrayList

class ChatActivity : AppCompatActivity() {

    private val OPCION_RSA = "RSA/ECB/OAEPWithSHA1AndMGF1Padding"
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
    private lateinit var reply_layout : CardView
    private lateinit var reply_message : TextView
    private lateinit var reply_name : TextView
    private lateinit var close_reply : TextView
    private lateinit var publicKeyString : String
    private lateinit var publicKey : PublicKey
    private lateinit var privateKey : PrivateKey
    private lateinit var cipher: Cipher
    private var currentMessageHeight = 0
    private var userId = ""

    private var reply_id : String = ""

    private val ANIMATION_DURATION = 200L

    private lateinit var mChat: DatabaseReference

    private var selectMode = false
    private var addWhileOnSelectMode = 0
    private var messageSelected = 0

    private lateinit var closeable : LinearLayout
    private lateinit var chatName : TextView
    private lateinit var selectLinearLayout : LinearLayout
    private lateinit var finishSelectMode : ImageView
    private lateinit var selectCountText : TextView
    private lateinit var deleteIcon : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        name = intent.extras?.getString("name")!!
        number = intent.extras?.getString("phone")!!
        chat = intent.extras?.getString("chat")!!

        closeable = findViewById(R.id.back_button)
        chatName = findViewById(R.id.username)
        selectLinearLayout = findViewById(R.id.selectLayout)
        finishSelectMode = findViewById(R.id.finish_select_mode)
        selectCountText = findViewById(R.id.select_count)
        deleteIcon = findViewById(R.id.delete_button)

        PushDownAnim.setPushDownAnimTo(deleteIcon)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                showDeleteDialog()
            }

        chatName.text = name
        closeable.setOnClickListener {
            finish()
        }

        loadChat(number)

        adapter = MessageAdapter(this, elements)

        adapter.setOnAddEventListener { view, pos ->
            if(!selectMode){
                if(elements[pos].type == 4 || elements[pos].type == 5){
                    val msg = elements[pos].`object` as MessageElement
                    val event = msg.event
                    setSelectSubject(event)
                }
            }
        }

        adapter.setOnLongClickListener{view, pos ->
            view.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            selectMode = true

            if(!(elements[pos].`object` as MessageElement).isSelect){
                messageSelected++
            }

            (elements[pos].`object` as MessageElement).isSelect = true
            adapter.notifyItemChanged(pos)
            updateToolbar()
        }

        adapter.setOnClickListener { view, pos ->
            if(selectMode){

                if(!(elements[pos].`object` as MessageElement).isSelect){
                    messageSelected++
                    (elements[pos].`object` as MessageElement).isSelect = true
                }else{
                    messageSelected--
                    (elements[pos].`object` as MessageElement).isSelect = false
                }
                adapter.notifyItemChanged(pos)

                if(messageSelected == 0){
                    selectMode = false
                }

                updateToolbar()
            }
        }

        val manager = LinearLayoutManager(this, RecyclerView.VERTICAL, true)
        recyclerView = findViewById(R.id.recyclerView)
        textInput = findViewById(R.id.textInput)
        reply_layout = findViewById(R.id.reply_layout)
        reply_message = findViewById(R.id.message_replyed)
        reply_name = findViewById(R.id.reply_name)
        close_reply = findViewById(R.id.close_reply)

        close_reply.setOnClickListener {
            hideReplyLayout()
        }

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        loadMessages(number)

        val messageController = MessageSwipeController(this, elements, selectMode, object : SwipeControllerActions {
            override fun showReplyUI(position: Int) {
                val message = elements[position].`object` as MessageElement
                reply_id = message.id
                showQuotedMessage(message)
            }
        })

        val itemTouchHelper = ItemTouchHelper(messageController)
        itemTouchHelper.attachToRecyclerView(recyclerView)
        val sendBtn = findViewById<ImageView>(R.id.sendMessage)

        sendBtn.setOnClickListener {
            val text = textInput.text.toString()
            if (text != "") {
                closeSelectMode()
                textInput.setText("")
                checkMessage(text)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferences()
    }

    private fun showDeleteDialog(){
        val dialog = PermissionDialog()
        dialog.setTitleDialog(getString(R.string.delete))
        dialog.setTextDialog(getString(R.string.want_delete_message))
        if(messageSelected > 1){
            dialog.setTitleDialog(getString(R.string.delete)+" "+messageSelected+" "+getString(R.string.messages).toLowerCase())
            dialog.setTextDialog(getString(R.string.want_delete_messages))
        }



        dialog.setDrawable(R.drawable.ic_trash)
        dialog.setPositive { view, which ->
            actionDeleteMessageFromDatabase()
            closeSelectMode()
        }
        dialog.setNeutral { view, which ->
            dialog.dismiss()
        }

        dialog.setNegative { view, which ->
            actionDeleteMessageFromDatabase()
            closeSelectMode()
        }

        dialog.setPositiveText(getString(R.string.del_4_all))
        dialog.setNegativeText(getString(R.string.del_4_me))
        dialog.setNeutralText(getString((R.string.Cancel_M)))
        dialog.show(supportFragmentManager, "Delete Message")
    }

    private fun actionDeleteMessageFromDatabase(){
        val ds : ArrayList<Item> = ArrayList()
        for(e in elements){
            if(e.type in 0..7){
                val m = (e.`object` as MessageElement)
                if(m.isSelect){
                    val id = m.id
                    deleteMessageFromDatabase(id)
                    ds.add(e)
                }
            }
        }

        for(d in ds){
            val m = elements.indexOf(d)
            elements.removeAt(m)
            adapter.notifyItemRemoved(m)
        }
    }


    fun preferences() {
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val b = sharedPreferences.getBoolean("DarkMode", false)
        if (b) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun updateToolbar(){
        if(selectMode){
            val st = if(messageSelected < 10) "0$messageSelected" else "$messageSelected"
            selectCountText.text = st
            finishSelectMode.setOnClickListener {
                closeSelectMode()
            }
            selectLinearLayout.visibility = View.VISIBLE
        }else{
            closeSelectMode()
        }
    }

    private fun closeSelectMode(){
        selectMode = false
        messageSelected = 0
        deselectAll()
        selectLinearLayout.visibility = View.GONE
    }

    private fun deselectAll(){
        for(x in 0 until elements.size){
            (elements[x].`object` as MessageElement).isSelect = false
        }

        adapter.notifyDataSetChanged()
    }

    private fun setSelectSubject(evt : EventMessageElement) {
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
            val n = (elements[position].getObject() as SubjectElement).id.toLong()
            uploadTasks(n, evt)
            subjectDialog.dismiss()
        }
        val add = subjectDialog.findViewById<LinearLayout>(R.id.add_subject)
        add.setOnClickListener { view: View? ->
            subjectDialog.dismiss()
            showSubjectCreator()
        }
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        subjectDialog.show()
    }

    private lateinit var colorD: ImageView
    private var color: Int = 0

    private fun showSubjectCreator() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_subject_creator_layout)
        val title = dialog.findViewById<TextView>(R.id.title_subject)
        val accept = dialog.findViewById<Button>(R.id.accept_subject)
        colorD = dialog.findViewById<ImageView>(R.id.color_select)
        val ta: TypedArray =
            getTheme().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        color = ta.getColor(R.styleable.AppCustomAttrs_palette_yellow, 0)
        colorD.setColorFilter(color)
        ta.recycle()
        val color = dialog.findViewById<LinearLayout>(R.id.color_picker)
        color.setOnClickListener { view: View? -> showColorPicker() }
        accept.setOnClickListener { view: View? ->
            if (!title.text.toString().isEmpty() || title.text.toString() != "") {
                dialog.dismiss()
                insertSubject(title.text.toString())
            } else {
                Toast.makeText(this, R.string.name_is_empty, Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)

    }

    private fun getSubjects(): List<ItemSubjectElement> {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val elements: MutableList<ItemSubjectElement> = ArrayList()
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects} ORDER BY name DESC", null)
        if (cursor.moveToFirst()) {
            do {
                elements.add(ItemSubjectElement(SubjectElement(cursor.getInt(0), cursor.getString(1), cursor.getInt(2)), 2))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return elements
    }

    private fun uploadTasks(s : Long, evt : EventMessageElement){
        val dbHelper = DbHelper(this)
        val db = dbHelper.writableDatabase
        val values = ContentValues()

        values.put("date", evt.date)
        values.put("end_date", evt.endDate)
        values.put("subject", s)
        values.put("description", evt.title)
        values.put("status", 0)
        values.put("user", evt.userId)

        db.insert(DbHelper.T_TASK, null, values)

        updateAddedEvent(evt.message)
        val returnIntent = Intent()
        returnIntent.putExtra("requestCode", 2)
        returnIntent.putExtra("awaitingCode", 2)
        setResult(RESULT_OK, returnIntent)
        adapter.notifyDataSetChanged()
    }

    private fun updateAddedEvent(id : Long){
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase
        val values = ContentValues()

        values.put("added", 1)
        db.update(DbChat.T_CHATS_EVENT, values, "message = '$id'", null)
    }

    override fun onBackPressed() {
        if(selectMode){
            closeSelectMode()
        }else{
            super.onBackPressed()
        }
    }

    fun checkMessage(text: String) {
        var length = text.length
        var start = 0
        if (length > 0) {
            var last = text.substring(length - 1, length)
            while (last == " " && length > 0) {
                length--
                last = text.substring(length - 1, length)
            }
            var first = text.substring(start, start+1)
            while(first == " " && (length - start) > 0){
                start++
                first = text.substring(start, start+1)
            }

            if ((length - start) > 0) {
                val msg = text.substring(start, length)
                sendMessage(msg)
                hideReplyLayout()
            }
        }
    }

    fun deleteMessageFromOtherUsers(id : String){
        val db = FirebaseDatabase.getInstance()
        val ref = db.reference.child("chat").child(chat).child("messages").child(id).child("status")
        ref.setValue(-1)
    }

    fun deleteMessageFromServer(id : String){
        val db = FirebaseDatabase.getInstance()
        db.reference.child("chat").child(chat).child("messages").child(id).removeValue()
    }

    private fun setRetreive(){
    }

    fun deleteMessageFromDatabase(id : String){
        val dbChat = DbChat(this)
        val dbw = dbChat.writableDatabase
        val dbr = dbChat.readableDatabase

        val q = dbr.rawQuery("SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE mId = '$id' OR (id = '$id' AND status == '0')", null)

        if(q.moveToFirst()){
            val mId = q.getInt(0)
            dbw.delete(DbChat.T_CHATS_EVENT, "message = '$mId'", null)
        }

        dbw.delete(DbChat.T_CHATS_MSG, "mId = '$id' OR (id = '$id' AND status == '0')", null)
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
                    val ids = query.getLong(0)
                    val msg = query.getString(1)
                    val ts = query.getLong(3)
                    var id = query.getString(7)
                    val reply = query.getString(9)
                    val status = query.getInt(8)

                    var tm = 0;
                    if (query.getInt(2) == 1) {
                        tm = 1
                    }

                    if(status == 0){
                        id = "$ids"
                    }

                    val ms = MessageElement(id, msg, tm, ts, status, 0, name)

                    if(reply != ""){
                        tm += 2
                        val Msg = getMessageById(reply)
                        ms.messageReplyed = Msg.message

                        if(Msg.mine == 0){
                            ms.theirName = "You"
                        }
                    }else{
                        val cursor = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_EVENT} WHERE message = '$ids'", null)
                        if(cursor.moveToFirst()){
                            tm += 4
                            val eid = cursor.getLong(0)
                            val eTitle = cursor.getString(5)
                            val eUser = cursor.getString(6)
                            val eDate = cursor.getLong(2)
                            val eEndDate = cursor.getLong(3)
                            val e = EventMessageElement("$eid", eTitle, eUser, eDate, eEndDate)
                            e.isAdded = cursor.getInt(7) > 0
                            e.message = cursor.getLong(4)

                            ms.addEvent(e)
                        }
                    }

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
        if(!isMovileDataActive() || !isSaverModeActive()) {
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
                            var reply = ""
                            var temp = -1L
                            var status = 1
                            if (snapshot.child("message").value != null)
                                msg = snapshot.child("message").value.toString()
                            if (snapshot.child("timestamp").value != null)
                                timestamp = snapshot.child("timestamp").value as Long
                            if (snapshot.child("idTemp").value != null)
                                temp = snapshot.child("idTemp").value as Long
                            if (snapshot.child("status").value != null)
                                status = snapshot.child("status").value as Int

                            var tm = 0

                            if (currentUser != auth.currentUser!!.uid) {
                                tm = 1
                            }

                            val isDb = isInDataBase(temp)
                            if (tm == 1 || !isDb) {
                                val dmsg = descryptMessage(msg)

                                val Msg = MessageElement(key, dmsg, tm, timestamp, status, 0, name)
                                val tmm = tm

                                if (snapshot.child("reply").value != null) {
                                    reply = snapshot.child("reply").value.toString()
                                    tm += 2
                                    val ms = getMessageById(reply)
                                    Msg.messageReplyed = ms.message

                                    if (ms.mine == 0) {
                                        Msg.theirName = "You"
                                    }
                                }

                                val d = addMessage(dmsg, key, tmm, timestamp, reply, currentUser)
                                updateMessage(d, key)
                                if(snapshot.child("event").value != null){

                                    val eId = snapshot.child("event").child("eventId").value.toString()
                                    val eTitle = snapshot.child("event").child("eventTitle").value.toString()
                                    val eUserId = snapshot.child("event").child("eventUserId").value.toString()
                                    val eDate = snapshot.child("event").child("eventDate").value as Long
                                    val eEndDate = snapshot.child("event").child("eventEndDate").value as Long
                                    val event = EventMessageElement(eId, eTitle, eUserId, eDate, eEndDate)

                                    tm += 4
                                    Msg.addEvent(event)
                                    uploadEvent(d, event)
                                }

                                if(tmm == 1){
                                    if(status < 3){
                                        val db = FirebaseDatabase.getInstance().reference.child("chat")
                                            .child(chat).child("messages").child(key)
                                        db.setValue(3)
                                    }
                                }


                                elements.add(0, Item(Msg, tm))
                                adapter.notifyItemInserted(0)
                                recyclerView.scrollToPosition(0)
                                val returnIntent = Intent()
                                returnIntent.putExtra("requestCode", 2)
                                setResult(RESULT_OK, returnIntent)
                            } else {
                                updateMessage(temp, key)
                                val d = getElementByMId("$temp")

                                if(d >= 0){
                                    (elements[d].`object` as MessageElement).id = key
                                    (elements[d].`object` as MessageElement).status = 1
                                    adapter.notifyItemChanged(d)
                                    recyclerView.scrollToPosition(0)


                                    val returnIntent = Intent()
                                    returnIntent.putExtra("requestCode", 2)
                                    setResult(RESULT_OK, returnIntent)
                                }
                                if(status == 3){
                                    deleteMessageFromServer(key)
                                }
                            }
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
    }

    private fun uploadEvent(n : Long, event : EventMessageElement){
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase

        val events = ContentValues()
        events.put("chat", chat)
        events.put("date", event.date)
        events.put("end_date", event.endDate)
        events.put("message", n)
        events.put("description", event.title)
        events.put("user", event.userId)
        events.put("added", 1)
        db.insert(DbChat.T_CHATS_EVENT, null, events)
    }

    fun getElementByMId(id : String): Int {

        var i = 0
        for (x in elements) {
            val e = x.`object` as MessageElement
            if(e.id == id){
                return i
            }
            i++
        }

        return -1
    }

    fun isInDataBase(id : Long) : Boolean{
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase

        val q = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE id = '$id'", null)

        if(q.moveToFirst()){
            return true
        }

        q.close()

        return false
    }

    fun updateMessage(id : Long, mId : String){
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase

        val content = ContentValues()

        content.put("mid", mId)
        content.put("status", 1)

        db.update(DbChat.T_CHATS_MSG, content, "id = '$id'", null)
    }

    fun getMessageById(id : String) : MessageElement{
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase
        val qe = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE mId = '$id'", null)
        var ms = MessageElement("", "", 0, 0, 0, 0, "")
        if(qe.moveToFirst()){
            val msg = qe.getString(1)
            val ts = qe.getLong(3)
            val ids = qe.getString(7)

            var tm = 0
            if (qe.getInt(2) == 1) {
                tm = 1
            }
            ms = MessageElement(ids, msg, tm, ts, 0, 0, name)
        }

        qe.close()
        return ms
    }


    private fun addMessageNotSent(msg: String, timestamp: Long, reply_id: String, userId:String): Long {
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase
        val content = ContentValues()
        val now = Calendar.getInstance().timeInMillis
        content.put("MSG", msg)
        content.put("me", 0)
        content.put("name", name)
        content.put("chat", auth.currentUser?.uid)
        content.put("number", number)
        content.put("mid", "$now")
        content.put("timeSend", timestamp)
        content.put("status", 0)
        content.put("reply", reply_id)
        content.put("publicKey", "")
        content.put("user", userId)

        return db.insert(DbChat.T_CHATS_MSG, null, content)
    }

    fun addMessage(msg: String, id: String, me: Int, timestamp: Long, reply_id : String, userId:String) : Long{
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
        content.put("reply", reply_id)
        content.put("publicKey", "")
        content.put("user", userId)

        val n = db.insert(DbChat.T_CHATS_MSG, null, content)
        return n
    }


    private fun sendMessage(msg: String) {
        createChat(number, msg)
        val returnIntent = Intent()
        returnIntent.putExtra("requestCode", 2)
        setResult(RESULT_OK, returnIntent)
    }

    private fun loadChat(number: String) {
        if(!isMovileDataActive() || !isSaverModeActive()) {
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
                                            updateChatCode(chat, number)
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
    }

    fun updateChatCode(c : String, n : String){
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase

        val values = ContentValues()
        values.put("chat", c)

        db.update(DbChat.T_CHATS_MSG, values,"number = '$n'", null)
    }

    fun sendToDatabase(chat: String, msg: String, user: String, reply : String, temp : Long): String {
        val db =
            FirebaseDatabase.getInstance().reference.child("chat").child(chat).child("messages")
                .push()
        val time = Calendar.getInstance().timeInMillis

        val emsg = encryptMessage(msg)

        var hash = mapOf(
            "message" to emsg,
            "userId" to user,
            "timestamp" to time,
            "event" to null,
            "idTemp" to temp,
            "status" to 0
        )
        if(reply != ""){
            hash = mapOf(
                "message" to emsg,
                "userId" to user,
                "timestamp" to time,
                "reply" to reply,
                "event" to null,
                "idTemp" to temp,
                "status" to 0
            )
        }


        db.updateChildren(hash)

        return db.key!!
    }

    override fun onDestroy() {
        cancelListener()
        super.onDestroy()
    }



    fun createChat(number: String, msg: String) {
        val sd = reply_id
        val now = Calendar.getInstance().timeInMillis
        val d = addMessageNotSent(msg, now, sd, auth.currentUser?.uid!!)

        var tm = 0
        val MSG = MessageElement("$d", msg, tm, now, 0, 0, name)

        if(sd != ""){
            tm += 2
            val ms = getMessageById(sd)
            MSG.messageReplyed = ms.message

            if(ms.mine == 0){
                MSG.theirName = "You"
            }
        }

        elements.add(0, Item(MSG, tm))
        adapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(0)

        if(!isMovileDataActive() || !isSaverModeActive()){
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
                                    sendToDatabase(chat, msg, auth.currentUser!!.uid, sd, d)
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

    private fun hideReplyLayout() {
        reply_id = ""
        val resizeAnim = ResizeAnim(reply_layout, currentMessageHeight, 0)
        resizeAnim.duration = ANIMATION_DURATION

        Handler().postDelayed({
            reply_layout.layout(0, -reply_layout.height, reply_layout.width, 0)
            reply_layout.requestLayout()
            reply_layout.forceLayout()
            reply_layout.visibility = View.GONE

        }, ANIMATION_DURATION - 50)

        reply_layout.startAnimation(resizeAnim)
        currentMessageHeight = 0

        resizeAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                val params = reply_layout.layoutParams
                params.height = 0
                reply_layout.layoutParams = params
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
    }




    private fun showQuotedMessage(message: MessageElement) {
        textInput.requestFocus()
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(textInput, InputMethodManager.SHOW_IMPLICIT)

        reply_message.text = message.message
        val height = reply_message.getActualHeight(message.message)
        val startHeight = currentMessageHeight

        if (height != startHeight) {

            if (reply_layout.visibility == View.GONE)
                Handler().postDelayed({
                    reply_layout.visibility = View.VISIBLE
                }, 50)

            val targetHeight = height - startHeight

            val resizeAnim =
                ResizeAnim(
                    reply_layout,
                    startHeight,
                    targetHeight
                )

            resizeAnim.duration = ANIMATION_DURATION
            reply_layout.startAnimation(resizeAnim)

            currentMessageHeight = height
        }
    }

    private fun TextView.getActualHeight(msg :String): Int {
        reply_message.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        return this.measuredHeight + convertToSpToPx(13) + convertToDpToPx(10)
    }

    class ResizeAnim(var view: View, private val startHeight: Int, private val targetHeight: Int) :
        Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            if (startHeight == 0 || targetHeight == 0) {
                view.layoutParams.height =
                    (startHeight + (targetHeight - startHeight) * interpolatedTime).toInt()
            } else {
                view.layoutParams.height = (startHeight + targetHeight * interpolatedTime).toInt()
            }
            view.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    private fun convertToDpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun convertToSpToPx(sp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun descryptMessage(msg: String): String {
        var descryptMessage : String = msg

        return descryptMessage
    }

    private fun encryptMessage(msg: String): String {
        var encryptMessage : String = msg

        return encryptMessage
    }

    private fun isSaverModeActive() : Boolean{
        val s = getSharedPreferences("settings", Context.MODE_PRIVATE)

        return s.getBoolean("datasaver", true)
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
    private fun showColorPicker() {
        val colorSelector = Dialog(this)
        colorSelector.requestWindowFeature(Window.FEATURE_NO_TITLE)
        colorSelector.setContentView(R.layout.bottom_recurrence_layout)
        colorSelector.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        colorSelector.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        colorSelector.window!!.attributes.windowAnimations = R.style.DialogAnimation
        colorSelector.window!!.setGravity(Gravity.BOTTOM)
        val edi = colorSelector.findViewById<LinearLayout>(R.id.color_selecting)
        edi.visibility = View.VISIBLE
        val r = colorSelector.findViewById<RecyclerView>(R.id.recycler)
        val m = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
        elementsC = getColors()
        adapterC = ColorSelectAdapter(this, elementsC) { view: View?, position: Int ->
            color = elementsC.get(position).getColorVibrant()
            colorD.setColorFilter(color)
            colorSelector.dismiss()
        }
        r.layoutManager = m
        r.setHasFixedSize(true)
        r.adapter = adapterC
        colorSelector.show()
    }

    private lateinit var adapterC: ColorSelectAdapter
    private lateinit var elementsC: List<ColorSelectElement>

    fun insertSubject(name: String?) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put("name", name)
        values.put("color", color)
        db.insert(DbHelper.t_subjects, null, values)
    }

    fun getColors(): List<ColorSelectElement> {
        val e: MutableList<ColorSelectElement> = ArrayList()
        e.add(ColorSelectElement("red", Color.parseColor("#f44236"), Color.parseColor("#b90005")))
        e.add(ColorSelectElement("rose", Color.parseColor("#ea1e63"), Color.parseColor("#af0039")))
        e.add(
            ColorSelectElement(
                "purple",
                Color.parseColor("#9c28b1"),
                Color.parseColor("#6a0080")
            )
        )
        e.add(
            ColorSelectElement(
                "purblue",
                Color.parseColor("#673bb7"),
                Color.parseColor("#320c86")
            )
        )
        e.add(ColorSelectElement("blue", Color.parseColor("#3f51b5"), Color.parseColor("#002983")))
        e.add(
            ColorSelectElement(
                "blueCyan",
                Color.parseColor("#2196f3"),
                Color.parseColor("#006ac0")
            )
        )
        e.add(ColorSelectElement("cyan", Color.parseColor("#03a9f5"), Color.parseColor("#007bc1")))
        e.add(
            ColorSelectElement(
                "turques",
                Color.parseColor("#008ba2"),
                Color.parseColor("#008ba2")
            )
        )
        e.add(
            ColorSelectElement(
                "bluegreen",
                Color.parseColor("#009788"),
                Color.parseColor("#00685a")
            )
        )
        e.add(ColorSelectElement("green", Color.parseColor("#4cb050"), Color.parseColor("#087f23")))
        e.add(
            ColorSelectElement(
                "greenYellow",
                Color.parseColor("#8bc24a"),
                Color.parseColor("#5a9215")
            )
        )
        e.add(
            ColorSelectElement(
                "yellowGreen",
                Color.parseColor("#cddc39"),
                Color.parseColor("#99ab01")
            )
        )
        e.add(
            ColorSelectElement(
                "yellow",
                Color.parseColor("#ffeb3c"),
                Color.parseColor("#c8b800")
            )
        )
        e.add(
            ColorSelectElement(
                "yellowOrange",
                Color.parseColor("#fec107"),
                Color.parseColor("#c89100")
            )
        )
        e.add(
            ColorSelectElement(
                "Orangeyellow",
                Color.parseColor("#ff9700"),
                Color.parseColor("#c66901")
            )
        )
        e.add(
            ColorSelectElement(
                "orange",
                Color.parseColor("#fe5722"),
                Color.parseColor("#c41c01")
            )
        )
        e.add(ColorSelectElement("gray", Color.parseColor("#9e9e9e"), Color.parseColor("#707070")))
        e.add(ColorSelectElement("grayb", Color.parseColor("#607d8b"), Color.parseColor("#34525d")))
        e.add(ColorSelectElement("brown", Color.parseColor("#795547"), Color.parseColor("#4a2c21")))
        return e
    }
}