package com.artuok.appwork.fragmets

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.work.WorkManager
import com.artuok.appwork.ChatActivity
import com.artuok.appwork.MainActivity
import com.artuok.appwork.R
import com.artuok.appwork.adapters.ChatAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.library.MessageSender
import com.artuok.appwork.library.MessageSender.OnLoadMessagesListener
import com.artuok.appwork.objects.ChatElement
import com.artuok.appwork.objects.Item
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseError
import com.google.firebase.ktx.Firebase
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File


class ChatFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatAdapter
    private var elements: ArrayList<Item> = ArrayList()
    private lateinit var task: AverageAsync
    private lateinit var skeleton: Skeleton
    private lateinit var contactsPermission: LinearLayout
    private lateinit var chatRecycler: LinearLayout
    private lateinit var loginView: LinearLayout
    private lateinit var btnAllowPermissions: TextView
    private lateinit var btnLogin: TextView

    private lateinit var auth: FirebaseAuth

    private var resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data!!.getIntExtra("requestCode", 0) == 2) {
                loadChatsMessage()
            }

            if (data.getIntExtra("awaitingCode", 0) == 2) {
                (requireActivity() as MainActivity).notifyAllChanged()
            }
        }
    }

    private var requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean? ->
        if (isGranted == true) {
            contactsPermission.visibility = GONE
            chatRecycler.visibility = VISIBLE
            loadChats()
        } else {
            contactsPermission.visibility = VISIBLE
        }
    }

    private fun registResultLauncher() {
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data!!.getIntExtra("requestCode", 0) == 2) {
                    loadChatsMessage()
                }

                if (data.getIntExtra("awaitingCode", 0) == 2) {
                    (requireActivity() as MainActivity).notifyAllChanged()
                }
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean? ->
            if (isGranted == true) {
                contactsPermission.visibility = GONE
                chatRecycler.visibility = VISIBLE
                loadChats()
            } else {
                contactsPermission.visibility = VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_chat, container, false)

        auth = Firebase.auth

        elements.clear()
        registResultLauncher()
        adapter = ChatAdapter(requireActivity(), elements) { _, pos ->
            val chat = (elements[pos].`object` as ChatElement).id
            val intent = Intent(requireActivity(), ChatActivity::class.java)

            intent.putExtra("id", chat.toInt())
            intent.putExtra("first", false)
            resultLauncher.launch(intent)
        }

        val manager = LinearLayoutManager(requireActivity(), VERTICAL, false)
        contactsPermission = root.findViewById(R.id.contactsPermissions)
        chatRecycler = root.findViewById(R.id.chatRecycler)
        loginView = root.findViewById(R.id.login)
        recycler = root.findViewById(R.id.recycler_chats)
        btnAllowPermissions = root.findViewById(R.id.contactAllowPermissions)
        btnLogin = root.findViewById(R.id.loginButon)

        PushDownAnim.setPushDownAnimTo(btnAllowPermissions)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }

        recycler.setHasFixedSize(true)
        recycler.layoutManager = manager
        recycler.adapter = adapter

        validateUserAndContacts()

        skeleton = recycler.applySkeleton(R.layout.skeleton_chat_layout, 20)

        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)
        ta.recycle()

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        skeleton.maskCornerRadius = 150f

        val workManager = WorkManager.getInstance(requireActivity())
        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE)
        val login = sharedPreferences.getBoolean("logged", false)
        if (!login) {
            workManager.cancelUniqueWork("messagePeriodic")
        }

        //task.exec(false)
        return root
    }

    private fun validateUserAndContacts() {
        loginView.visibility = GONE
        contactsPermission.visibility = VISIBLE
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            contactsPermission.visibility = GONE

            chatRecycler.visibility = VISIBLE
            loadChats()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            showInContextUI()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun showInContextUI() {
        val dialog = PermissionDialog()
        dialog.setTitleDialog(requireActivity().getString(R.string.required_permissions))
        dialog.setTextDialog(requireActivity().getString(R.string.permissions_read_contacts_description))
        dialog.setPositive { it, i ->
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

        dialog.setNegative { _, _ ->
            contactsPermission.visibility = VISIBLE
        }

        dialog.setDrawable(R.drawable.ic_users)
        dialog.show(requireActivity().supportFragmentManager, "permissions")
    }

    private fun loadChats() {
        if (!isAdded)
            return
        val dbChat = DbChat(requireActivity())
        val db = dbChat.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_MSG} GROUP BY chat ORDER BY timeSend DESC",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val chat = cursor.getInt(8)
                val message = cursor.getString(1)
                val query = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE id = '$chat'", null)
                val timestamp = cursor.getLong(3)
                val status = cursor.getInt(5)
                val who = cursor.getInt(2)
                if (query.moveToFirst() && query.count == 1) {
                    val name = query.getString(1)
                    val chatId = query.getString(4)
                    val image = query.getString(5)

                    //query.getString(5)
                    val chatElement =
                        ChatElement("$chat", name, message, chatId, "", "", true, timestamp)
                    if (message == " 1") {
                        chatElement.desc = requireActivity().getString(R.string.task)
                        chatElement.contentIcon =
                            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_book)
                    }

                    val root = requireActivity().getExternalFilesDir("Media")
                    val path = File(root, ".Profiles")
                    val appName =
                        requireActivity().getString(R.string.app_name).uppercase()
                    val fileName = "CHAT-$image-$appName.jpg"
                    val file = File(path, fileName)
                    Log.d("CattoPath", file.path)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.path)
                        chatElement.image = bitmap
                    }

                    if (who == 0) {
                        chatElement.status = status
                    } else {
                        chatElement.status = -1
                    }
                    elements.add(Item(chatElement, 0))
                    adapter.notifyItemInserted(elements.size - 1)
                }
                query.close()
            } while (cursor.moveToNext())
        }
        cursor.close()
        loadGlobalChats()
    }

    private fun loadGlobalChats() {
        val messageSender = MessageSender(requireActivity())
        messageSender.loadGlobalChats();

        messageSender.loadGlobalMessages(object : OnLoadMessagesListener {
            override fun onLoadMessages(newMessages: Boolean) {
                if (newMessages)
                    loadChatsMessage()
            }

            override fun onFailure(databaseError: DatabaseError?) {

            }

        })
    }

    public fun loadChatsMessage() {
        elements.clear()
        adapter.notifyDataSetChanged()
        loadChats()
    }
}