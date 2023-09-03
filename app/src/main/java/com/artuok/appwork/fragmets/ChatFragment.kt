package com.artuok.appwork.fragmets

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.artuok.appwork.ChatActivity
import com.artuok.appwork.R
import com.artuok.appwork.adapters.ChatAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.library.MessageControler
import com.artuok.appwork.objects.ChatElement
import com.artuok.appwork.objects.Item
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File


class ChatFragment : Fragment() {
    private lateinit var recycler : RecyclerView
    private lateinit var adapter : ChatAdapter
    private lateinit var manager : LinearLayoutManager
    private val chats = ArrayList<Item>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_chat, container, false)
        initilizateViews(root)
        setupRecycler()
        loadChats()
        getAllChatsAndUpdate()
        return root
    }

    private fun initilizateViews(root : View){
        recycler = root.findViewById(R.id.recycler)
    }

    private fun setupRecycler(){
        adapter = ChatAdapter(requireActivity(), chats) { it, pos ->
            val chatElement = chats[pos].`object` as ChatElement
            val i = Intent(requireActivity(), ChatActivity::class.java)
            i.putExtra("name", chatElement.name)
            i.putExtra("id", chatElement.chatId)
            i.putExtra("cachePicture", chatElement.pictureName)
            i.putExtra("chatType", 1)
            startActivity(i)
        }
        manager = LinearLayoutManager(requireActivity(), VERTICAL, false)
        recycler.layoutManager = manager
        recycler.adapter = adapter
    }

    private fun loadChats(){
        val chatData = DbChat(requireActivity())
        val db = chatData.readableDatabase

        chats.clear()
        val query = "SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE timestamp IN " +
                "(SELECT MAX(timestamp) FROM ${DbChat.T_CHATS_MSG} GROUP BY chat) " +
                "ORDER BY timestamp DESC"
        val chats = db.rawQuery(query, null)
        var i = 0
        if(chats.moveToFirst()){
            do {
                val id = chats.getInt(7)
                val message = chats.getString(1)
                val timestamp = chats.getLong(3)
                var status = chats.getInt(5)
                val type = chats.getInt(2)

                if(type == 1){
                    status = -1
                }
                val msg = MessageControler.Message.Builder(message)
                    .setStatus(status)
                    .setTimestamp(timestamp)
                    .build()


                val chatElement = getChatById(id, db, msg)
                if(chatElement != null){
                    this.chats.add(Item(chatElement, 0))
                }

                i++
            }while (chats.moveToNext())
        }
        adapter.notifyDataSetChanged()
        chats.close()
    }

    private fun getChatById(chat : Int, db : SQLiteDatabase, msg : MessageControler.Message) : ChatElement?{
        val chats = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE id = '$chat'", null)
        if(chats.moveToFirst()){
            val name = chats.getString(1)
            val chatId = chats.getString(4)
            val publicKey = chats.getString(6)
            val pictureName = chats.getString(5)
            val picture = getPicture(pictureName)

            chats.close()
            return ChatElement(name, msg.message, chatId, publicKey, pictureName, picture, msg.status, msg.timestamp)
        }
        chats.close()
        return null
    }

    private fun getPicture(photo: String) : Bitmap?{
        val root: String = requireActivity().externalCacheDir.toString()
        val fName = "$photo.jpg"
        val file = File(root, fName)

        if (file.exists()) {
            return BitmapFactory.decodeFile(file.path)
        }
        return null
    }

    private fun getAllChatsAndUpdate(){
        val dbChat = DbChat(requireActivity())
        val dbr = dbChat.readableDatabase
        val c = dbr.rawQuery("SELECT * FROM ${DbChat.T_CHATS}", null)
        val database = FirebaseDatabase.getInstance().reference
        val id = FirebaseAuth.getInstance().currentUser!!.uid
        if(c.moveToFirst()){
            do {
                val code = c.getString(3)
                database
                    .child("chat")
                    .child(code)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if(snapshot.exists()){
                                if(snapshot.child("users").exists() && snapshot.child("type").value.toString().toInt() == 0)
                                    for(user in snapshot.child("users").children){
                                        if(user.key.toString() != id)
                                            getUserAndDownloadPicture(database, user.key.toString(), code)
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            if(error.code == -3){
                                deleteChatInDevice(code)
                            }
                        }

                    })
            }while (c.moveToNext())
        }
        c.close()
    }

    private fun deleteChatInDevice(code : String){
        val dbChat = DbChat(requireActivity())
        val dbw = dbChat.writableDatabase
        val values = ContentValues()
        dbw.delete(DbChat.T_CHATS, "chat = '$code'", null)
    }

    private fun getUserAndDownloadPicture(database: DatabaseReference, user: String, code : String){
        database.child("user")
            .child(user)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        val picture = snapshot.child("photo").value.toString()
                        val name = snapshot.child("name").value.toString()
                        changeInDatabase(code, picture, name)
                        downloadPhoto(user, picture)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun downloadPhoto(user: String, photo : String){
        if(!isAdded)
            return
        val picture = FirebaseStorage.getInstance().reference.child("chats").child(user).child("$photo.jpg")
        val root: String = requireActivity().externalCacheDir.toString()
        val fName = "$photo.jpg"
        val file = File(root, fName)

        if (!file.exists()) {
            picture.getFile(file).addOnCompleteListener {
                if (it.isSuccessful) {
                    loadChats()
                }
            }
        }

    }

    private fun changeInDatabase(uid: String, image : String, name : String){
        if(!isAdded) return
        val dbChat = DbChat(requireActivity())
        val dbw = dbChat.writableDatabase
        val values = ContentValues()
        values.put("image", image)
        values.put("name", name)
        dbw.update(DbChat.T_CHATS, values, "chat = '$uid'", null)

    }

    private interface OnFinishUpdateChats{
        fun onUpdate()
    }
}