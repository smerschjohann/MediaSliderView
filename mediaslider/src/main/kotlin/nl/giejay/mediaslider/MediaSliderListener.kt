package nl.giejay.mediaslider

import android.view.KeyEvent

interface MediaSliderListener {
    fun onButtonPressed(keyEvent: KeyEvent): Boolean
}