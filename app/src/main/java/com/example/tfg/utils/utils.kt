package com.example.tfg.utils

import android.view.View
import androidx.core.content.ContextCompat
import com.example.tfg.R
import com.example.tfg.databinding.ItemMediaBinding
import com.example.tfg.model.dataclass.MediaItem

fun porcentaje(mediaItem: MediaItem, b: ItemMediaBinding, itemView: View) {
    val porcentaje = (mediaItem.vote_average * 10).toInt()
    b.tvNota.text = "$porcentaje%"

    b.pbNota.progress = porcentaje
    b.pbNota.secondaryProgress = porcentaje

    val drawableRes = when {
        porcentaje == 0 -> R.drawable.circular_progress_null
        porcentaje >= 70 -> R.drawable.circular_progress_green
        porcentaje in 30..69 -> R.drawable.circular_progress_yellow
        else -> R.drawable.circular_progress_red
    }

    val drawable = ContextCompat.getDrawable(itemView.context, drawableRes)
    b.pbNota.progressDrawable = drawable

    b.pbNota.progressDrawable.level = porcentaje * 100
    b.pbNota.invalidate()
}