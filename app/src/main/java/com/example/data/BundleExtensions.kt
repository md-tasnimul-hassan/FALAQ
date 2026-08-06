package com.example.data

import android.os.Bundle
import android.os.Parcel

fun Bundle.toByteArray(): ByteArray {
    val parcel = Parcel.obtain()
    writeToParcel(parcel, 0)
    val bytes = parcel.marshall()
    parcel.recycle()
    return bytes
}

fun ByteArray.toBundle(): Bundle {
    val parcel = Parcel.obtain()
    parcel.unmarshall(this, 0, size)
    parcel.setDataPosition(0)
    val bundle = Bundle.CREATOR.createFromParcel(parcel)
    parcel.recycle()
    return bundle
}
