package com.pesan.happyplace.util

import androidx.recyclerview.widget.RecyclerView
import com.pesan.happyplace.activity.MainActivity


abstract class SwipeToDeleteCallback(mainActivity: MainActivity) {

    abstract fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
}