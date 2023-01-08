package com.artuok.appwork.fragmets

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.artuok.appwork.ChatActivity
import com.artuok.appwork.R
import com.artuok.appwork.adapters.ChatAdapter
import com.artuok.appwork.objects.ChatElement
import com.artuok.appwork.objects.Item
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton


class ChatFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatAdapter
    private var elements: ArrayList<Item> = ArrayList()
    private lateinit var task: AverageAsync
    private lateinit var skeleton: Skeleton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val root = inflater.inflate(R.layout.fragment_chat, container, false)

        adapter = ChatAdapter(requireActivity(), elements) { view, pos ->
            var name = (elements[pos].`object` as ChatElement).name
            val intent = Intent(requireActivity(), ChatActivity::class.java)

            intent.putExtra("name", name)
            startActivity(intent)
        }

        var manager = LinearLayoutManager(requireActivity(), VERTICAL, false)
        recycler = root.findViewById(R.id.recycler_chats)

        recycler.setHasFixedSize(true)
        recycler.layoutManager = manager
        recycler.adapter = adapter

        skeleton = recycler.applySkeleton(R.layout.skeleton_chat_layout, 20)

        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppWidgetAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppWidgetAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppWidgetAttrs_maskSkeleton, Color.LTGRAY)

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        skeleton.maskCornerRadius = 150f

        task = AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {
                skeleton.showSkeleton()
            }

            override fun onExecute(b: Boolean) {
                getContacts()
                adapter.notifyDataSetChanged()
            }

            override fun onPostExecute(b: Boolean) {
                skeleton.showOriginal()
            }
        })

        //task.exec(false)
        return root
    }


    fun getContacts(): String {
        val cr = requireActivity().contentResolver
        val cur = cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )

        if ((cur?.count ?: 0) > 0) {
            if (cur?.moveToFirst() == true) {
                do {
                    val id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID))
                    val name =
                        cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                    if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        val pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        if (pCur?.moveToNext() == true) {
                            val phoneNo: String =
                                pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            elements.add(
                                Item(
                                    ChatElement(
                                        Integer.parseInt(id),
                                        name,
                                        "$id - $phoneNo",
                                        ""
                                    ), 0
                                )
                            )
                        }

                        pCur?.close()
                    }
                } while (cur.moveToNext())
            }
        }
        cur?.close()

        return ""
    }
}