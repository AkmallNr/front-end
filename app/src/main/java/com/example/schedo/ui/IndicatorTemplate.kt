package com.example.schedo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.schedo.ui.theme.*

@Composable
fun IndicatorTemplate(
    pageSize: Int,
    currentPage: Int,
    selectedColor: Color = Utama2,
    unselectedColor: Color = Grey1
) {

    Row(horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(pageSize){

            Box(modifier = Modifier
                .height(16.dp)
                .width(width = if (it == currentPage) 24.dp else 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color = if (it == currentPage) selectedColor else unselectedColor)
            )

            Spacer(modifier = Modifier.size(2.5.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IndicatorTemplatePreview1() {
    IndicatorTemplate(pageSize = 3, currentPage = 0)
}

@Preview(showBackground = true)
@Composable
fun IndicatorTemplatePreview2() {
    IndicatorTemplate(pageSize = 3, currentPage = 1)
}

@Preview(showBackground = true)
@Composable
fun IndicatorTemplatePreview3() {
    IndicatorTemplate(pageSize = 3, currentPage = 2)
}