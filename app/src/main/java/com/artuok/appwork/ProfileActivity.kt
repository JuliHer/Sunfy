package com.artuok.appwork

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.faltenreich.skeletonlayout.Skeleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File

class ProfileActivity : AppCompatActivity() {
    private lateinit var name : TextView
    private lateinit var usercode : TextView
    private lateinit var followers : TextView
    private lateinit var following : TextView
    private lateinit var description : TextView
    private lateinit var follow : TextView
    private lateinit var profilePicture : ImageView
    private lateinit var followingLayout : LinearLayout
    private val userme = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var id : String
    private lateinit var msgBtn : ImageView
    private val users = FirebaseDatabase.getInstance().reference.child("user")
    private lateinit var editBtn : LinearLayout
    private lateinit var skeleton : Skeleton
    private var photo : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        name = findViewById(R.id.username)
        followers = findViewById(R.id.followers)
        following = findViewById(R.id.following)
        description = findViewById(R.id.description)
        follow = findViewById(R.id.action_btn)
        followingLayout = findViewById(R.id.following_layout)
        editBtn = findViewById(R.id.edit_btn)
        msgBtn = findViewById(R.id.message_btn)
        usercode = findViewById(R.id.usercode)
        profilePicture = findViewById(R.id.picture)
        skeleton = findViewById(R.id.skeleton)

        followers.text = "0"
        following.text = "0"
        val extras = intent.extras
        val username = extras?.getString("name", "Profile")
        id = extras?.getString("id", "none")!!

        if(id == userme){
            follow.visibility = View.GONE
        }

        name.text = username
        usercode.text = username

        findViewById<LinearLayout>(R.id.back_button).setOnClickListener {
            finish()
        }

        skeleton.showSkeleton()
        users.child(id).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    updateProfile(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })

        PushDownAnim.setPushDownAnimTo(msgBtn)
            .setDurationPush(100)
            .setOnClickListener {
                val i = Intent(this@ProfileActivity, ChatActivity::class.java)
                i.putExtra("name", username)
                i.putExtra("id", id)
                i.putExtra("chatType", 0)
                i.putExtra("cachePicture", photo)
                startActivity(i)
            }

        PushDownAnim.setPushDownAnimTo(editBtn)
            .setDurationPush(100)
            .setOnClickListener{
                val i = Intent(this@ProfileActivity, ProfileEditActivity::class.java)

                startActivity(i)
            }

        PushDownAnim.setPushDownAnimTo(follow)
            .setDurationPush(100)
            .setOnClickListener {
                users.child(id).addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                            if(snapshot.child("followers").child(userme).exists()){
                                users
                                    .child(userme)
                                    .child("following")
                                    .child(id).removeValue()
                                users
                                    .child(id)
                                    .child("followers")
                                    .child(userme).removeValue().addOnCompleteListener {
                                        if(it.isSuccessful){
                                            setFollow(false)
                                        }
                                    }
                            }else{
                                users
                                    .child(userme)
                                    .child("following")
                                    .child(id).setValue(true)
                                users
                                    .child(id)
                                    .child("followers")
                                    .child(userme).setValue(true).addOnCompleteListener {
                                        if(it.isSuccessful){
                                            setFollow(true)
                                        }
                                    }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })

            }
    }

    private fun downloadPictureAndSet(uid : String, photo: String){
        val image = FirebaseStorage.getInstance().reference.child("chats")
            .child(uid)
            .child("$photo.jpg")

        val root: String = externalCacheDir.toString()
        val fName = "$photo.jpg"
        val file = File(root, fName)

        if(!file.exists()){
            image.getFile(file).addOnCompleteListener{
                if(it.isSuccessful){
                    val bitmap = BitmapFactory.decodeFile(file.path)
                    profilePicture.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun getBitmapPicture(photo: String) : Bitmap?{
        val root: String = externalCacheDir.toString()
        val fName = "$photo.jpg"
        val file = File(root, fName)

        if (file.exists()) {
            return BitmapFactory.decodeFile(file.path)
        }

        return null
    }

    fun updateProfile(snapshot: DataSnapshot){
        if(snapshot.exists()){
            if(id == userme){
                followingLayout.visibility = View.GONE
                editBtn.visibility = View.VISIBLE
            }else{
                followingLayout.visibility = View.VISIBLE
                editBtn.visibility = View.GONE
                if(snapshot.child("followers").child(userme).exists()){
                    setFollow(true)
                }else{
                    setFollow(false)
                }
            }
            if(snapshot.child("photo").exists()){
                photo = snapshot.child("photo").value.toString()
                val bitmap : Bitmap? = getBitmapPicture(photo)
                if(bitmap == null){
                    downloadPictureAndSet(id, photo)
                }else{
                    profilePicture.setImageBitmap(bitmap)
                }
            }

            description.visibility = View.VISIBLE
            name.text = snapshot.child("name").value.toString()
            usercode.text = name.text
            if(snapshot.child("description").exists())
                description.text = snapshot.child("description").value.toString()
            else {
                description.text = ""
                description.visibility = View.GONE
            }
            if(snapshot.child("followers").exists())
                followers.text = formatNumber(snapshot.child("followers").childrenCount.toString().toLong())
            if(snapshot.child("following").exists())
                following.text = formatNumber(snapshot.child("following").childrenCount.toString().toLong())

            skeleton.showOriginal()
        }
    }

    fun setFollow(follows:Boolean){
        if(follows){
            follow.text = getString(R.string.unfollow)
            val ta = obtainStyledAttributes(R.styleable.AppCustomAttrs)
            val color = ta.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE)
            ta.recycle()
            follow.backgroundTintList = null
            follow.setTextColor(color)
        }else{
            follow.text = getString(R.string.follow)
            val ta = obtainStyledAttributes(R.styleable.AppCustomAttrs)
            val color = ta.getColor(R.styleable.AppCustomAttrs_iMainColor, Color.BLUE)
            val textColor = Color.WHITE;
            ta.recycle()
            follow.backgroundTintList = ColorStateList.valueOf(color)
            follow.setTextColor(textColor)
        }
    }

    fun formatNumber(number : Long) : String{
        val absNumber = Math.abs(number)
        val suffixes = listOf("", "K", "M", "B", "T") // Agrega más si es necesario

        // Encontrar el índice apropiado para el sufijo
        var index = 0
        var num = absNumber.toDouble()
        while (num >= 1000 && index < suffixes.size - 1) {
            num /= 1000
            index++
        }

        // Formatear el número con hasta dos decimales
        val formattedNumber = String.format("%.1f", num)

        // Eliminar ceros redundantes en el decimal (ej: 1.00 => 1)
        val decimalSeparator = if (formattedNumber.contains(",")) "," else "."
        val decimalPart = formattedNumber.substringAfter(decimalSeparator)
        val nonZeroDecimalPart = decimalPart.trimEnd('0')
        val formattedDecimal = if (nonZeroDecimalPart.isEmpty()) "" else "$decimalSeparator$nonZeroDecimalPart"
        return "${if (number < 0) "-" else ""}${formattedNumber.substringBefore(decimalSeparator)}$formattedDecimal${suffixes[index]}"
    }
}