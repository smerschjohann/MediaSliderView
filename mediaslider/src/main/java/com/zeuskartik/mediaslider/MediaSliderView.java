package com.zeuskartik.mediaslider;

import static com.google.android.exoplayer2.Player.STATE_IDLE;

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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaSliderView extends ConstraintLayout {
    private View playButton;
    private Handler mainHandler;
    private ViewPager mPager;
    private TextView slider_media_number;
    private ExoPlayer currentPlayerInScope;
    private DefaultHttpDataSource.Factory defaultExoFactory = new DefaultHttpDataSource.Factory();
    private boolean slideShowPlaying;
    private Runnable goToNextAssetRunnable = this::goToNextAsset;
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
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && items.get(mPager.getCurrentItem()).getType().equals("image")) {
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
            public void onPlayerError(PlaybackException error) {
                if(slideShowPlaying){
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
            if(this.items.get(this.mPager.getCurrentItem()).getType().equals("image")){
                startTimerNextAsset();
            }
        } else {
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
            mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
        } else {
            mPager.setCurrentItem(0, true);
        }
    }

    private void initViewsAndSetAdapter(Player.Listener listener) {
        RelativeLayout statusLayout = findViewById(R.id.status_holder);
        TextView slider_title = findViewById(R.id.title);
        slider_media_number = findViewById(R.id.number);
        ImageView left = findViewById(R.id.left_arrow);
        ImageView right = findViewById(R.id.right_arrow);
        mPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getContext(), items, defaultExoFactory);
        mPager.setAdapter(pagerAdapter);
        setStartPosition();
        String hexRegex = "/^#(?:(?:[\\da-f]{3}){1,2}|(?:[\\da-f]{4}){1,2})$/i";
        if (config.isTitleVisible() || config.isMediaCountVisible()) {
            if (config.getTitleBackgroundColor() != null && config.getTitleBackgroundColor().matches(hexRegex)) {
                statusLayout.setBackgroundColor(Color.parseColor(config.getTitleBackgroundColor()));
            } else {
                statusLayout.setBackgroundColor(getResources().getColor(R.color.transparent));
            }
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
                slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + items.size());
                if (sliderItem.getType().equalsIgnoreCase("video")) {
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
                if (sliderItem.getType().equalsIgnoreCase("image")) {
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
        stopPlayer();
        if (mainHandler != null) {
            mainHandler.removeCallbacks(goToNextAssetRunnable);
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
    private static void prepareMedia(String mediaUrl, ExoPlayer player, DefaultHttpDataSource.Factory defaultExoFactory) {
        Uri mediaUri = Uri.parse(mediaUrl);
        MediaItem mediaItem = MediaItem.fromUri(mediaUri);
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(defaultExoFactory).createMediaSource(mediaItem);
        player.prepare(mediaSource, true, true);
    }
    public void setItems(@NotNull List<SliderItem> items) {
        if(slideShowPlaying){
            // to prevent timing issues when adding + sliding at the same time
            mainHandler.removeCallbacks(goToNextAssetRunnable);
        }
        this.items = items;
        pagerAdapter.setItems(items);
        if(slideShowPlaying && this.items.get(mPager.getCurrentItem()).getType().equals("image")){
            startTimerNextAsset();
        }
    }
    private static class ScreenSlidePagerAdapter extends PagerAdapter {
        private final DefaultHttpDataSource.Factory exoFactory;
        private Context context;
        private List<SliderItem> items;
        private TouchImageView imageView;
        private Map<Integer, ProgressBar> progressBars;

        private ScreenSlidePagerAdapter(Context context,
                                        List<SliderItem> items,
                                        DefaultHttpDataSource.Factory defaultExoFactory) {
            this.context = context;
            this.items = items;
            this.progressBars = new HashMap<>();
            this.exoFactory = defaultExoFactory;
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
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            View view = null;
            SliderItem model = items.get(position);
            if (model.getType().equalsIgnoreCase("image")) {
                view = inflater.inflate(R.layout.image_item, container, false);
                imageView = view.findViewById(R.id.mBigImage);
                ProgressBar progressBar = view.findViewById(R.id.mProgressBar);
                progressBars.put(position, progressBar);
                Glide.with(context).load(model.getUrl()).centerInside().placeholder(context.getResources().getDrawable(R.drawable.images)).listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        hideProgressBar(position);
                        imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.images));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        hideProgressBar(position);
                        return false;
                    }
                }).into(imageView);
            } else if (model.getType().equalsIgnoreCase("video")) {
                view = inflater.inflate(R.layout.video_item, container, false);
                PlayerView playerView = view.findViewById(R.id.video_view);
                playerView.setTag("view" + position);
                ExoPlayer player = new ExoPlayer.Builder(context).build();
                prepareMedia(model.getUrl(), player, exoFactory);
                player.setPlayWhenReady(false);
                playerView.setPlayer(player);
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
            PlayerView exoplayer = (PlayerView) view.findViewById(R.id.video_view);
            if (exoplayer != null && exoplayer.getPlayer() != null) {
                exoplayer.getPlayer().release();
            }
            container.removeView(view);
        }


    }
}
