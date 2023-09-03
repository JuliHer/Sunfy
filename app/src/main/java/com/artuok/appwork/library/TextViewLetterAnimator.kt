package com.artuok.appwork.library

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.os.postDelayed
import kotlinx.coroutines.delay
import javax.annotation.Nullable

class TextViewLetterAnimator constructor(
    private val textView: TextView,
    durationMillis: Long
) {
    private var textToShow = ""
    private val handler = Handler(Looper.getMainLooper())
    private var currentIndex = 0
    private val delayMillis = if(textToShow.isNotEmpty()) durationMillis / textToShow.length else if(textView.text.toString().isNotEmpty()) durationMillis /textView.text.toString().length else 50

    private val textAnimator: Runnable = object : Runnable {
        override fun run() {
            if (currentIndex < textToShow.length) {
                val newText = textToShow.substring(0, currentIndex + 1)
                textView.text = newText
                currentIndex++
                handler.postDelayed(this, delayMillis)
            } else {
                onFinishListener?.onFinish()
            }
        }
    }

    private val textDeletionAnimator: Runnable = object : Runnable {
        override fun run() {
            if (currentIndex >= 0) {
                val newText = textToShow.substring(0, currentIndex)
                textView.text = newText
                currentIndex--
                handler.postDelayed(this, delayMillis)
            } else {
                onFinishListener?.onFinish()
            }
        }
    }

    private var onFinishListener: OnFinishListener? = null

    fun startGenerateAnimation(textToShow : String) {
        currentIndex = 0
        textView.text = ""
        this.textToShow = textToShow
        handler.postDelayed(textAnimator, delayMillis)
    }

    fun startDeleteAnimation(wait : Long) {
        currentIndex = textToShow.length
        handler.postDelayed(textDeletionAnimator, wait)
    }

    fun stopAnimation() {
        handler.removeCallbacks(textAnimator)
        handler.removeCallbacks(textDeletionAnimator)
    }

    fun setOnFinishListener(listener: OnFinishListener) {
        onFinishListener = listener
    }

    companion object {
        private const val DEFAULT_DURATION_MILLIS = 1000L

        fun animateText(textView: TextView, textToShow: String) {
            val animator = TextViewLetterAnimator(textView, DEFAULT_DURATION_MILLIS)
            animator.startGenerateAnimation(textToShow)
        }

        fun animateText(textView: TextView, textToShow: String, durationMillis: Long) {
            val animator = TextViewLetterAnimator(textView, durationMillis)
            animator.startGenerateAnimation(textToShow)
        }

    }

    interface OnFinishListener {
        fun onFinish()
    }
}