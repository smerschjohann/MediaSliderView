package nl.giejay.mediaslider

import com.zeuskartik.mediaslider.SliderItemViewHolder

typealias LoadMore = suspend () -> List<SliderItemViewHolder>
