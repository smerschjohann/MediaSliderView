package nl.giejay.mediaslider

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.zeuskartik.mediaslider.R
import com.zeuskartik.mediaslider.SliderItem
import com.zeuskartik.mediaslider.SliderItemType
import com.zeuskartik.mediaslider.SliderItemViewHolder
import com.zeuskartik.mediaslider.TouchImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MediaSliderView(context: Context) : ConstraintLayout(context) {
    private var playButton: View
    private var mainHandler: Handler
    private lateinit var mPager: ViewPager
    private lateinit var sliderMediaNumber: TextView
    private lateinit var config: MediaSliderConfiguration

    private var currentPlayerInScope: ExoPlayer? = null
    private var defaultExoFactory = DefaultHttpDataSource.Factory()
    private var slideShowPlaying = false
    private val goToNextAssetRunnable = Runnable { this.goToNextAsset() }
    private var pagerAdapter: ScreenSlidePagerAdapter? = null
    private var loading = false
    private val ioScope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        inflate(getContext(), R.layout.slider, this)
        playButton = findViewById(R.id.playPause)
        playButton.setOnClickListener { v: View? -> toggleSlideshow(true) }
        mainHandler = Handler(Looper.getMainLooper())
    }

//    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
//
//    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
//
//    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (mPager.adapter == null) {
            return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            if ((event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER) && config.items[mPager.currentItem].type == SliderItemType.IMAGE) {
                toggleSlideshow(true)
                return false
            } else if (slideShowPlaying) {
                if (event.keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) {
                    toggleSlideshow(true)
                } else {
                    // remove all current callbacks to prevent multiple runnables
                    mainHandler.removeCallbacks(goToNextAssetRunnable)
                }
                // Go to next photo if dpad right is clicked or just stop
                return super.dispatchKeyEvent(event)
            } else if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mPager.setCurrentItem(if (mPager.adapter!!.count - 1 == mPager.currentItem) 0 else mPager.currentItem + 1, true)
                return false
            } else if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mPager.setCurrentItem((if (0 == mPager.currentItem) mPager.adapter!!.count else mPager.currentItem) - 1, true)
                return false
            }
        }
        return false
    }

    fun loadMediaSliderView(config: MediaSliderConfiguration) {
        this.config = config
        val listener: Player.Listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && slideShowPlaying) {
                    goToNextAsset()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val playPauseButton = findViewById<ImageButton>(R.id.exo_pause)
                playPauseButton?.setImageResource(if (isPlaying) R.drawable.exo_legacy_controls_pause else R.drawable.exo_legacy_controls_play)
            }

            override fun onPlayerError(error: PlaybackException) {
                if (slideShowPlaying) {
                    goToNextAsset()
                }
            }
        }
        initViewsAndSetAdapter(listener)
    }

    private fun setStartPosition() {
        if (config.startPosition >= 0) {
            if (config.startPosition > config.items.size) {
                mPager.currentItem = (config.items.size - 1)
                return
            }
            mPager.currentItem = config.startPosition
        } else {
            mPager.currentItem = 0
        }
        mPager.offscreenPageLimit = 1
    }

    fun toggleSlideshow(togglePlayButton: Boolean) {
        slideShowPlaying = !slideShowPlaying
        if (slideShowPlaying) {
            // do not start timers for videos, they will continue in the player listener
            if (config.items[mPager.currentItem].type == SliderItemType.IMAGE) {
                startTimerNextAsset()
            }
            if (context is Activity) {
                // view is being triggered from main app, prevent app going to sleep
                (context as Activity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            clearKeepScreenOnFlags()
            mainHandler.removeCallbacks(goToNextAssetRunnable)
        }
        if (togglePlayButton) {
            togglePlayButton()
        }
    }

    private fun togglePlayButton() {
        playButton.visibility = VISIBLE
        playButton.setBackgroundResource(if (slideShowPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
        mainHandler.postDelayed({
            playButton.visibility = GONE
        }, 2000)
    }

    private fun startTimerNextAsset() {
        mainHandler.postDelayed(goToNextAssetRunnable, (config.interval * 1000).toLong())
    }

    private fun goToNextAsset() {
        if (mPager.currentItem < mPager.adapter!!.count - 1) {
            mPager.setCurrentItem(mPager.currentItem + 1, config.slideItemIntoView())
        } else {
            mPager.setCurrentItem(0, config.slideItemIntoView())
        }
    }

    private fun initViewsAndSetAdapter(listener: Player.Listener) {
        val statusLayoutLeft = findViewById<RelativeLayout>(R.id.status_holder_left)
        if (config.isGradiantOverlayVisible) {
            statusLayoutLeft.setBackgroundResource(R.drawable.gradient_overlay)
        }

        val slider_clock = findViewById<TextView>(R.id.clock)
        val slider_title_right = findViewById<TextView>(R.id.title_right)
        val slider_subtitle_right = findViewById<TextView>(R.id.subtitle_right)
        val slider_date_right = findViewById<TextView>(R.id.date_right)

        val slider_title_left = findViewById<TextView>(R.id.title_left)
        val slider_subtitle_left = findViewById<TextView>(R.id.subtitle_left)
        val slider_date_left = findViewById<TextView>(R.id.date_left)

        sliderMediaNumber = findViewById(R.id.number)
        val left = findViewById<ImageView>(R.id.left_arrow)
        val right = findViewById<ImageView>(R.id.right_arrow)
        mPager = findViewById(R.id.pager)
        pagerAdapter = ScreenSlidePagerAdapter(
            context, config.items,
            defaultExoFactory,
            config.isOnlyUseThumbnails,
            config.isVideoSoundEnable)
        mPager.setAdapter(pagerAdapter)
        setStartPosition()
        if (config.isClockVisible) {
            slider_clock.visibility = VISIBLE
        }
        if (config.isTitleVisible) {
            slider_title_left.visibility = VISIBLE
            slider_title_right.visibility = VISIBLE
        }
        if (config.isSubtitleVisible) {
            slider_subtitle_right.visibility = VISIBLE
            slider_subtitle_left.visibility = VISIBLE
        }
        if (config.isDateVisible) {
            slider_date_right.visibility = VISIBLE
            slider_date_left.visibility = VISIBLE
        }
        if (config.isMediaCountVisible) {
            sliderMediaNumber.visibility = VISIBLE
            updateMediaCount()
        }
        if (config.isNavigationVisible) {
            left.visibility = VISIBLE
            right.visibility = VISIBLE
            left.setOnClickListener {
                val i = mPager.currentItem
                mPager.setCurrentItem(i - 1)
                updateMediaCount()
            }
            right.setOnClickListener {
                val i = mPager.currentItem
                mPager.setCurrentItem(i + 1)
                updateMediaCount()
            }
        }

        mPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {
                stopPlayer()
                if (i != mPager.currentItem) {
                    return
                }
                val sliderItem = config.items[i]
                setItemText(sliderItem)

                if (config.loadMore != null && mPager.currentItem > config.items.size - 10 && !loading) {
                    loading = true

                    ioScope.launch {
                        val nextItems = config.loadMore!!.invoke()
                        addItemsMain(nextItems)
                        loading = nextItems.isEmpty()
                    }
                }
                updateMediaCount()

                if (sliderItem.type == SliderItemType.VIDEO) {
                    val viewTag = mPager.findViewWithTag<View>("view$i") ?: return
                    val simpleExoPlayerView = viewTag.findViewById<PlayerView>(R.id.video_view)
                    if (simpleExoPlayerView.player != null) {
                        currentPlayerInScope = simpleExoPlayerView.player as ExoPlayer?
                        currentPlayerInScope!!.seekTo(0, 0)
                        if (currentPlayerInScope!!.playbackState == Player.STATE_IDLE) {
                            prepareMedia(sliderItem.url,
                                currentPlayerInScope!!, defaultExoFactory)
                        }
                        currentPlayerInScope!!.addListener(listener)
                        currentPlayerInScope!!.playWhenReady = true
                    }
                }
            }

            override fun onPageSelected(i: Int) {
                val sliderItem = config.items[i]
                if (sliderItem.type == SliderItemType.IMAGE) {
                    if (slideShowPlaying) {
                        startTimerNextAsset()
                    }
                    stopPlayer()
                }

                setItemText(sliderItem)
            }

            fun setItemText(sliderItem: SliderItemViewHolder) {
                slider_title_right.text = sliderItem.descriptionRight
                slider_subtitle_right.text = sliderItem.subtitleRight
                val date = sliderItem.dateRight
                if (date != null) {
                    slider_date_right.text = formatDate(date)
                } else {
                    slider_date_right.text = ""
                }

                if (sliderItem.hasSecondaryItem()) {
                    slider_title_left.text = sliderItem.descriptionLeft
                    slider_subtitle_left.text = sliderItem.subtitleLeft
                    val dateLeft = sliderItem.dateLeft
                    if (dateLeft != null) {
                        slider_date_left.text = formatDate(dateLeft)
                    } else {
                        slider_date_left.text = ""
                    }
                } else {
                    slider_title_left.text = ""
                    slider_subtitle_left.text = ""
                    slider_date_left.text = ""
                }
            }

            override fun onPageScrollStateChanged(i: Int) {
            }
        })
    }

    private fun updateMediaCount() {
        sliderMediaNumber.text = "${(mPager.currentItem + 1)}/${config.items.size}"
    }

    fun onDestroy() {
        if (currentPlayerInScope != null) {
            currentPlayerInScope!!.release()
        }
        clearKeepScreenOnFlags()
        mainHandler.removeCallbacks(goToNextAssetRunnable)
    }

    private fun clearKeepScreenOnFlags() {
        if (context is Activity) {
            // view is being triggered from main app, remove the flags to keep screen on
            val window = (context as Activity).window
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun stopPlayer() {
        if (currentPlayerInScope != null && (currentPlayerInScope!!.isPlaying || currentPlayerInScope!!.isLoading)) {
            currentPlayerInScope!!.stop()
        }
    }

    fun setDefaultExoFactory(defaultExoFactory: DefaultHttpDataSource.Factory) {
        this.defaultExoFactory = defaultExoFactory
    }

    suspend fun addItemsMain(items: List<SliderItemViewHolder>) = withContext(Dispatchers.Main){
        addItems(items)
    }

    private fun addItems(items: List<SliderItemViewHolder>) {
        setItems(Lists.newArrayList(Iterables.concat(config.items, items)).distinct())
    }

    fun setItems(items: List<SliderItemViewHolder>) {
        if (slideShowPlaying) {
            // to prevent timing issues when adding + sliding at the same time
            mainHandler.removeCallbacks(goToNextAssetRunnable)
        }
        config.items = items
        pagerAdapter!!.setItems(items)
        if (slideShowPlaying && config.items[mPager.currentItem].type == SliderItemType.IMAGE) {
            startTimerNextAsset()
        }
    }

    private class ScreenSlidePagerAdapter(private val context: Context,
                                          private var items: List<SliderItemViewHolder>,
                                          private val exoFactory: DefaultHttpDataSource.Factory,
                                          private val onlyUseThumbnails: Boolean,
                                          private val isVideoSoundEnable: Boolean) : PagerAdapter() {
        private var imageView: TouchImageView? = null
        private val progressBars: MutableMap<Int, ProgressBar> = HashMap()
        private var currentVolume = 0f

        fun setItems(items: List<SliderItemViewHolder>) {
            this.items = items
            notifyDataSetChanged()
        }

        fun hideProgressBar(position: Int) {
            val progressBar = progressBars[position]
            if (progressBar != null) {
                progressBar.visibility = GONE
                progressBars.remove(position)
            }
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        @SuppressLint("UnsafeOptInUsageError")
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            var view: View? = null
            val model = items[position]
            if (model.type == SliderItemType.IMAGE) {
                if (model.hasSecondaryItem()) {
                    view = inflater.inflate(R.layout.image_double_item, container, false)
                    loadImageIntoView(view, R.id.left_image, position, model.mainItem)
                    loadImageIntoView(view, R.id.right_image, position, model.secondaryItem!!)
                } else {
                    view = inflater.inflate(R.layout.image_item, container, false)
                    loadImageIntoView(view, R.id.mBigImage, position, model.mainItem)
                }
            } else if (model.type == SliderItemType.VIDEO) {
                view = inflater.inflate(R.layout.video_item, container, false)
                val playerView = view.findViewById<PlayerView>(R.id.video_view)
                playerView.tag = "view$position"
                val player = ExoPlayer.Builder(context)
                    .setLoadControl(DefaultLoadControl.Builder()
                        .setPrioritizeTimeOverSizeThresholds(false)
                        .build()
                    ).build()
                prepareMedia(model.url, player, exoFactory)
                if (!isVideoSoundEnable) {
                    currentVolume = player.volume
                    player.volume = 0f
                }
                player.playWhenReady = false
                playerView.player = player
                val playBtn = playerView.findViewById<ImageButton>(R.id.exo_pause)
                playBtn.setOnClickListener { v: View? ->
                    //events on play buttons
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.currentPosition >= player.contentDuration) {
                            player.seekToDefaultPosition()
                        }
                        player.play()
                    }
                }
            }
            container.addView(view)
            return view!!
        }

        fun loadImageIntoView(imageRootLayout: View, imageViewResource: Int, position: Int, model: SliderItem) {
            imageView = imageRootLayout.findViewById(imageViewResource)
            val progressBar = imageRootLayout.findViewById<ProgressBar>(R.id.mProgressBar)
            if (progressBar != null) {
                progressBars[position] = progressBar
            }
            var glideLoader = Glide.with(context)
                .load(if (onlyUseThumbnails) model.thumbnailUrl else model.url)
                .centerInside() //                        .placeholder(context.getResources().getDrawable(R.drawable.images))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                        Timber.e(e, "Could not fetch image: %s", model)
                        hideProgressBar(position)
                        return false
                    }

                    override fun onResourceReady(resource: Drawable,
                                                 model: Any,
                                                 target: Target<Drawable>,
                                                 dataSource: DataSource,
                                                 isFirstResource: Boolean): Boolean {
                        hideProgressBar(position)
                        return false
                    }
                })
            if (!onlyUseThumbnails) {
                glideLoader = glideLoader.thumbnail(Glide.with(context)
                    .load(model.thumbnailUrl))
            }
            glideLoader.into(imageView!!)
        }

        override fun getCount(): Int {
            return items.size
        }

        override fun isViewFromObject(view: View, o: Any): Boolean {
            return (view === o)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val view = `object` as View
            val exoplayer = view.findViewById<PlayerView>(R.id.video_view)
            if (exoplayer != null && exoplayer.player != null) {
                if (!isVideoSoundEnable) {
                    exoplayer.player!!.volume = currentVolume
                    currentVolume = 0f
                }
                exoplayer.player!!.release()
            } else {
                val imageView = view.findViewById<View>(R.id.mBigImage)
                if (imageView != null) {
                    Glide.with(context).clear(imageView)
                }
            }
            container.removeView(view)
        }
    }

    companion object {
        @SuppressLint("UnsafeOptInUsageError")
        private fun prepareMedia(mediaUrl: String, player: ExoPlayer, factory: DefaultHttpDataSource.Factory) {
            val mediaUri = Uri.parse(mediaUrl)
            val mediaItem = MediaItem.fromUri(mediaUri)
            val mediaSource = ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem)
            player.setMediaSource(mediaSource, 0L)
            player.prepare()
        }

        private fun formatDate(date: Date): String {
            val calendar = Calendar.getInstance()
            calendar.time = date
            val locale = Locale.getDefault(Locale.Category.FORMAT)
            val day = calendar[Calendar.DATE]
            val formatString = when (day) {
                1, 21, 31 -> "EEEE',' d'ˢᵗ' MMMM yyyy"
                2, 22 -> "EEEE',' d'ⁿᵈ' MMMM yyyy"
                3, 23 -> "EEEE',' d'ʳᵈ' MMMM yyyy"
                else -> "EEEE',' d'ᵗʰ' MMMM yyyy"
            }
            return SimpleDateFormat(formatString, locale).format(date)
        }
    }
}