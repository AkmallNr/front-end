package com.example.schedo.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.schedo.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ButtonOnBoard(
    text: String = "Next",
    backgroundColor: Color = Utama2,
    textColor: Color = Color.White,
    fontsize: Int = 14,
    onClick: () -> Unit
){
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = textColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            fontSize = fontsize.sp
        )
    }


}

@Preview(showBackground = false)
@Composable
fun NextButtonPreview(){
    ButtonOnBoard(
        text = "Next",
        backgroundColor = Utama2,
        textColor = Color.White
    ){}
}

@Preview(showBackground = false)
@Composable
fun BackButtonPreview(){
    ButtonOnBoard(
        text = "Back",
        backgroundColor = Color.Transparent,
        textColor = Color.Gray
    ){}
}