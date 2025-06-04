package com.example.schedo.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.schedo.model.OnBoarding
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnBoardingScreen(onFinished: () -> Unit) {
    val pages = listOf(OnBoarding.FirstPages,OnBoarding.SecondPages,OnBoarding.ThirdPages)

    val pagerState = rememberPagerState(initialPage = 0) {
        pages.size
    }

    val coroutineScope = rememberCoroutineScope()

    val buttonState = remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> listOf("", "Next")
                1 -> listOf("Back", "Next")
                2 -> listOf("Back", "Get Started")
                else -> listOf("", "")
            }
        }
    }


    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol kiri (Back / kosong)
                if (buttonState.value[0].isNotEmpty()) {
                    ButtonOnBoard(text = buttonState.value[0]) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp)) // Biar layout tetap seimbang
                }

                IndicatorTemplate(pageSize = pages.size, currentPage = pagerState.currentPage)

                // Tombol kanan (Next / Get Started)
                ButtonOnBoard(text = buttonState.value[1]) {
                    coroutineScope.launch {
                        if (pagerState.currentPage == pages.lastIndex) {
                            onFinished()
                        } else {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            }
        },
            content = { paddingValues ->
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f) // agar isi mengambil ruang yang tersisa
                    ) { index ->
                        OnBoardingTemplate(onBoarding = pages[index])
                    }
                }
            }
    )
}

@Preview(showBackground = true)
@Composable
fun OnBoardingScreenPreview() {
    OnBoardingScreen {

    }
}
