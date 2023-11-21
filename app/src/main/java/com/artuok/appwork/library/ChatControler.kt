package com.artuok.appwork.library

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import android.util.Log
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.library.Message.ChatType
import com.artuok.appwork.library.Message
import com.artuok.appwork.objects.EventMessageElement
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.MessageElement
import com.artuok.appwork.objects.TextElement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Arrays
import java.util.Calendar
import java.util.TimeZone

class ChatControler(var context: Context, var chatCode: String){
    private val dbChat : DbChat = DbChat(context)
    private val dbr: SQLiteDatabase = dbChat.readableDatabase
    private val dbw: SQLiteDatabase = dbChat.writableDatabase
    private val fDatabase = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    var chatName = ""
    var chatId = -1
    var chat = ""
    var type = ChatType.SEARCH

    var tempId = ""
    var tempUId = ""

    constructor(context: Context, chatCode: String, chatName: String, type: ChatType): this(context, chatCode){
        this.type = type
        this.chatName = chatName
        tempId = chatCode
        tempUId = auth.currentUser?.uid!!
        loadChat(tempId, tempUId)
        initChat()
    }

    init {
        initChat()
    }

    companion object{
        fun setNameChat(context: Context, chatId: String) {
            val fDatabase = FirebaseDatabase.getInstance().reference
            val me = FirebaseAuth.getInstance().currentUser!!.uid
            fDatabase.child("chat")
                .child(chatId)
                .child("users")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (user in snapshot.children) {
                                if (user.key != me) {
                                    fDatabase.child("user").child(user.key!!)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                if (snapshot.exists()) {
                                                    var name = snapshot.child("name").value.toString()
                                                    name = name.ifEmpty { "Anonimo" }
                                                    changeChatName(context, chatId, name)
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        fun existChat(context:Context, key:String) : Boolean{
            val dbChat = DbChat(context)
            val db = dbChat.readableDatabase
            val c = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE chat = ?", arrayOf(key))
            if(c.moveToFirst()){
                c.close()
                return true
            }
            c.close()
            return false
        }

        fun createChat(name: String, id: String, code: String, image: String, publicKey: String, context: Context): Long {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            val currentTimeMillis = calendar.timeInMillis
            val dbChat = DbChat(context)
            val db = dbChat.writableDatabase
            val values = ContentValues()
            values.put("name", name)
            values.put("type", 0)
            values.put("chat", id)
            values.put("code", code)
            values.put("image", image)
            values.put("publicKey", publicKey)
            values.put("updated", currentTimeMillis)
            Log.d("Cattomessage", "Create Chat")
            return db.insert(DbChat.T_CHATS, null, values)
        }

        fun changeChatName(context: Context, chatId: String, newName: String) {
            val dbChat = DbChat(context)
            val dbw = dbChat.readableDatabase
            val values = ContentValues()
            values.put("name", newName)
            dbw.update(DbChat.T_CHATS, values, "chat = ?", arrayOf(chatId))
        }
        fun saveNotify(context: Context, msg: Message, chatId: Int) : Long{
            val dbChat = DbChat(context)
            val dbw = dbChat.writableDatabase
            val values = ContentValues()
            val message = msg.message
            values.put("message", message)
            values.put("type", 1)
            values.put("timestamp", msg.timestamp)
            values.put("mid", msg.id)
            values.put("status", 0)
            values.put("reply", msg.replyId)
            values.put("chat", chatId)

            val i: Long = dbw.insert(DbChat.T_CHATS_MSG, null, values)
            if (msg.task != null) {
                val task = ContentValues()
                task.put("deadline", msg.task.deadline)
                task.put("message", i)
                task.put("description", msg.task.description)
                task.put("user", msg.task.user)
                dbw.insert(DbChat.T_CHATS_EVENT, null, task)
            }
            return i
        }

        fun getChatByString(context: Context, chat: String) : Int{
            val dbChat = DbChat(context)
            val dbr = dbChat.readableDatabase
            val query = "SELECT id FROM ${DbChat.T_CHATS} WHERE chat = ?"
            val c = dbr.rawQuery(query, arrayOf(chat))
            if(c.moveToFirst()){
                val id = c.getInt(0)
                c.close()
                return id
            }
            return -1
        }

        fun updateMessage(context:Context, chatCode: String, msgKey:String, status:Int){
            val dbChat = DbChat(context)
            val dbw = dbChat.writableDatabase
            val ref = FirebaseDatabase.getInstance().reference.child("chat/$chatCode/messages/$msgKey")
                .child("status")
            ref.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        val v = snapshot.value.toString().toInt()
                        if(status > v){
                            ref.setValue(status)
                                .addOnCompleteListener {
                                    val values = ContentValues()
                                    values.put("status", status)
                                    dbw.update(DbChat.T_CHATS_MSG, values, "mid = ?", arrayOf(msgKey))
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("CattoMessage", "Error en la consulta ${error.message}")
                }

            })
        }

        fun checkIfExists(context: Context, key: String): Boolean {
            val dbChat = DbChat(context)
            val dbr = dbChat.readableDatabase
            val c = dbr.rawQuery("SELECT * FROM " + DbChat.T_CHATS_MSG + " WHERE mid = ?", arrayOf(key))
            if (c.moveToFirst()) {
                c.close()
                return true
            }
            c.close()
            return false
        }
    }

    private fun initChat(){
        val query = """
            SELECT * FROM ${DbChat.T_CHATS}
            WHERE ${if(type == ChatType.SEARCH) "code = ?" else "chat = ?"}
        """.trimIndent()
        val c = dbr.rawQuery(query, arrayOf(if(type == ChatType.SEARCH) chatCode else chat))
        if(c.moveToFirst()){
            chatId = c.getInt(0)
            chatName = c.getString(1)
            chat = c.getString(3)
            chatCode = c.getString(4)
        }
        c.close()
    }

    private fun loadChat(id: String, uId: String) {
         if (type == ChatType.SEARCH) {
            val usersArr = arrayOf(
                id, uId
            )
            Arrays.sort(usersArr)
            chatCode = usersArr[0] + usersArr[1]
        } else {
            chat = id
        }
    }

    fun send(msg: Message){
        if(type == ChatType.SEARCH){
            recreateChat(msg)
        }else{
            sendAndSave(msg)
        }
    }

    private fun recreateChat(msg: Message){
        fDatabase.child("chat").orderByChild("code").equalTo(chatCode)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val key: String
                    var chat: Long
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            key = child.key!!
                            chat = chatByKey(key)
                            if (chat < 0) {
                                chat = createChat("", key, chatCode, "", "", context)
                                setNameChat(context, key)
                                chatId = chat.toInt()
                            }
                            this@ChatControler.chat = key
                            break
                        }
                    } else {

                        if(type == ChatType.SEARCH) {
                            val chatHash = java.util.HashMap<String, Any>()
                            chatHash["code"] = chatCode
                            chatHash["messages"] = false
                            chatHash["type"] = 0
                            val userHash = java.util.HashMap<String, Any>()
                            userHash[tempId] = true
                            userHash[tempUId] = true
                            chatHash["users"] = userHash
                            key = fDatabase.child("chat").push().key!!
                            chat = createChat("", key, chatCode, "", "", context)
                            setNameChat(context, key)
                            chatId = chat.toInt()
                            fDatabase.child("chat").child(key).updateChildren(chatHash)
                            this@ChatControler.chat = key
                            fDatabase.child("user").child(tempId).child("chat").child(key)
                                .setValue(true)
                            fDatabase.child("user").child(tempUId).child("chat").child(key)
                                .setValue(true)

                        }
                    }
                    sendAndSave(msg)
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun sendAndSave(msg: Message){
        val values = ContentValues()
        val message = TextUtils.htmlEncode(msg.message)
        values.put("message", message)
        values.put("type", 0)
        values.put("timestamp", msg.timestamp)
        values.put("mid", msg.id)
        values.put("status", 0)
        values.put("reply", msg.replyId)
        values.put("chat", chatId)

        val i: Long = dbw.insert(DbChat.T_CHATS_MSG, null, values)
        if (msg.task != null) {
            val task = ContentValues()
            task.put("deadline", msg.task.deadline)
            task.put("message", i)
            task.put("description", msg.task.description)
            task.put("user", msg.task.user)
            dbw.insert(DbChat.T_CHATS_EVENT, null, task)
        }
        sendToDatabase(msg)
    }

    fun getChatRecycler(): ArrayList<Item>{
        val elements = ArrayList<Item>()
        var time: Long = 0
        val query = """
            SELECT m.*
            FROM ${DbChat.T_CHATS_MSG} AS m
            JOIN ${DbChat.T_CHATS} AS c ON m.chat = c.id 
            WHERE c.chat = ?
        """.trimIndent()
        val c = dbr.rawQuery(query, arrayOf(chat))
        if (c.moveToFirst()) {
            do {
                val id = c.getLong(0)
                val mid = c.getString(4)
                val message = c.getString(1)
                val timestamp = c.getLong(3)
                val type = c.getInt(2)
                val status = c.getInt(5)
                val replyId = c.getString(6)
                if (type != 0) {
                    if (status < 3) {
                        updateMessage(context, chat, mid, 3)
                    }
                }
                val task: EventMessageElement? = getTaskByMessage(dbr, id)
                if (time != 0L) {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = timestamp
                    val daytm = cal[Calendar.DAY_OF_YEAR]
                    cal.timeInMillis = time
                    val daylm = cal[Calendar.DAY_OF_YEAR]
                    if (daytm != daylm) {
                        val te = TextElement(getDate(timestamp))
                        elements.add(0, Item(te, 3))
                    }
                } else if (c.isFirst) {
                    val te = TextElement(getDate(timestamp))
                    elements.add(0, Item(te, 3))
                }
                time = timestamp
                val msg = MessageElement(mid, message, timestamp, type, "", status)
                msg.task = task
                if (task == null) if (replyId != null && replyId.isNotEmpty()) {
                    val reply: MessageElement? = getMsgById(replyId)
                    msg.reply = reply
                    elements.add(0, Item(msg, 1))
                } else {
                    elements.add(0, Item(msg, 0))
                } else elements.add(0, Item(msg, 2))
            } while (c.moveToNext())
        }
        c.close()


        return elements
    }

    private fun chatByKey(key: String): Long {
        val c = dbr.rawQuery("SELECT id FROM ${DbChat.T_CHATS} WHERE chat = ?", arrayOf(key))
        if (c.moveToFirst()) {
            val id = c.getLong(0)
            c.close()
            return id
        }
        c.close()
        return -1
    }

    private fun getTaskByMessage(db: SQLiteDatabase, id: Long): EventMessageElement? {
        val query = """
            SELECT * FROM ${DbChat.T_CHATS_EVENT} WHERE message = ?
        """.trimIndent()
        val c = db.rawQuery(
            query,
            arrayOf("$id")
        )
        if (c.moveToFirst()) {
            val desc = c.getString(3)
            val user = c.getString(4)
            val deadline = c.getLong(1)
            c.close()
            return EventMessageElement(deadline, desc, user)
        }
        c.close()
        return null
    }

    private fun getDate(time: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = time
        return "${c[Calendar.DAY_OF_MONTH]} ${Constants.getMonthMinor(context, c[Calendar.MONTH])} ${c[Calendar.YEAR]}"
    }

    private fun getMsgById(id: String): MessageElement? {
        val query = """
            SELECT * FROM ${DbChat.T_CHATS_MSG}
            WHERE mid = ?
        """.trimIndent()
        val c = dbr.rawQuery(query, arrayOf(id))
        if (c.moveToFirst()) {
            val mid = c.getString(4)
            val message = c.getString(1)
            val timestamp = c.getLong(3)
            val type = c.getInt(2)
            val status = c.getInt(5)
            c.close()
            return MessageElement(mid, message, timestamp, type, chatName, status)
        }
        c.close()
        return null
    }

    fun updateStatus(msgKey:String, status:Int){
        val values = ContentValues()
        values.put("status", status)
        dbw.update(DbChat.T_CHATS_MSG, values, "mid = ?", arrayOf(msgKey))
    }

    fun checkIfExists(key: String): Boolean {
        val c = dbr.rawQuery("SELECT * FROM " + DbChat.T_CHATS_MSG + " WHERE mid = ?", arrayOf(key))
        if (c.moveToFirst()) {
            c.close()
            return true
        }
        c.close()
        return false
    }

    fun insertMessage(msg: Message) {
        var tempChatId = chatByKey(chat)

        val chatId:Int = if(tempChatId >= 0){
            getChatByString(context, chat)
        }else{
            createChat("", chat, chatCode, "", "", context).toInt()
        }

        setNameChat(context, chat)
        val values = ContentValues()
        val message = msg.message
        values.put("message", message)
        values.put("type", 1)
        values.put("timestamp", msg.timestamp)
        values.put("mid", msg.id)
        values.put("status", msg.status)
        values.put("reply", msg.replyId)
        values.put("chat", chatId)
        val i = dbw.insert(DbChat.T_CHATS_MSG, null, values)
        if (msg.task != null) {
            val task = ContentValues()
            task.put("deadline", msg.task.deadline)
            task.put("message", i)
            task.put("description", msg.task.description)
            task.put("user", msg.task.user)
            dbw.insert(DbChat.T_CHATS_EVENT, null, task)
        }
    }



    //Online Methods
    private fun updateMessage(msgKey:String, status:Int){
        fDatabase.child("chat/$chat/messages/$msgKey")
            .child("status")
            .setValue(status)
            .addOnCompleteListener {
                val values = ContentValues()
                values.put("status", status)
                dbw.update(DbChat.T_CHATS_MSG, values, "mid = ?", arrayOf(msgKey))
            }
    }



    private fun sendToDatabase(msg: Message){
        val database = fDatabase.child("chat")
        val user = FirebaseAuth.getInstance().currentUser!!.uid
        val message = HashMap<String, Any?>()
        val purger = msg.message
        message["message"] = purger
        message["timestamp"] = msg.timestamp
        message["status"] = 1
        message["userId"] = user
        if (msg.replyId != null && msg.replyId.isNotEmpty()) {
            message["reply"] = msg.replyId
        }

        message["task"] = msg.task

        val msgKey = msg.id
        database.child(chat).child("messages").child(msgKey).updateChildren(message)
    }

    fun removeMessagesViewed() {
        val database = FirebaseDatabase.getInstance().reference
        database.child("chat").orderByChild("code").equalTo(chatCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val key: String
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            key = child.key!!
                            if (child.child("messages").exists()) {
                                for (message in child.child("messages").children) {
                                    var user = ""
                                    if (message.child("userId").exists()) user =
                                        message.child("userId").value.toString()
                                    var status = 0
                                    if (message.child("status").exists()) status =
                                        message.child("status").value.toString().toInt()
                                    if (status >= 3 && user == tempUId) {
                                        message.ref.removeValue()
                                    }
                                }
                            }
                            break
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}