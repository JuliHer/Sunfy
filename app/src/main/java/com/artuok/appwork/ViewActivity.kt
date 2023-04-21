package com.artuok.appwork

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.AwaitingAdapter
import com.artuok.appwork.adapters.PublicationImageAdapter
import com.artuok.appwork.adapters.ShareAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.fragmets.SettingsFragment
import com.artuok.appwork.fragmets.homeFragment
import com.artuok.appwork.objects.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.thekhaeng.pushdownanim.PushDownAnim
import java.text.SimpleDateFormat
import java.util.*

class ViewActivity : AppCompatActivity() {

    private lateinit var name: TextView
    private lateinit var subject: TextView
    private lateinit var day: TextView
    private lateinit var desc: TextView
    private lateinit var photo : ImageView
    private lateinit var dayOfWeek : TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PublicationImageAdapter
    private val elements : ArrayList<PublicationImageElement> = ArrayList()
    private lateinit var checkButton : LinearLayout
    private lateinit var shareButton: LinearLayout
    private lateinit var likeButton: LinearLayout
    private lateinit var likeIndicator: ImageView
    private var id: Int = 0
    private lateinit var titleTask: String
    private lateinit var userUid: String
    private var liked = false;

    private var endDate: Long = 0

    private val suggests: ArrayList<Item> = ArrayList()
    private lateinit var awaitRecyclerView: RecyclerView
    private lateinit var adapterAwait: AwaitingAdapter

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)
        val backButton = findViewById<ImageView>(R.id.back_button)
        subject = findViewById(R.id.title_subject)
        dayOfWeek = findViewById(R.id.dayofweek)
        day = findViewById(R.id.days_left)
        desc = findViewById(R.id.description_task)
        checkButton = findViewById(R.id.checkPublication)
        likeButton = findViewById(R.id.likePublication)
        shareButton = findViewById(R.id.sharePublication)
        photo = findViewById(R.id.usericon)
        name = findViewById(R.id.name)
        likeIndicator = findViewById(R.id.image_like)

        awaitRecyclerView = findViewById(R.id.more_views)


        adapterAwait = AwaitingAdapter(this, suggests)
        adapterAwait.setOnClickListener { _, pos ->

        }

        awaitRecyclerView.setHasFixedSize(true)
        awaitRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        awaitRecyclerView.isNestedScrollingEnabled = false
        awaitRecyclerView.adapter = adapterAwait

        val c = findViewById<ImageView>(R.id.checkButton)

        val a = obtainStyledAttributes(R.styleable.AppCustomAttrs)

        shareButton.setOnClickListener {
            showShares()
        }

        PushDownAnim.setPushDownAnimTo(likeButton)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.95f)
            .setOnClickListener {
                liked = !liked
                setLike(liked)
                updateLike(liked)
            }

        PushDownAnim.setPushDownAnimTo(checkButton)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 1.1f)
            .setOnClickListener {
                val s = checkTask(id)
                if (s) {
                    c.setColorFilter(getColor(R.color.blue_500))
                } else {
                    c.setColorFilter(a.getColor(R.styleable.AppCustomAttrs_subTextColor, 0))
                }
            }

        recyclerView = findViewById(R.id.recyclerImages)


        adapter = PublicationImageAdapter(this, elements
        ) { view, pos ->

        }

        val manager = GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)

        recyclerView.layoutManager = manager
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false

        PushDownAnim.setPushDownAnimTo(backButton)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener {
                    finish()
                }
        id = intent.getIntExtra("id", 0)
        val s = isCheckTask(id)
        if (s) {
            c.setColorFilter(getColor(R.color.blue_500))
        } else {
            c.setColorFilter(a.getColor(R.styleable.AppCustomAttrs_subTextColor, 0))
        }
        a.recycle()
        loadData(id)
        loadImages(id)
        preparePendingTasks()
        enableButton()
        loadPendingTasks()
    }

    private fun loadPendingTasks() {
        adapterAwait.notifyItemRangeInserted(0, suggests.size)
    }

    private fun preparePendingTasks() {
        val helper = DbHelper(this)
        val db = helper.readableDatabase
        val c = db.rawQuery(
            "SELECT * FROM ${DbHelper.T_TASK} WHERE status = '0' AND id != '$id' ORDER BY end_date DESC",
            null
        )

        if (c.moveToFirst()) {
            do {
                val cc = Calendar.getInstance()
                val date: Long = c.getLong(2)
                cc.timeInMillis = date
                val day = cc[Calendar.DAY_OF_MONTH]
                val month = cc[Calendar.MONTH]
                val year = cc[Calendar.YEAR]
                val hourFormat = DateFormat.is24HourFormat(this)
                var hour = cc[Calendar.HOUR_OF_DAY]
                if (!hourFormat) hour = if (cc[Calendar.HOUR] == 0) 12 else cc[Calendar.HOUR]

                val minute = cc[Calendar.MINUTE]

                val dd = if (day < 10) "0$day" else "" + day
                val dates = dd + " " + homeFragment.getMonthMinor(
                    this,
                    month
                ) + " " + year + " "
                val mn = if (minute < 10) "0$minute" else "" + minute
                var times = "$hour:$mn"

                if (!hourFormat) {
                    times += if (cc[Calendar.AM_PM] == Calendar.AM) " a. m." else " p. m."
                }

                val done = c.getInt(5) == 1
                val liked = c.getInt(7) == 1
                val subjectName: Int = c.getInt(3)

                val s = db.rawQuery(
                    "SELECT * FROM " + DbHelper.t_subjects + " WHERE id = " + subjectName,
                    null
                )
                var colors = 0
                var subject: String? = ""
                if (s.moveToFirst()) {
                    colors = s.getInt(2)
                    subject = s.getString(1)
                }

                s.close()

                val id: Int = c.getInt(0)
                val title: String = c.getString(4)
                val status: String = daysLeft(true, date)
                val statusColor: Int = statusColor(false, date)

                val eb = AwaitElement(id, title, status, dates, times, colors, statusColor)
                eb.isDone = done
                eb.subject = subject
                eb.isLiked = liked
                suggests.add(Item(eb, 3))
            } while (c.moveToNext())
        }

        c.close()
        loadPendingTasks()
    }

    private fun statusColor(isClosed: Boolean, time: Long): Int {
        val ta: TypedArray = obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val colorB = ta.getColor(
            R.styleable.AppCustomAttrs_subTextColor,
            getColor(R.color.yellow_700)
        )
        ta.recycle()
        val today = Calendar.getInstance()
        val tod = today.timeInMillis
        val rest = (time - tod) / 86400000
        return colorB

    }

    private fun daysLeft(isOpen: Boolean, time: Long): String {
        var d = ""
        val c = Calendar.getInstance()
        c.timeInMillis = time
        val today = Calendar.getInstance()
        val tod = today.timeInMillis
        if (isOpen) {
            var rest = (time - tod) / 86400000
            if (tod < time) {
                val toin = today[Calendar.DAY_OF_YEAR]
                val awin = c[Calendar.DAY_OF_YEAR]
                rest = if (awin < toin) {
                    (awin + 364 - toin).toLong()
                } else {
                    (awin - toin).toLong()
                }
            }
            val dow = c[Calendar.DAY_OF_WEEK]
            if (rest == 1L) {
                d = getString(R.string.tomorrow)
            } else if (rest == 0L) {
                d = getString(R.string.today)
            } else if (rest < 7) {
                if (dow == 1) {
                    d = getString(R.string.sunday)
                } else if (dow == 2) {
                    d = getString(R.string.monday)
                } else if (dow == 3) {
                    d = getString(R.string.tuesday)
                } else if (dow == 4) {
                    d = getString(R.string.wednesday)
                } else if (dow == 5) {
                    d = getString(R.string.thursday)
                } else if (dow == 6) {
                    d = getString(R.string.friday)
                } else if (dow == 7) {
                    d = getString(R.string.saturday)
                }
            } else {
                d = rest.toString() + " " + getString(R.string.day_left)
            }
        }
        return d
    }

    private fun loadImages(id: Int) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_PHOTOS} WHERE awaiting = '$id'", null)

        var i = 0;
        if(cursor.moveToFirst()){
            do{

                if(i < 4){
                    val path = cursor.getString(3)
                    val map = BitmapFactory.decodeFile(path)
                    elements.add(PublicationImageElement(map))
                }else{
                    elements[3].isLast = true
                }

                i++
            }while (cursor.moveToNext())
            adapter.notifyDataSetChanged()
        }

        if(elements.size > 0){

            var s =
                if (elements.size > 3) {
                    2
                } else {
                    elements.size
                }

            val manager = GridLayoutManager(this, s, RecyclerView.VERTICAL, false)
            desc.textSize = convertToSpToPx(11).toFloat()
            recyclerView.layoutManager = manager
        } else {
            recyclerView.visibility = View.GONE
        }
        cursor.close()
    }

    private fun enableButton(){
        val s = getSharedPreferences("chat", MODE_PRIVATE)
        val loggin = s.getBoolean("logged", false)

        if(userUid != "" && userUid != "noUser"){
            if(loggin){
                shareButton.visibility = View.VISIBLE
            }else{
                shareButton.visibility = View.GONE
            }
        }else{
            shareButton.visibility = View.GONE
        }
    }

    private fun isCheckTask(id: Int) : Boolean{
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = '$id'", null)
        var sa = false
        if (c.moveToFirst() && c.count == 1) {
            val s: Boolean = c.getInt(5) > 0
            sa = s
        }
        c.close()
        return sa
    }

    private fun sendEventMessageToDatabase(msg : MessageElement, chat : ChatElement, event : EventMessageElement) {
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase

        val message = ContentValues()
        val now = Calendar.getInstance().timeInMillis
        var key = FirebaseDatabase.getInstance().reference.child("chat").push().key!!
        val chatId =
            if (!chat.isLog) {
                createChatLocal(chat, key)
            } else {
                key = obtainChatId(chat.id.toInt())
                chat.id.toLong()
            }

        val msgKey =
            FirebaseDatabase.getInstance().reference.child("chat").child(key).child("messages")
                .push().key!!

        msg.id = msgKey
        chat.id = chatId.toString()

        message.put("message", " 1")
        message.put("me", 0)
        message.put("timeSend", now)
        message.put("mid", msgKey)
        message.put("status", 0)
        message.put("reply", "")
        message.put("number", chat.number)
        message.put("chat", chatId)
        val n: Long = db.insert(DbChat.T_CHATS_MSG, null, message)

        val events = ContentValues()
        events.put("chat", chatId)
        events.put("date", event.date)
        events.put("end_date", event.endDate)
        events.put("message", n)
        events.put("description", event.title)
        events.put("user", event.userId)
        events.put("added", 1)
        db.insert(DbChat.T_CHATS_EVENT, null, events)

        msg.addEvent(event)
        createMsg(chat.number, msg, n, key)
    }

    private fun obtainChatId(id: Int): String {
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase
        val query = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE id = '$id'", null)
        if (query.moveToFirst()) {
            val chat = query.getString(4)
            query.close()
            return chat
        }
        query.close()
        return ""
    }

    private fun createChatLocal(chat: ChatElement, key: String): Long {
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase

        val values = ContentValues()
        values.put("name", chat.name)
        values.put("type", 0)
        values.put("contact", chat.number)
        values.put("chat", key)
        values.put("image", chat.chat)
        values.put("publicKey", "")

        return db.insert(DbChat.T_CHATS, null, values)
    }

    private fun checkTask(id: Int): Boolean {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = '$id'", null)
        var sa = false
        if (c.moveToFirst()) {
            val s: Boolean = c.getInt(5) > 0
            sa = s
            val values = ContentValues()
            if(sa){
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val dat = format.format(Date())
                values.put("date", dat)
                values.put("status", !s)
                val db2 = dbHelper.writableDatabase
                db2.update(DbHelper.T_TASK, values, " id = '$id'", null)
                db2.close()
            }else{
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val dat = format.format(Date())
                values.put("date", dat)
                values.put("status", !s)
                val db2 = dbHelper.writableDatabase
                db2.update(DbHelper.T_TASK, values, " id = '$id'", null)
                db2.close()
            }
            val returnIntent = Intent()
            returnIntent.putExtra("requestCode", 2)
            returnIntent.putExtra("taskModify", id)
            returnIntent.putExtra("requestCode2", 8)
            setResult(RESULT_OK, returnIntent)

            return !sa
        }

        return false
    }

    private fun getNameById(uid: String): String {
        if (SettingsFragment.isLogged(this)) {
            val dbChat = DbChat(this)
            val db = dbChat.readableDatabase

            val cursor =
                db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE userId = '$uid'", null)

            if (cursor.moveToFirst()) {
                val name = cursor.getString(1)
                cursor.close()
                return name
            }
            cursor.close()
            if (auth.currentUser?.uid!! == uid) {
                return getString(R.string.you)
            }
            return "Unknow"
        }
        return getString(R.string.you)
    }

    private fun loadData(id: Int) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = '$id'", null)

        var title = ""
        var user = ""
        var days = ""
        titleTask = ""
        var color = 0
        var dow = ""
        if (cursor.moveToFirst()) {
            title = getSubjectNameById(cursor.getInt(3))
            color = getSubjectColorById(cursor.getInt(3))
            val t = cursor.getLong(2)
            endDate = t
            days = getDayLeft(t)
            titleTask = cursor.getString(4)

            liked = cursor.getInt(7) == 1
            setLike(cursor.getInt(7) == 1)
            user = cursor.getString(6)
            dow = getDOW(t)
        }


        photo.setColorFilter(color)
        dayOfWeek.text = dow
        userUid = user

        name.text = getNameById(user)
        subject.text = title
        day.text = days
        desc.text = titleTask
        cursor.close()
    }

    private fun setLike(like: Boolean) {
        val a = obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val likedColor = a.getColor(R.styleable.AppCustomAttrs_iMainColor, Color.TRANSPARENT)
        val unlikedColor = a.getColor(R.styleable.AppCustomAttrs_subTextColor, Color.TRANSPARENT)

        a.recycle()
        if (like) {
            likeIndicator.setImageDrawable(getDrawable(R.drawable.heart_fill))
            likeIndicator.setColorFilter(likedColor)
        } else {
            likeIndicator.setImageDrawable(getDrawable(R.drawable.heart))
            likeIndicator.setColorFilter(unlikedColor)
        }
    }

    private fun updateLike(like: Boolean) {
        val dbhelper = DbHelper(this)
        val db = dbhelper.writableDatabase
        val values = ContentValues()
        values.put("favorite", like)

        db.update(DbHelper.T_TASK, values, "id = '$id'", null)
    }

    private fun getDOW(time: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = time

        val d = c[Calendar.DAY_OF_WEEK]

        val day = homeFragment.getDayOfWeek(this, d)

        return day
    }

    private fun showShares(){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_share_layout)
        val recycler = dialog.findViewById<RecyclerView>(R.id.recycler)
        var elements : ArrayList<Item> = getUserShares()
        if(elements.size == 0){
            elements = getUserLogged()
        }
        val adapter = ShareAdapter(this, elements)

        val manager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        adapter.setOnClickListener { view, pos ->

            val c = elements.get(pos).`object` as ChatElement
            val m = MessageElement("0", " 1", 0, 0, 0, 0, c.name)
            m.messageReplyed = ""
            val tim = Calendar.getInstance().timeInMillis
            val ed = endDate
            val em = EventMessageElement("0", titleTask, auth.currentUser?.uid!!, tim, ed)
            sendEventMessageToDatabase(m, c, em)
            val returnIntent = Intent()
            returnIntent.putExtra("shareCode", 2)
            setResult(RESULT_OK, returnIntent)
            dialog.dismiss()
        }

        recycler.setHasFixedSize(false)
        recycler.layoutManager = manager
        recycler.adapter = adapter


        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun getUserLogged(): ArrayList<Item> {
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE isLog = '1' GROUP BY phone ORDER BY name", null)
        val dd : ArrayList<Item> = ArrayList()

        try{
            if(cursor.moveToFirst()){
                do{
                    dd.add(
                        Item(
                            ChatElement(
                                "${cursor.getInt(0)}",
                                cursor.getString(1),
                                "",
                                "",
                                cursor.getString(2),
                                "",
                                false,
                                0
                            ), 0
                        )
                    )
                }while (cursor.moveToNext())
            }
        }finally{
            cursor.close()
        }
        return dd
    }

    private fun getUserShares(): ArrayList<Item> {
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase
        val curso = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_MSG} GROUP BY chat ORDER BY timeSend DESC",
            null
        )
        val dd: ArrayList<Item> = ArrayList()

        curso.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val chat = cursor.getInt(8)
                    val query =
                        db.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE id = '$chat'", null)
                    if (query.moveToFirst()) {
                        dd.add(
                            Item(
                                ChatElement(
                                    chat.toString(),
                                    query.getString(1),
                                    "",
                                    "",
                                    query.getString(3),
                                    "",
                                    true,
                                    0
                                ), 0
                            )
                        )
                    }
                    query.close()
                } while (cursor.moveToNext())
            }
        }
        return dd
    }

    private fun getSubjectNameById(id: Int): String {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects} WHERE id = '$id'", null)

        var name = ""
        if (c.moveToFirst()) {
            name = c.getString(1)
        }

        return name
    }

    private fun getSubjectColorById(id: Int): Int {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects} WHERE id = '$id'", null)

        var color = 0
        if (c.moveToFirst()) {
            color = c.getInt(2)
        }

        return color
    }

    private fun convertToSpToPx(sp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun getDayLeft(tim: Long): String {
        val d : String
        val c = Calendar.getInstance()
        c.timeInMillis = tim

        val today = Calendar.getInstance()

        val tod = today.timeInMillis

        var rest: Long = (tim - tod) / 86400000

        if (tod < tim) {
            val toin = today[Calendar.DAY_OF_YEAR]
            val awin = c[Calendar.DAY_OF_YEAR]
            rest = if (awin < toin) {
                (awin + 364 - toin).toLong()
            } else {
                (awin - toin).toLong()
            }
        }

        val open = tod < tim

        if (open) {
            if (rest == 1L) {
                d = getString(R.string.tomorrow)
            } else if (rest == 0L) {
                d = getString(R.string.today)
            } else if (rest < 7) {
                d = rest.toString() + " " + getString(R.string.day_left)
            } else {
                val day = c.get(Calendar.DAY_OF_MONTH)
                val month = c.get(Calendar.MONTH)
                val year = c.get(Calendar.YEAR)
                val hour = if(c.get(Calendar.HOUR) == 0) 12 else c.get(Calendar.HOUR)
                val minute = c.get(Calendar.MINUTE)

                val dd = if (day < 10) "0$day" else "$day"
                var datetime = "$dd " + homeFragment.getMonthMinor(this, month) + " $year "
                val mn = if (minute < 10) "0$minute" else "" + minute
                datetime += "$hour:$mn"
                datetime += if(c.get(Calendar.AM_PM) == Calendar.AM) " a. m." else " p. m."
                d = datetime
            }
        }else {
            val day = c.get(Calendar.DAY_OF_MONTH)
            val month = c.get(Calendar.MONTH)
            val year = c.get(Calendar.YEAR)
            val hour = if(c.get(Calendar.HOUR) == 0) 12 else c.get(Calendar.HOUR)
            val minute = c.get(Calendar.MINUTE)

            val dd = if (day < 10) "0$day" else "$day"
            var datetime = "$dd " + homeFragment.getMonthMinor(this, month) + " $year "
            val mn = if (minute < 10) "0$minute" else "" + minute
            datetime += "$hour:$mn"
            datetime += if (c.get(Calendar.AM_PM) == Calendar.AM) " a. m." else " p. m."
            d = datetime
        }

        return d
    }


    private fun createMsg(number: String, Msg: MessageElement, sd: Long, key: String) {
        if (!SettingsFragment.isMobileData(this) || !SettingsFragment.isSaverModeActive(this)) {
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
                                .orderByChild("code").equalTo(users)
                            nochat.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    var chat : String = ""
                                    if (snapshot.exists()) {
                                        for (childes in snapshot.children) {
                                            if (childes.key != null)
                                                chat = childes.key!!
                                            break
                                        }
                                    } else {
                                        chat = key
                                        val hash = mapOf(
                                            "code" to users,
                                            "messages" to true,
                                            "type" to 0
                                        )

                                        FirebaseDatabase.getInstance().reference.child("chat")
                                            .child(key).setValue(hash)
                                        db.child(name).child("chat").child(key).setValue(true)
                                        db.child(FirebaseAuth.getInstance().currentUser!!.uid)
                                            .child("chat").child(key).setValue(true)
                                    }
                                    sendToDatabase(chat, number, Msg, auth.currentUser!!.uid)
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


    fun sendToDatabase(chat: String, number: String, msg: MessageElement, user: String) {
        val messageRef =
            FirebaseDatabase.getInstance().reference.child("chat").child(chat).child("messages")
                .child(msg.id)
        val now = Calendar.getInstance().timeInMillis
        val emsg = msg.message

        val events = msg.event

        val event = mapOf(
            "eventId" to "-1",
            "eventTitle" to events.title,
            "eventUserId" to events.userId,
            "eventDate" to events.date,
            "eventEndDate" to events.endDate
        )

        val hash = mapOf(
            "message" to emsg,
            "status" to 1,
            "number" to number,
            "userId" to user,
            "timestamp" to now,
            "event" to event
        )

        messageRef.updateChildren(hash)

    }
}


