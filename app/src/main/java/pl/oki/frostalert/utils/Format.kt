package pl.oki.frostalert.utils

fun Float.format(digits: Int) = "%.${digits}f".format(this)
