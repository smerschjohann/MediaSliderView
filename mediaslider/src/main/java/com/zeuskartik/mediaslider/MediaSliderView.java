package com.zeuskartik.mediaslider;

import static androidx.media3.common.Player.STATE_IDLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class MediaSliderView extends ConstraintLayout {
    private View playButton;
    private Handler mainHandler;
    private ViewPager mPager;
    private TextView slider_media_number;
    private ExoPlayer currentPlayerInScope;
    private DefaultHttpDataSource.Factory defaultExoFactory = new DefaultHttpDataSource.Factory();
    private boolean slideShowPlaying;
    private final Runnable goToNextAssetRunnable = this::goToNextAsset;
    private MediaSliderConfiguration config;
    private List<SliderItem> items;
    private ScreenSlidePagerAdapter pagerAdapter;

    public MediaSliderView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.slider, this);
        playButton = findViewById(R.id.playPause);
        playButton.setOnClickListener(v -> toggleSlideshow(true));
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public MediaSliderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaSliderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MediaSliderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mPager == null || mPager.getAdapter() == null) {
            return super.dispatchKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && items.get(mPager.getCurrentItem()).getType() == SliderItemType.IMAGE) {
                toggleSlideshow(true);
                return false;
            } else if (slideShowPlaying) {
                if (event.getKeyCode() != KeyEvent.KEYCODE_DPAD_RIGHT) {
                    toggleSlideshow(true);
                } else {
                    // remove all current callbacks to prevent multiple runnables
                    mainHandler.removeCallbacks(goToNextAssetRunnable);
                }
                // Go to next photo if dpad right is clicked or just stop
                return super.dispatchKeyEvent(event);
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT && mPager.getAdapter().getCount() - 1 == mPager.getCurrentItem()) {
                // last item, go to first
                mPager.setCurrentItem(0, true);
                return false;
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT && 0 == mPager.getCurrentItem()) {
                // last item, go to first
                mPager.setCurrentItem(mPager.getAdapter().getCount() - 1, true);
                return false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void loadMediaSliderView(final MediaSliderConfiguration config, final List<SliderItem> items) {
        this.config = config;
        this.items = items;
        Player.Listener listener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED && slideShowPlaying) {
                    goToNextAsset();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                ImageButton playPauseButton = findViewById(R.id.exo_pause);
                if (playPauseButton != null) {
                    playPauseButton.setImageResource(isPlaying ? R.drawable.exo_legacy_controls_pause : R.drawable.exo_legacy_controls_play);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (slideShowPlaying) {
                    goToNextAsset();
                }
            }
        };
        initViewsAndSetAdapter(listener);
    }

    private void setStartPosition() {
        if (config.getStartPosition() >= 0) {
            if (config.getStartPosition() > items.size()) {
                mPager.setCurrentItem((items.size() - 1));
                return;
            }
            mPager.setCurrentItem(config.getStartPosition());
        } else {
            mPager.setCurrentItem(0);
        }
        mPager.setOffscreenPageLimit(1);
    }

    public void toggleSlideshow(boolean togglePlayButton) {
        slideShowPlaying = !slideShowPlaying;
        if (slideShowPlaying) {
            // do not start timers for videos, they will continue in the player listener
            if (this.items.get(this.mPager.getCurrentItem()).getType() == SliderItemType.IMAGE) {
                startTimerNextAsset();
            }
            if (getContext() instanceof Activity) {
                // view is being triggered from main app, prevent app going to sleep
                ((Activity) getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } else {
            clearKeepScreenOnFlags();
            mainHandler.removeCallbacks(goToNextAssetRunnable);
        }
        if (togglePlayButton) {
            togglePlayButton();
        }
    }

    private void togglePlayButton() {
        playButton.setVisibility(View.VISIBLE);
        playButton.setBackgroundResource(slideShowPlaying ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
        mainHandler.postDelayed((Runnable) () -> {
            playButton.setVisibility(View.GONE);
        }, 2000);
    }

    private void startTimerNextAsset() {
        mainHandler.postDelayed(goToNextAssetRunnable, this.config.getInterval() * 1000);
    }

    private void goToNextAsset() {
        if (mPager.getCurrentItem() < mPager.getAdapter().getCount() - 1) {
            mPager.setCurrentItem(mPager.getCurrentItem() + 1, this.config.slideItemIntoView());
        } else {
            mPager.setCurrentItem(0, this.config.slideItemIntoView());
        }
    }

    private void initViewsAndSetAdapter(Player.Listener listener) {
        RelativeLayout statusLayout = findViewById(R.id.status_holder);
        if (config.isGradiantOverlayVisible()) {
            statusLayout.setBackgroundResource(R.drawable.gradient_overlay);
        }
        TextView slider_clock = findViewById(R.id.clock);
        TextView slider_title = findViewById(R.id.title);
        TextView slider_subtitle = findViewById(R.id.subtitle);
        TextView slider_date = findViewById(R.id.date);
        slider_media_number = findViewById(R.id.number);
        ImageView left = findViewById(R.id.left_arrow);
        ImageView right = findViewById(R.id.right_arrow);
        mPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(
                getContext(), items,
                defaultExoFactory,
                config.isOnlyUseThumbnails(),
                config.isVideoSoundEnable());
        mPager.setAdapter(pagerAdapter);
        setStartPosition();
        String hexRegex = "/^#(?:(?:[\\da-f]{3}){1,2}|(?:[\\da-f]{4}){1,2})$/i";
        if (config.isClockVisible()) {
            slider_clock.setVisibility(View.VISIBLE);
        }
        if (config.isTitleVisible()) {
            slider_title.setVisibility(View.VISIBLE);
            if (config.getTitle() != null) {
                slider_title.setText(config.getTitle());
            } else {
                slider_title.setText("");
            }
            if (config.getTitleTextColor() != null && config.getTitleTextColor().matches(hexRegex)) {
                slider_title.setTextColor(Color.parseColor(config.getTitleTextColor()));
            }
        }
        if (config.isSubtitleVisible()) {
            slider_subtitle.setVisibility(View.VISIBLE);
            slider_subtitle.setText("");
        }
        if (config.isDateVisible()) {
            slider_date.setVisibility(View.VISIBLE);
            slider_date.setText("");
        }
        if (config.isMediaCountVisible()) {
            slider_media_number.setVisibility(View.VISIBLE);
            slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + items.size());
        }
        if (config.isNavigationVisible()) {
            left.setVisibility(View.VISIBLE);
            right.setVisibility(View.VISIBLE);
            left.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = mPager.getCurrentItem();
                    mPager.setCurrentItem(i - 1);
                    slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + items.size());
                }
            });
            right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = mPager.getCurrentItem();
                    mPager.setCurrentItem(i + 1);
                    slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + items.size());
                }
            });
        }

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
                stopPlayer();
                if (i != mPager.getCurrentItem()) {
                    return;
                }
                SliderItem sliderItem = items.get(i);
                slider_title.setText(sliderItem.getDescription());
                slider_subtitle.setText(sliderItem.getSubtitle());
                Date date = sliderItem.getDate();
                if (date != null) {
                    slider_date.setText(formatDate(date));
                } else {
                    slider_date.setText("");
                }

                slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + items.size());
                if (sliderItem.getType() == SliderItemType.VIDEO) {
                    View viewTag = mPager.findViewWithTag("view" + i);
                    if (viewTag == null) {
                        return;
                    }
                    PlayerView simpleExoPlayerView = viewTag.findViewById(R.id.video_view);
                    if (simpleExoPlayerView.getPlayer() != null) {
                        currentPlayerInScope = (ExoPlayer) simpleExoPlayerView.getPlayer();
                        currentPlayerInScope.seekTo(0, 0);
                        if (currentPlayerInScope.getPlaybackState() == STATE_IDLE) {
                            prepareMedia(sliderItem.getUrl(), currentPlayerInScope, defaultExoFactory);
                        }
                        currentPlayerInScope.addListener(listener);
                        currentPlayerInScope.setPlayWhenReady(true);
                    }
                }
            }

            @Override
            public void onPageSelected(int i) {
                SliderItem sliderItem = items.get(i);
                if (sliderItem.getType() == SliderItemType.IMAGE) {
                    if (slideShowPlaying) {
                        startTimerNextAsset();
                    }
                    stopPlayer();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    public void onDestroy() {
        if (currentPlayerInScope != null) {
            currentPlayerInScope.release();
        }
        clearKeepScreenOnFlags();
        if (mainHandler != null) {
            mainHandler.removeCallbacks(goToNextAssetRunnable);
        }
    }

    private void clearKeepScreenOnFlags() {
        if (getContext() instanceof Activity) {
            // view is being triggered from main app, remove the flags to keep screen on
            Window window = ((Activity) getContext()).getWindow();
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private void stopPlayer() {
        if (currentPlayerInScope != null && (currentPlayerInScope.isPlaying() || currentPlayerInScope.isLoading())) {
            this.currentPlayerInScope.stop();
        }
    }

    public void setDefaultExoFactory(DefaultHttpDataSource.Factory defaultExoFactory) {
        this.defaultExoFactory = defaultExoFactory;
    }

    @SuppressLint("UnsafeOptInUsageError")
    private static void prepareMedia(String mediaUrl, ExoPlayer player, DefaultHttpDataSource.Factory factory) {
        Uri mediaUri = Uri.parse(mediaUrl);
        MediaItem mediaItem = MediaItem.fromUri(mediaUri);
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem);
        player.setMediaSource(mediaSource, 0L);
        player.prepare();
    }

    public void setItems(@NotNull List<SliderItem> items) {
        if (slideShowPlaying) {
            // to prevent timing issues when adding + sliding at the same time
            mainHandler.removeCallbacks(goToNextAssetRunnable);
        }
        this.items = items;
        pagerAdapter.setItems(items);
        if (slideShowPlaying && this.items.get(mPager.getCurrentItem()).getType() == SliderItemType.IMAGE) {
            startTimerNextAsset();
        }
    }

    private static class ScreenSlidePagerAdapter extends PagerAdapter {
        private final DefaultHttpDataSource.Factory exoFactory;
        private final boolean onlyUseThumbnails;
        private Context context;
        private List<SliderItem> items;
        private TouchImageView imageView;
        private Map<Integer, ProgressBar> progressBars;
        private float currentVolume = 0;
        private final boolean isVideoSoundEnable;

        private ScreenSlidePagerAdapter(Context context,
                                        List<SliderItem> items,
                                        DefaultHttpDataSource.Factory defaultExoFactory,
                                        boolean onlyUseThumbnails,
                                        boolean isVideoSoundEnable) {
            this.context = context;
            this.items = items;
            this.progressBars = new HashMap<>();
            this.exoFactory = defaultExoFactory;
            this.onlyUseThumbnails = onlyUseThumbnails;
            this.isVideoSoundEnable = isVideoSoundEnable;
        }

        public void setItems(List<SliderItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        private void hideProgressBar(int position) {
            if (progressBars.containsKey(position)) {
                progressBars.get(position).setVisibility(View.GONE);
                progressBars.remove(position);
            }
        }

        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @NonNull
        @Override
        @SuppressLint("UnsafeOptInUsageError")
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            View view = null;
            SliderItem model = items.get(position);
            if (model.getType() == SliderItemType.IMAGE) {
                view = inflater.inflate(R.layout.image_item, container, false);
                imageView = view.findViewById(R.id.mBigImage);
                ProgressBar progressBar = view.findViewById(R.id.mProgressBar);
                progressBars.put(position, progressBar);
                RequestBuilder<Drawable> glideLoader = Glide.with(context)
                        .load(onlyUseThumbnails ? model.getThumbnailUrl() : model.getUrl())
                        .centerInside()
//                        .placeholder(context.getResources().getDrawable(R.drawable.images))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                Timber.e(e, "Could not fetch image: %s", model);
                                hideProgressBar(position);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                hideProgressBar(position);
                                return false;
                            }
                        });
                if (!onlyUseThumbnails) {
                    glideLoader = glideLoader.thumbnail(Glide.with(context)
                            .load(model.getThumbnailUrl()));
                }
                glideLoader.into(imageView);
            } else if (model.getType() == SliderItemType.VIDEO) {
                view = inflater.inflate(R.layout.video_item, container, false);
                PlayerView playerView = view.findViewById(R.id.video_view);
                playerView.setTag("view" + position);
                ExoPlayer player = new ExoPlayer.Builder(context)
                        .setLoadControl(new DefaultLoadControl.Builder()
                                .setPrioritizeTimeOverSizeThresholds(false)
                                .build()
                        ).build();
                prepareMedia(model.getUrl(), player, exoFactory);
                if (!isVideoSoundEnable) {
                    currentVolume = player.getVolume();
                    player.setVolume(0f);
                }
                player.setPlayWhenReady(false);
                playerView.setPlayer(player);
                ImageButton playBtn = playerView.findViewById(R.id.exo_pause);
                playBtn.setOnClickListener(v -> {
                    //events on play buttons
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        if (player.getCurrentPosition() >= player.getContentDuration()) {
                            player.seekToDefaultPosition();
                        }
                        player.play();
                    }
                });
            }
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return (view == o);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View view = (View) object;
            PlayerView exoplayer = view.findViewById(R.id.video_view);
            if (exoplayer != null && exoplayer.getPlayer() != null) {
                if (!isVideoSoundEnable) {
                    exoplayer.getPlayer().setVolume(currentVolume);
                    currentVolume = 0;
                }
                exoplayer.getPlayer().release();
            } else {
                View imageView = view.findViewById(R.id.mBigImage);
                if (imageView != null) {
                    Glide.with(context).clear(imageView);
                }
            }
            container.removeView(view);
        }
    }

    private static String formatDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        int day = calendar.get(Calendar.DATE);
        String formatString;
        switch (day) {
            case 1:
            case 21:
            case 31:
                formatString = "EEEE',' d'ˢᵗ' MMMM yyyy";
                break;
            case 2:
            case 22:
                formatString = "EEEE',' d'ⁿᵈ' MMMM yyyy";
                break;
            case 3:
            case 23:
                formatString = "EEEE',' d'ʳᵈ' MMMM yyyy";
                break;
            default:
                formatString = "EEEE',' d'ᵗʰ' MMMM yyyy";
                break;
        }
        return new SimpleDateFormat(formatString, locale).format(date);
    }
}
