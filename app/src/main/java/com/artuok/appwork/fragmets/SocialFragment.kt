package com.artuok.appwork.fragmets

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.R
import com.artuok.appwork.adapters.SocialSearchAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.library.TextViewLetterAnimator
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.UserSearchElement
import com.artuok.appwork.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.Normalizer

class SocialFragment : Fragment() {


    private lateinit var emptyLayout: ConstraintLayout
    private lateinit var resultsLayout: LinearLayout

    private lateinit var searchInput: EditText
    private lateinit var recyclerResults: RecyclerView
    private lateinit var adapter: SocialSearchAdapter
    private lateinit var manager: LinearLayoutManager
    private val elements: ArrayList<Item> = ArrayList()

    private lateinit var logout : LinearLayout
    private lateinit var login : LinearLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_social, container, false)
        login = root.findViewById(R.id.logged)
        logout = root.findViewById(R.id.logout)
        if(FirebaseAuth.getInstance().currentUser != null){
            login.visibility = View.VISIBLE
            logout.visibility = View.GONE
            emptyLayout = root.findViewById(R.id.empty_layout)
            resultsLayout = root.findViewById(R.id.results_layout)

            searchInput = root.findViewById(R.id.search_input)
            recyclerResults = root.findViewById(R.id.recycler_social)
            adapter = SocialSearchAdapter(requireActivity(), elements)
            manager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)

            recyclerResults.layoutManager = manager
            recyclerResults.adapter = adapter
            recyclerResults.setHasFixedSize(false)

            searchInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // No se utiliza
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Realiza la búsqueda cuando el texto cambia
                    if (s.toString().isNotEmpty()) {
                        emptyLayout.visibility = View.GONE
                        resultsLayout.visibility = View.VISIBLE
                        val searchTerm = s.toString().trim()
                        performSearch(searchTerm)
                    } else {
                        emptyLayout.visibility = View.VISIBLE
                        resultsLayout.visibility = View.GONE
                        adapter.clearResults()
                        adapter.notifyDataSetChanged()
                    }

                }

                override fun afterTextChanged(s: Editable?) {
                    // No se utiliza
                }

            })

            adapter.setOnClickListener { view, position ->
                val i = Intent(requireActivity(), ProfileActivity::class.java)

                val item = elements[position]
                if (item.type == 0) {
                    val element = item.`object` as UserSearchElement
                    i.putExtra("cachePicture", element.imageName)
                    i.putExtra("name", element.name)
                    i.putExtra("id", element.id)
                }
                startActivity(i)
            }
        }
        else
        {
            login.visibility = View.GONE
            logout.visibility = View.VISIBLE
            val text = root.findViewById<TextView>(R.id.startSession)

            val textToShow = text.text.toString()
            TextViewLetterAnimator.animateText(text, textToShow, 2000)
        }



        return root
    }


    private fun performSearch(search: String) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("user")

        val normalized = Normalizer.normalize(search.lowercase(), Normalizer.Form.NFD)
        var searchTerm = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        searchTerm = searchTerm.replace("#", "-")

        val query = databaseReference.orderByChild("search").startAt(searchTerm)
            .endAt(searchTerm + "\uf8ff").limitToFirst(20)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adapter.clearResults()

                // Recorre los resultados de la consulta
                for (dataSnapshot in snapshot.children) {
                    val uId = dataSnapshot.key!!
                    if (uId != FirebaseAuth.getInstance().currentUser!!.uid) {
                        val name = dataSnapshot.child("name").value.toString()
                        val code = dataSnapshot.child("code").value.toString()
                        var bitmap: Bitmap? = null
                        var photo = ""
                        if (dataSnapshot.child("photo").exists()) {
                            photo = dataSnapshot.child("photo").value.toString()
                            bitmap = getBitmapPicture(photo)
                            if (bitmap == null) {
                                downloadPictureAndSet(uId, photo, adapter.itemCount)
                            } else {

                            }
                        }

                        var desc = ""
                        var followers = "0"
                        var following = "0"
                        if (dataSnapshot.child("following").exists())
                            following = formatNumber(dataSnapshot.child("following").childrenCount)
                        if (dataSnapshot.child("followers").exists())
                            followers = formatNumber(dataSnapshot.child("followers").childrenCount)

                        if (dataSnapshot.child("description").value != null)
                            desc = dataSnapshot.child("description").value.toString()

                        val element =
                            UserSearchElement(uId, name, desc, followers, following, photo, bitmap)
                        element.code = code

                        verifyAndChageInDatabase(uId, photo, name)
                        adapter.addResult(Item(element, 0))
                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun verifyAndChageInDatabase(uid: String, image : String, name : String){
        if(!isAdded) return
        val dbChat = DbChat(requireActivity())
        val dbr = dbChat.readableDatabase
        val dbw = dbChat.writableDatabase
        val c = dbr.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE code = '$uid'", null)
        if(c.moveToFirst()){
            val values = ContentValues()
            values.put("image", image)
            values.put("name", name)
            dbw.update(DbChat.T_CHATS, values, "code = '$uid'", null)
        }
    }

    private fun downloadPictureAndSet(uid: String, photo: String, n: Int) {
        if (!isAdded) return
        val image = FirebaseStorage.getInstance().reference.child("chats")
            .child(uid)
            .child("$photo.jpg")

        val root: String = requireActivity().externalCacheDir.toString()
        val fName = "$photo.jpg"
        val file = File(root, fName)

        if (!file.exists()) {
            image.getFile(file).addOnCompleteListener {
                if (it.isSuccessful) {
                    val bitmap = BitmapFactory.decodeFile(file.path)
                    adapter.changeImage(n, bitmap)
                    adapter.notifyItemChanged(n)
                }
            }
        }
    }

    private fun getBitmapPicture(photo: String): Bitmap? {
        if (!isAdded)
            return null
        val root: String = requireActivity().externalCacheDir.toString()
        val fName = "$photo.jpg"
        val file = File(root, fName)

        if (file.exists()) {
            return BitmapFactory.decodeFile(file.path)
        }

        return null
    }

    fun formatNumber(number: Long): String {
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
        val formattedDecimal =
            if (nonZeroDecimalPart.isEmpty()) "" else "$decimalSeparator$nonZeroDecimalPart"
        return "${if (number < 0) "-" else ""}${formattedNumber.substringBefore(decimalSeparator)}$formattedDecimal${suffixes[index]}"
    }

    private fun cleanString(input: String): String {
        val cleanString = input.replace("[^a-zA-Z0-9ñÑ ]".toRegex(), "")
        return cleanString.trim()
    }
}
