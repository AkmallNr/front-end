package com.example.schedo.ui

import android.widget.Space
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedo.model.OnBoarding

@Composable
fun OnBoardingTemplate(onBoarding: OnBoarding) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = onBoarding.image),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp), // atur tinggi gambar agar tidak terlalu besar
            alignment = Alignment.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = onBoarding.title,
            fontSize = 22.sp,
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp)) // Jarak antar teks dikurangi

        Text(
            text = onBoarding.description,
            fontSize = 14.sp,
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Preview(showBackground = true)
@Composable
fun OnBoardingTemplatePreview1(){
    OnBoardingTemplate(OnBoarding.FirstPages)
}

@Preview(showBackground = true)
@Composable
fun OnBoardingTemplatePreview2(){
    OnBoardingTemplate(OnBoarding.SecondPages)
}

@Preview(showBackground = true)
@Composable
fun OnBoardingTemplatePreview3(){
    OnBoardingTemplate(OnBoarding.ThirdPages)
}