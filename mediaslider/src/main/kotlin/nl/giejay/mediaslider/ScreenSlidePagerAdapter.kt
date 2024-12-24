package nl.giejay.mediaslider

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.zeuskartik.mediaslider.R
import com.zeuskartik.mediaslider.SliderItem
import com.zeuskartik.mediaslider.SliderItemType
import com.zeuskartik.mediaslider.SliderItemViewHolder
import com.zeuskartik.mediaslider.TouchImageView
import nl.giejay.mediaslider.MediaSliderView.Companion.prepareMedia
import timber.log.Timber

class ScreenSlidePagerAdapter(private val context: Context,
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
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>, isFirstResource: Boolean): Boolean {
                        Timber.e(e, "Could not fetch image: %s", model)
                        hideProgressBar(position)
                        return false
                    }

                    override fun onResourceReady(resource: Drawable,
                                                 model: Any,
                                                 target: com.bumptech.glide.request.target.Target<Drawable>,
                                                 dataSource: com.bumptech.glide.load.DataSource,
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