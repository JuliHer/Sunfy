package com.artuok.appwork

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
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
import com.artuok.appwork.adapters.PublicationImageAdapter
import com.artuok.appwork.adapters.ShareAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.fragmets.homeFragment
import com.artuok.appwork.objects.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ViewActivity : AppCompatActivity() {

    private lateinit var subject: TextView
    private lateinit var day: TextView
    private lateinit var desc: TextView
    private lateinit var photo : ImageView
    private lateinit var dayOfWeek : TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PublicationImageAdapter
    private val elements : ArrayList<PublicationImageElement> = ArrayList()
    private lateinit var checkButton : LinearLayout
    private lateinit var shareButton : LinearLayout
    private lateinit var likeButton : LinearLayout
    private var id : Int = 0
    private lateinit var titleTask : String
    private lateinit var userUid : String

    private var endDate : Long = 0

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

        val c = findViewById<ImageView>(R.id.checkButton)

        val a = obtainStyledAttributes(R.styleable.AppCustomAttrs)

        shareButton.setOnClickListener {
            showShares()
        }

        PushDownAnim.setPushDownAnimTo(checkButton)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.95f)
            .setOnClickListener{
                val s = checkTask(id)
                if(s){
                    c.setColorFilter(getColor(R.color.blue_500))
                }else{
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
        if(s){
            c.setColorFilter(getColor(R.color.blue_500))
        }else{
            c.setColorFilter(a.getColor(R.styleable.AppCustomAttrs_subTextColor, 0))
        }
        loadData(id)
        loadImages(id)
        setPhoto()
        enableButton()
    }

    private fun setPhoto() {
        if(userUid != ""){
            if(auth.currentUser != null && (userUid == auth.currentUser?.uid || userUid == "noUser")){
                val root = getExternalFilesDir("Media")

                val cw = ContextWrapper(this)




                val appname = getString(R.string.app_name)
                val myDir = File(root, "$appname Profile")
                if (myDir.exists()) {
                    val fname = appname.uppercase() + "-USER-IMG.jpg"
                    val file = File(myDir, fname)
                    if (file.exists()) {
                        val map = BitmapFactory.decodeFile(file.path)
                        photo.setImageBitmap(map)
                    }
                }
            }else{
                val root = getExternalFilesDir("Media")
                val appname = getString(R.string.app_name)
                val myDir = File(root, ".Profiles")
                if (myDir.exists()) {
                    val fname = appname.uppercase() + "-$userUid-IMG.jpg"
                    val file = File(myDir, fname)
                    if (file.exists()) {
                        val map = BitmapFactory.decodeFile(file.path)
                        photo.setImageBitmap(map)
                    }
                }
            }
        }

    }

    private fun loadImages(id: Int){
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
            if(elements.size > 3){
                2
            }else{
                elements.size
            }

            val manager = GridLayoutManager(this, s, RecyclerView.VERTICAL, false)
                desc.textSize = convertToSpToPx(11).toFloat()
            recyclerView.layoutManager = manager
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
        if (c.moveToFirst() && c.getCount() == 1) {
            val s :Boolean = c.getInt(5) > 0
            sa = s
        }
        c.close()
        return sa
    }

    private fun sendEventMessageToDatabase(msg : MessageElement, chat : ChatElement, event : EventMessageElement){
        val dbChat = DbChat(this)
        val db = dbChat.writableDatabase

        val message = ContentValues()
        val now = Calendar.getInstance().timeInMillis
        message.put("MSG", " 1")
        message.put("me", 0)
        message.put("name", msg.theirName)
        message.put("chat", chat.chat)
        message.put("number", chat.number)
        message.put("mid", "$now")
        message.put("timeSend", now)
        message.put("status", 0)
        message.put("reply", "")
        message.put("publicKey", "")
        val n : Long = db.insert(DbChat.T_CHATS_MSG, null, message)

        val events = ContentValues()
        events.put("chat", chat.chat)
        events.put("date", event.date)
        events.put("end_date", event.endDate)
        events.put("message", n)
        events.put("description", event.title)
        events.put("user", event.userId)
        events.put("added", 1)
        db.insert(DbChat.T_CHATS_EVENT, null, events)

        msg.addEvent(event)
        createMsg(chat.number, msg, n)
    }

    private fun checkTask(id : Int) : Boolean{
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = '$id'", null)
        var sa = false
        if (c.moveToFirst()) {
            val s :Boolean = c.getInt(5) > 0
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

    private fun loadData(id: Int) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = '$id'", null)

        var title = ""
        var user = ""
        var days = ""
        titleTask = ""
        var dow = ""
        if (cursor.moveToFirst()) {
            title = getsubjectNameById(cursor.getInt(3))
            val t = cursor.getLong(2)
            endDate = t
            days = getDayLeft(t)
            titleTask = cursor.getString(4)
            user = cursor.getString(6)
            dow = getDOW(t)
        }


        dayOfWeek.text = dow
        userUid = user
        subject.text = title
        day.text = days
        desc.text = titleTask
        cursor.close()
    }

    private fun getDOW(time : Long) : String{
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
                    dd.add(Item(ChatElement("0", cursor.getString(1), "", cursor.getString(4), cursor.getString(2), "", true, 0), 0))
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
        val cursor = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS_MSG} GROUP BY number ORDER BY timeSend", null)
        val dd : ArrayList<Item> = ArrayList()

        try{
            if(cursor.moveToFirst()){
                do{
                    dd.add(Item(ChatElement("0", cursor.getString(4), "", cursor.getString(5), cursor.getString(6), "", true, 0), 0))
                }while (cursor.moveToNext())
            }
        }finally{
            cursor.close()
        }
        return dd
    }

    private fun getsubjectNameById(id : Int) : String{
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects} WHERE id = '$id'", null)

        var name = ""
        if(c.moveToFirst()){
            name = c.getString(1)
        }

        return name
    }

    private fun convertToSpToPx(sp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun getDayLeft(tim : Long) : String{
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
            datetime += if(c.get(Calendar.AM_PM) == Calendar.AM) " a. m." else " p. m."
            d = datetime
        }

        return d
    }


    private fun createMsg(number : String, Msg : MessageElement, sd : Long){
        if(!isMovileDataActive() || !isSaverModeActive()){
            val key = FirebaseDatabase.getInstance().reference.child("chat").push().key!!
            val db = FirebaseDatabase.getInstance().reference.child("user")
            val query = db.orderByChild("phone").equalTo(number)
            query.addListenerForSingleValueEvent(object : ValueEventListener{
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
                                    sendToDatabase(chat, Msg, auth.currentUser!!.uid, "", sd)
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

    fun sendToDatabase(chat: String, msg: MessageElement, user: String, reply : String, temp : Long): String {
        if(chat == "")
            return ""
        val db =
            FirebaseDatabase.getInstance().reference.child("chat").child(chat).child("messages")
                .push()
        val time = Calendar.getInstance().timeInMillis

        val emsg = msg.message

        val events = msg.event

        val event = mapOf(
            "eventId" to "-1",
            "eventTitle" to events.title,
            "eventUserId" to events.userId,
            "eventDate" to events.date,
            "eventEndDate" to events.endDate
        )

        var hash = mapOf(
            "message" to emsg,
            "userId" to user,
            "timestamp" to time,
            "event" to event,
            "idTemp" to temp
        )
        if(reply != ""){
            hash = mapOf(
                "message" to emsg,
                "userId" to user,
                "timestamp" to time,
                "reply" to reply,
                "event" to event,
                "idTemp" to temp
            )
        }

        db.updateChildren(hash)

        return db.key!!
    }
}


