package com.zeuskartik.mediaslider

import java.io.Serializable

interface LoadMore : Serializable {
    suspend fun loadMore(): List<SliderItemViewHolder>
}