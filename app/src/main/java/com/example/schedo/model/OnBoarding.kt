package com.example.schedo.model

import com.example.schedo.R
import androidx.annotation.DrawableRes

sealed class OnBoarding(
    @DrawableRes val image: Int,
    val title: String,
    val description: String
) {

    data object FirstPages : OnBoarding(
        image = R.drawable.vectorpage1,
        title = "Selamat Datang di Schedo",
        description = "Buat hidup lebih teratur dengan mencatat semua tugas penting dalam satu tempat."
    )

    data object SecondPages : OnBoarding(
        image = R.drawable.vectorpage2,
        title = "Jangan Lewatkan Apapun",
        description = "Atur pengingat otomatis agar kamu selalu tepat waktu menyelesaikan tugas."
    )

    data object ThirdPages : OnBoarding(
        image = R.drawable.vectorpage3,
        title = "Siap Jadi Lebih Produktif?",
        description = "Mulai kelola harimu dengan lebih baik. Yuk, buat daftar tugas pertamamu sekarang!"
    )
}
