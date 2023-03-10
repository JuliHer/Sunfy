package com.artuok.appwork.dialogs

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.artuok.appwork.R
import com.artuok.appwork.library.AspectRatioView

class ImagePreviewDialog : DialogFragment() {

    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f

    private var WIDTH = 0
    private var HEIGHT = 0
    private var DURATION = 0L

    private var animator : ValueAnimator = ValueAnimator.ofFloat(0f, 1f);
    private lateinit var image : ImageView
    private lateinit var text : TextView
    private lateinit var name : String
    private lateinit var views : Bitmap
    private lateinit var card : AspectRatioView

    private var nul = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val root: View = inflater.inflate(R.layout.dialog_imagepreview_layout, null)
        image = root.findViewById(R.id.usericon)
        text = root.findViewById(R.id.name)
        card = root.findViewById(R.id.cardImage)
        builder.setView(root)

        text.text = name
        if(!nul){
            image.setImageBitmap(views)
        }

        WIDTH = root.width
        HEIGHT = root.height
        return builder.create()
    }


    public fun setStartPosition(x : Float, y : Float){
        startX = x
        startY = y
    }

    public fun setEndPosition(x : Float, y : Float){
        endX = x
        endY = y
    }

    public fun setDuration(milis : Long){
        DURATION = milis
        animator.duration = DURATION
    }

    public fun setImage(img : Bitmap?){
        if(img == null){
            nul = true
        }else{
            nul = false
            views = img
        }
    }

    public fun setText(text : String){
        name = text
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = super.onCreateView(inflater, container, savedInstanceState)

        return root
    }
}