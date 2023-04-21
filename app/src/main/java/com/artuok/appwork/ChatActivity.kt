package com.artuok.appwork

import android.app.Dialog
import android.content.*
import android.content.res.TypedArray
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
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
import com.artuok.appwork.library.MessageSender
import com.artuok.appwork.library.MessageSender.Message
import com.artuok.appwork.library.MessageSender.OnLoadChatListener
import com.artuok.appwork.library.MessageSwipeController
import com.artuok.appwork.library.SwipeControllerActions
import com.artuok.appwork.objects.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView
    private var elements: ArrayList<Item> = ArrayList()
    private var messages: ArrayList<String> = ArrayList()
    private lateinit var textInput: EditText

    private lateinit var number: String
    private lateinit var name: String
    private lateinit var chat: String


    private var idChat: Int = -1
    private var isGroup = false
    private var firstChat = true

    private var auth = FirebaseAuth.getInstance()
    private lateinit var listener: ChildEventListener
    private var loadesdMsgFromDB = false
    private lateinit var reply_layout: CardView
    private lateinit var reply_message: TextView
    private lateinit var reply_name: TextView

    private lateinit var close_reply: TextView
    private var currentMessageHeight = 0
    private var userId = ""
    private var reply_id : String = ""
    private val ANIMATION_DURATION = 200L
    private lateinit var mChat: DatabaseReference
    private var selectMode = false
    private var addWhileOnSelectMode = 0
    private var messageSelected = 0

    private lateinit var closeable: LinearLayout
    private lateinit var chatName: TextView
    private lateinit var selectLinearLayout: LinearLayout
    private lateinit var finishSelectMode: ImageView
    private lateinit var usericon: ImageView
    private lateinit var selectCountText: TextView
    private lateinit var deleteIcon: ImageView
    private lateinit var copyIcon: ImageView

    lateinit var messageSender: MessageSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        idChat = intent.extras?.getInt("id", -1)!!
        firstChat = intent.extras?.getBoolean("first", true)!!
        usericon = findViewById(R.id.usericon)
        loadChatLocal(idChat, firstChat)

        closeable = findViewById(R.id.back_button)
        chatName = findViewById(R.id.username)
        selectLinearLayout = findViewById(R.id.selectLayout)
        finishSelectMode = findViewById(R.id.finish_select_mode)
        selectCountText = findViewById(R.id.select_count)
        deleteIcon = findViewById(R.id.delete_button)
        copyIcon = findViewById(R.id.copy_button)


        PushDownAnim.setPushDownAnimTo(deleteIcon)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                showDeleteDialog()
            }

        PushDownAnim.setPushDownAnimTo(copyIcon)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                actionCopyMessages()
                closeSelectMode()
            }

        chatName.text = name
        closeable.setOnClickListener {
            finish()
        }

        loadChat()

        adapter = MessageAdapter(this, elements)

        adapter.setOnAddEventListener { _, pos ->
            if (!selectMode) {
                if (elements[pos].type == 4 || elements[pos].type == 5) {
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
            //Log.d("cattoChat", "Long selected: $pos")
            (elements[pos].`object` as MessageElement).isSelect = true
            adapter.notifyItemChanged(pos)
            updateToolbar()
        }

        adapter.setOnClickListener { _, pos ->
            if (selectMode) {

                if (!(elements[pos].`object` as MessageElement).isSelect) {
                    messageSelected++
                    (elements[pos].`object` as MessageElement).isSelect = true
                } else {
                    messageSelected--
                    (elements[pos].`object` as MessageElement).isSelect = false
                }
                //Log.d("cattoChat", "Click selected: $pos")
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

        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        loadMessages(idChat)

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

    private fun loadChatLocal(idChat: Int, first: Boolean) {
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase

        if (!first) {
            val query = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE id = '$idChat'", null)

            if (query.moveToFirst()) {
                val name = query.getString(1)
                val chat = query.getString(4)
                val group = query.getInt(2) == 1
                val number = query.getString(3)
                this.number = number
                val image = query.getString(5)
                setPhoto(image)
                messageSender = MessageSender(this, chat, 0)

                this.name = name
                this.chat = chat
                this.isGroup = group
            }

            query.close()
        } else {
            val query =
                db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE id = '$idChat'", null)

            if (query.moveToFirst()) {
                val name = query.getString(1)
                val number = query.getString(2)
                val image = query.getString(5)
                setPhoto(image)
                this.number = number
                this.name = name
                this.chat = ""
                this.isGroup = false

                val quer =
                    db.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE contact = '$number'", null)
                if (quer.moveToFirst()) {
                    this.chat = quer.getString(4)
                    firstChat = false
                    this.idChat = quer.getInt(0)
                    messageSender = MessageSender(this, chat, 0)
                } else {
                    messageSender = MessageSender(this, number)
                }

                quer.close()
            }

            query.close()
        }

        Log.d("cattoChat", "group: $isGroup")
        messageSender.setGroup(isGroup)
    }

    private fun setPhoto(name: String) {
        val root = getExternalFilesDir("Media")
        val path = File(root, ".Profiles")
        val appName = getString(R.string.app_name).uppercase()
        val fileName = "CHAT-$name-$appName.jpg"
        val file = File(path, fileName)
        if (file.exists()) {
            val bit = BitmapFactory.decodeFile(file.path)
            usericon.setImageBitmap(bit)
        }
    }

    private fun loadMessages(chat: Int) {
        if (!loadesdMsgFromDB) {
            val dbChat = DbChat(this)
            val db = dbChat.readableDatabase

            val query = db.rawQuery(
                "SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE chat = '$chat' ORDER BY timeSend DESC",
                null
            )

            if (query.moveToFirst()) {
                loadesdMsgFromDB = true
                var lastTime = 0L
                do {
                    val id = query.getLong(0)
                    val message = query.getString(1)
                    val who = query.getInt(2)
                    val timestamp = query.getLong(3)
                    val mId = query.getString(4)
                    val status = query.getInt(5)
                    val reply = query.getString(6)
                    var type = who

                    val msg = MessageElement(mId, message, who, timestamp, status, 0, name)

                    if (reply != "") {
                        type += 2
                        val messageReplyed = getMessageById(reply)

                        if (messageReplyed.event != null) {
                            msg.messageReplyed =
                                "${getString(R.string.task)}: ${messageReplyed.event.title}"
                        } else {
                            msg.messageReplyed = messageReplyed.message
                        }

                        if (messageReplyed.mine == 0) {
                            msg.theirName = getString(R.string.you)
                        }
                    } else {
                        val cursor = db.rawQuery(
                            "SELECT * FROM ${DbChat.T_CHATS_EVENT} WHERE message = '$id'",
                            null
                        )
                        if (cursor.moveToFirst()) {
                            type += 4
                            val eid = cursor.getLong(0)
                            val eTitle = cursor.getString(5)
                            val eUser = cursor.getString(6)
                            val eDate = cursor.getLong(2)
                            val eEndDate = cursor.getLong(3)
                            val e = EventMessageElement("$eid", eTitle, eUser, eDate, eEndDate)
                            e.isAdded = cursor.getInt(7) > 0
                            e.message = cursor.getLong(4)

                            msg.addEvent(e)
                        }
                        cursor.close()
                    }

                    if (who == 1) {
                        if (isGroup) {
                            updateStatusThatI(this.chat, mId, 3)
                        } else {
                            if (status < 3) {
                                updateStatusInServer(this.chat, mId, 3);
                            }
                        }
                    }
                    if (lastTime != 0L) {
                        val c = Calendar.getInstance()
                        val td = c[Calendar.DAY_OF_YEAR]
                        val ty = c[Calendar.YEAR]
                        c.timeInMillis = timestamp
                        val ld = c[Calendar.DAY_OF_YEAR]
                        val ly = c[Calendar.YEAR]
                        c.timeInMillis = lastTime
                        val ad = c[Calendar.DAY_OF_YEAR]
                        val ay = c[Calendar.YEAR]
                        if (ad != ld || ay != ly) {
                            if (ay == ty) {
                                val r = td - ad

                                if (r == 0) {
                                    elements.add(
                                        Item(
                                            MessageElement(
                                                "",
                                                getString(R.string.today),
                                                3,
                                                0L,
                                                0,
                                                0,
                                                ""
                                            ), 6
                                        )
                                    )
                                } else if (r == 1) {
                                    elements.add(
                                        Item(
                                            MessageElement(
                                                "",
                                                "Yesterday",
                                                3,
                                                0L,
                                                0,
                                                0,
                                                ""
                                            ), 6
                                        )
                                    )
                                } else {
                                    val date = Date()
                                    date.time = lastTime
                                    val format = SimpleDateFormat("dd MMMM")
                                    val time = format.format(date)
                                    elements.add(Item(MessageElement("", time, 3, 0L, 0, 0, ""), 6))
                                }
                            } else {
                                val date = Date()
                                date.time = lastTime
                                val format = SimpleDateFormat("dd MMMM yyyy")
                                val time = format.format(date).uppercase()
                                elements.add(Item(MessageElement("", time, 3, 0L, 0, 0, ""), 6))
                            }
                        }
                    }

                    lastTime = timestamp

                    elements.add(Item(msg, type))
                    messages.add(mId)
                } while (query.moveToNext())
                adapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferences()
    }

    private fun showDeleteDialog() {
        val dialog = PermissionDialog()
        dialog.setTitleDialog(getString(R.string.delete))
        dialog.setTextDialog(getString(R.string.want_delete_message))
        if(messageSelected > 1){
            dialog.setTitleDialog(
                getString(R.string.delete) + " " + messageSelected + " " + getString(
                    R.string.messages
                ).lowercase()
            )
            dialog.setTextDialog(getString(R.string.want_delete_messages))
        }

        dialog.setDrawable(R.drawable.ic_trash)
        dialog.setPositive { _, _ ->
            actionDeleteMessageFromDatabase()
            closeSelectMode()
        }
        dialog.setNeutral { _, _ ->
            dialog.dismiss()
        }

        dialog.setNegative { _, _ ->
            actionDeleteMessageFromDatabase()
            closeSelectMode()
        }

        dialog.setPositiveText(getString(R.string.del_4_all))
        dialog.setNegativeText(getString(R.string.del_4_me))
        dialog.setNeutralText(getString((R.string.Cancel_M)))
        dialog.show(supportFragmentManager, "Delete Message")
    }

    private fun actionCopyMessages() {

        val clipBoard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        var text = ""
        var i = 0
        var lastText = ""
        for (e in elements) {
            if (e.type in 0..7) {
                val m = (e.`object` as MessageElement)
                if (m.isSelect) {
                    val c = Calendar.getInstance()

                    c.timeInMillis = m.timestamp

                    val day = c[Calendar.DAY_OF_MONTH]
                    val month =
                        if ((c[Calendar.MONTH] + 1) < 10) "0${(c[Calendar.MONTH] + 1)}" else "${(c[Calendar.MONTH] + 1)}"
                    val year = c[Calendar.YEAR]
                    val hour = c[Calendar.HOUR_OF_DAY]
                    val minute =
                        if ((c[Calendar.MINUTE]) < 10) "0${(c[Calendar.MINUTE])}" else "${(c[Calendar.MINUTE])}"

                    lastText = m.message
                    text += "${m.theirName} [$day/$month/$year $hour:$minute]: ${m.message} \n\n"
                    i++
                }
            }
        }

        val clipData = if (i == 1) {
            ClipData.newPlainText("Diarify", lastText)
        } else {
            ClipData.newPlainText("Diarify", text)
        }

        clipBoard.setPrimaryClip(clipData)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "text copied!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actionDeleteMessageFromDatabase() {
        val ds: ArrayList<Item> = ArrayList()
        for (e in elements) {
            if (e.type in 0..7) {
                val m = (e.`object` as MessageElement)
                if (m.isSelect) {
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


    private fun preferences() {
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
        val adapter = SubjectAdapter(this, elements) { _: View?, position: Int ->
            val n = (elements[position].getObject() as SubjectElement).id.toLong()
            uploadTasks(n, evt)
            subjectDialog.dismiss()
        }
        val add = subjectDialog.findViewById<LinearLayout>(R.id.add_subject)
        add.setOnClickListener {
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
        colorD = dialog.findViewById(R.id.color_select)
        val ta: TypedArray =
            theme.obtainStyledAttributes(R.styleable.AppCustomAttrs)
        color = ta.getColor(R.styleable.AppCustomAttrs_palette_yellow, 0)
        colorD.setColorFilter(color)
        ta.recycle()
        val color = dialog.findViewById<LinearLayout>(R.id.color_picker)
        color.setOnClickListener { showColorPicker() }
        accept.setOnClickListener {
            if (title.text.toString().isNotEmpty() || title.text.toString() != "") {
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

    private fun updateStatusThatI(chat: String, key: String, status: Int) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("chat").child(chat).child("messages").child(key).child("status")
            .child(auth.currentUser?.uid!!).setValue(true)
    }

    private fun updateStatusInServer(chat: String, key: String, status: Int) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("chat").child(chat).child("messages").child(key).child("status").setValue(status)
    }

    private fun uploadTasks(s: Long, evt: EventMessageElement) {
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

    private fun checkText(text: String): Boolean {
        var length = text.length
        var start = 0
        if (length > 0) {
            var last = text.substring(length - 1, length)
            while (last == " " && length > 0) {
                length--
                if (length <= 0)
                    break
                last = text.substring(length - 1, length)
            }
            var first = text.substring(start, 1)
            while (first == " " && (length - start) > 0) {
                start++
                if (length - start > 0)
                    break
                first = text.substring(start, start + 1)
            }

            if ((length - start) > 0) {
                return true
            }
        }

        return false
    }

    private fun checkMessage(text: String) {
        var length = text.length
        var start = 0
        if (length > 0) {
            var last = text.substring(length - 1, length)
            while (last == " " && length > 0) {
                length--
                if (length <= 0)
                    break
                last = text.substring(length - 1, length)
            }
            var first = text.substring(start, 1)
            while (first == " " && (length - start) > 0) {
                start++
                if (length - start > 0)
                    break
                first = text.substring(start, start + 1)
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

    private fun deleteMessageFromDatabase(id: String) {
        val dbChat = DbChat(this)
        val dbw = dbChat.writableDatabase
        val dbr = dbChat.readableDatabase

        val q = dbr.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE mid = '$id' OR (id = '$id' AND status == '0')",
            null
        )

        if (q.moveToFirst()) {
            val mId = q.getInt(0)
            dbw.delete(DbChat.T_CHATS_EVENT, "message = '$mId'", null)
        }

        dbw.delete(DbChat.T_CHATS_MSG, "mid = '$id' OR (id = '$id' AND status == '0')", null)
        q.close()
    }


    fun cancelListener() {
        if (::mChat.isInitialized && ::listener.isInitialized) {
            mChat.removeEventListener(listener)
        }

    }


    fun onReceiveListener() {
        if (!isMovileDataActive() || !isSaverModeActive()) {
            listener = mChat.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    messageSender.receivingMessage(snapshot) { it, user ->

                        val type = if (user == auth.currentUser?.uid!!) 0 else 1
                        Log.d("CattoChat", "type: $type, received: ${it.key}")
                        if (!messages.contains(it.key) && type == 1) {
                            messages.add(it.key)
                            var view = type
                            val msg =
                                MessageElement(it.key, it.message, type, it.timestamp, 3, 0, name)

                            val isReply = it.reply != null && it.reply != ""
                            val isEvent = it.event != null

                            if (isReply) {
                                view += 2
                                val replied = getMessageById(it.reply)
                                msg.messageReplyed = replied.message
                                if (replied.mine == 0)
                                    msg.theirName = getString(R.string.you)
                            }

                            if (isEvent) {
                                view += 4
                                msg.addEvent(it.event)
                            }

                            elements.add(0, Item(msg, type))
                            adapter.notifyItemInserted(0)
                            recyclerView.scrollToPosition(0)
                        } else if (type == 0) {
                            val d = getElementByMId(it.key)
                            if (d >= 0) {
                                (elements[d].`object` as MessageElement).status = 1
                                adapter.notifyItemChanged(d)
                            }
                        }

                        val returnIntent = Intent()
                        returnIntent.putExtra("requestCode", 2)
                        setResult(RESULT_OK, returnIntent)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    if (snapshot.exists()) {
                        if (snapshot.child("userId").value == null)
                            return

                        val key = snapshot.key!!
                        var status = 1

                        if (snapshot.child("status").value != null)
                            status = (snapshot.child("status").value as Long).toInt()

                        updateStatus(key, status)

                        val d = getElementByMId(key)
                        if (d >= 0) {
                            (elements[d].`object` as MessageElement).status = status
                            adapter.notifyItemChanged(d)
                        }
                    }
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

    private fun updateStatus(key: String, status: Int) {
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase
        val values = ContentValues()
        values.put("status", status)
        db.update(DbChat.T_CHATS_MSG, values, "mid = '$key' AND status < '3'", null)
    }

    private fun uploadEvent(n: Long, event: EventMessageElement) {
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
        return elements
            .asSequence()
            .map { it.`object` as MessageElement }
            .indexOfFirst { it.id == id }
    }

    private fun isInDataBase(id: String): Boolean {
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase

        val q = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE mid = '$id'", null)

        if (q.moveToFirst()) {
            return true
        }
        q.close()
        return false
    }

    fun getMessageById(id : String) : MessageElement{
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase
        val qe = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE mid = '$id'", null)
        var ms = MessageElement("", "", 0, 0, 0, 0, "")
        if(qe.moveToFirst()){
            val mid = qe.getInt(0)
            val msg = qe.getString(1)
            val ts = qe.getLong(3)
            val ids = qe.getString(7)

            var tm = 0
            if (qe.getInt(2) == 1) {
                tm = 1
            }
            ms = MessageElement(ids, msg, tm, ts, 0, 0, name)

            val ev =
                db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_EVENT} WHERE message = '$mid'", null)
            if (ev.moveToFirst()) {
                val eid = ev.getLong(0)
                val eTitle = ev.getString(5)
                val eUser = ev.getString(6)
                val eDate = ev.getLong(2)
                val eEndDate = ev.getLong(3)
                val e = EventMessageElement("$eid", eTitle, eUser, eDate, eEndDate)
                e.isAdded = ev.getInt(7) > 0
                e.message = ev.getLong(4)

                ms.addEvent(e)
            }
            ev.close()
        }

        qe.close()
        return ms
    }

    fun addMessage(
        id: String,
        msg: String,
        me: Int,
        timestamp: Long,
        reply_id: String,
        status: Int,
        number: String
    ): Long {
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase

        val content = ContentValues()

        content.put("message", msg)
        content.put("me", me)
        content.put("timeSend", timestamp)
        content.put("mid", id)
        content.put("status", status)
        content.put("reply", reply_id)
        content.put("number", number)
        content.put("chat", idChat)

        return db.insert(DbChat.T_CHATS_MSG, null, content)
    }

    private fun sendMessage(msg: String) {
        if (firstChat) {
            val key = FirebaseDatabase.getInstance().reference.child("chat").push().key!!
            this.chat = key
            idChat = createChatLocal(idChat, key).toInt()
            firstChat = false
        }


        sendMessage(msg, idChat, chat)
        val returnIntent = Intent()
        returnIntent.putExtra("requestCode", 2)
        setResult(RESULT_OK, returnIntent)
    }

    private fun sendMessage(message: String, n: Int, chatId: String) {
        val replayed = reply_id
        val mes = Message.Builder()
            .setMessage(message)
            .setStatus(0)
            .setReply(replayed)
            .build()

        messageSender.uploadLocalMessage(mes)

        var type = 0
        val msg = MessageElement(mes.key, message, 0, mes.timestamp, 0, 0, name)

        if (replayed != "") {
            type += 2
            val ms = getMessageById(replayed)
            if (ms.message == " 1") {
                msg.messageReplyed = "${getString(R.string.task)}: ${ms.event.title}"
            } else {
                msg.messageReplyed = ms.message
            }

            if (ms.mine == 0) {
                msg.theirName = getString(R.string.you)
            }
        }

        elements.add(0, Item(msg, type))
        adapter.notifyItemInserted(elements.size)
        recyclerView.scrollToPosition(0)

        if (!isMovileDataActive() || !isSaverModeActive()) {
            messageSender.sendMsg(mes) { snapshot, code, userCode, isGroup ->
                cancelListener()
                if (isGroup) {
                    if (snapshot.exists()) {
                        chat = snapshot.key!!
                        mChat =
                            FirebaseDatabase.getInstance().reference.child("chat")
                                .child(chat).child("messages")
                    }
                } else {
                    val db = FirebaseDatabase.getInstance().reference
                    val userMe = FirebaseAuth.getInstance().currentUser!!
                    if (snapshot.exists()) {
                        for (chatc in snapshot.children) {
                            if (chatc.key != null)
                                chat = chatc.key!!
                            updateLocalChatId(chat, n.toLong())
                            mChat =
                                FirebaseDatabase.getInstance().reference.child("chat")
                                    .child(chat).child("messages")
                            db.child("user").child(userCode!!).child("chat").child(chat)
                                .setValue(true)
                            db.child("user").child(userMe.uid)
                                .child("chat").child(chat).setValue(true)
                            break
                        }
                    } else {
                        chat = chatId
                        mChat = FirebaseDatabase.getInstance().reference.child("chat")
                            .child(chat).child("messages")
                        val usersPhones = hashMapOf(
                            userCode to number,
                            userMe.uid to userMe.phoneNumber!!
                        )
                        val hash = mapOf(
                            "code" to code,
                            "users" to usersPhones,
                            "messages" to true,
                            "type" to 0
                        )

                        FirebaseDatabase.getInstance().reference.child("chat")
                            .child(chatId).setValue(hash)
                        db.child("user").child(userCode!!).child("chat").child(chatId)
                            .setValue(true)
                        db.child("user").child(userMe.uid)
                            .child("chat").child(chatId).setValue(true)
                    }
                }
                onReceiveListener()
            }
        }
    }

    private fun updateLocalChatId(chatId: String, n: Long) {
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase
        val values = ContentValues()
        values.put("chat", chatId)

        db.update(DbChat.T_CHATS, values, "id = '$n'", null)
    }

    private fun createChatLocal(idContact: Int, chat: String): Long {
        val dbChat = DbChat(this)
        val dbw = dbChat.writableDatabase
        val dbr = dbChat.readableDatabase
        val query =
            dbr.rawQuery("SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE id = '$idContact'", null)
        val values = ContentValues()

        if (query.moveToFirst()) {
            val name = query.getString(1)
            val number = query.getString(2)
            val image = query.getString(5)
            val publicKey = query.getString(7)

            values.put("name", name)
            values.put("type", 0)
            values.put("contact", number)
            values.put("chat", chat)
            values.put("image", image)
            values.put("publicKey", publicKey)


            query.close()
            return dbw.insert(DbChat.T_CHATS, null, values)
        }

        query.close()
        return -1
    }

    private fun loadChat() {
        if (!isMovileDataActive() || !isSaverModeActive()) {
            messageSender.loadChat(object : OnLoadChatListener {
                override fun onLoadChat(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (childes in snapshot.children) {
                            if (childes.key != null)
                                chat = childes.key!!

                            mChat = FirebaseDatabase.getInstance().reference.child("chat")
                                .child(chat).child("messages")
                            onReceiveListener()
                            break
                        }
                    }
                }

                override fun onFailure(databaseError: DatabaseError) {

                }
            })
        }
    }

    override fun onDestroy() {
        cancelListener()
        super.onDestroy()
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

        if (message.mine == 0) {
            reply_name.text = getString(R.string.you)
        } else {
            reply_name.text = name
        }


        var messages = message.message

        if (messages == " 1") {
            messages = "${getString(R.string.task)}: ${message.event.title}"
        }

        reply_message.text = messages
        val startHeight = currentMessageHeight
        val height = reply_message.getActualHeight()

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

    private fun TextView.getActualHeight(): Int {
        val lines = this.lineCount
        val linesHeight = this.lineHeight
        return (lines * linesHeight) + (linesHeight * 2)
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
        return msg
    }

    private fun encryptMessage(msg: String): String {
        return msg
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
            color = elementsC.get(position).colorVibrant
            colorD.setColorFilter(color)
            colorSelector.dismiss()
        }
        r.layoutManager = m
        r.setHasFixedSize(true)
        r.adapter = adapterC
        colorSelector.show()
    }

    //SubjectCreator
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