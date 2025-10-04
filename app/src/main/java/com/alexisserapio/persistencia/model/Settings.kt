package com.alexisserapio.persistencia.model

import androidx.annotation.ColorRes

data class Settings(
    @ColorRes var bgColor: Int,
    var user: String
)