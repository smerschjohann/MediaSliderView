package com.zeuskartik.mediaslider;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MediaSliderActivity extends AppCompatActivity {
    private ViewPager mPager;
    private TextView slider_media_number;
    private long playbackPosition = 0;
    private int currentWindow = 0;
    private boolean isTitleVisible, isMediaCountVisible, isNavigationVisible;
    private String title;
    private ArrayList<SliderItem> urlList;
    private String titleTextColor;
    private String titleBackgroundColor;
    private int startPosition = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slider);
    }

    public void loadMediaSliderView(ArrayList<SliderItem> mediaUrlList, boolean isTitleVisible, boolean isMediaCountVisible, boolean isNavigationVisible, String title, String titleBackgroundColor, String titleTextColor, int startPosition) {
        this.urlList = mediaUrlList;
        this.isTitleVisible = isTitleVisible;
        this.isMediaCountVisible = isMediaCountVisible;
        this.isNavigationVisible = isNavigationVisible;
        this.title = title;
        this.titleBackgroundColor = titleBackgroundColor;
        this.titleTextColor = titleTextColor;
        this.startPosition = startPosition;
        initViewsAndSetAdapter();
    }

    private void setStartPosition() {
        if (startPosition >= 0) {
            if (startPosition > urlList.size()) {
                mPager.setCurrentItem((urlList.size() - 1));
                return;
            }
            mPager.setCurrentItem(startPosition);
        } else {
            mPager.setCurrentItem(0);
        }
        mPager.setOffscreenPageLimit(1);
    }

    private void initViewsAndSetAdapter() {
        RelativeLayout statusLayout = findViewById(R.id.status_holder);
        TextView slider_title = findViewById(R.id.title);
        slider_media_number = findViewById(R.id.number);
        ImageView left = findViewById(R.id.left_arrow);
        ImageView right = findViewById(R.id.right_arrow);
        mPager = findViewById(R.id.pager);
        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(MediaSliderActivity.this, urlList);
        mPager.setAdapter(pagerAdapter);
        setStartPosition();
        String hexRegex = "/^#(?:(?:[\\da-f]{3}){1,2}|(?:[\\da-f]{4}){1,2})$/i";
        if (isTitleVisible || isMediaCountVisible) {
            if (titleBackgroundColor != null && titleBackgroundColor.matches(hexRegex)) {
                statusLayout.setBackgroundColor(Color.parseColor(titleBackgroundColor));
            } else {
                statusLayout.setBackgroundColor(getResources().getColor(R.color.transparent));
            }
        }
        if (isTitleVisible) {
            slider_title.setVisibility(View.VISIBLE);
            if (title != null) {
                slider_title.setText(title);
            } else {
                slider_title.setText("");
            }
            if (titleTextColor != null && titleTextColor.matches(hexRegex)) {
                slider_title.setTextColor(Color.parseColor(titleTextColor));
            }
        }
        if (isMediaCountVisible) {
            slider_media_number.setVisibility(View.VISIBLE);
            slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + urlList.size());
        }
        if (isNavigationVisible) {
            left.setVisibility(View.VISIBLE);
            right.setVisibility(View.VISIBLE);
            left.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = mPager.getCurrentItem();
                    mPager.setCurrentItem(i - 1);
                    slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + urlList.size());


                }
            });
            right.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int i = mPager.getCurrentItem();
                    mPager.setCurrentItem(i + 1);
                    slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + urlList.size());

                }
            });
        }

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
                SliderItem sliderItem = urlList.get(i);
                slider_title.setText(sliderItem.getDescription());
                slider_media_number.setText((mPager.getCurrentItem() + 1) + "/" + urlList.size());
                if (sliderItem.getType().equalsIgnoreCase("video")) {
                    View viewTag = mPager.findViewWithTag("view" + i);
                    PlayerView simpleExoPlayerView = viewTag.findViewById(R.id.video_view);
                    if (simpleExoPlayerView.getPlayer() != null) {
                        ExoPlayer player = (ExoPlayer) simpleExoPlayerView.getPlayer();
                        playbackPosition = player.getCurrentPosition();
                        currentWindow = player.getCurrentWindowIndex();
                        player.setPlayWhenReady(true);
                    }
                }
            }

            @Override
            public void onPageSelected(int i) {
                SliderItem sliderItem = urlList.get(i);
                if (sliderItem.getType().equalsIgnoreCase("video")) {
                    View viewTag = mPager.findViewWithTag("view" + i);
                    PlayerView simpleExoPlayerView = viewTag.findViewById(R.id.video_view);
                    if (simpleExoPlayerView.getPlayer() != null) {
                        ExoPlayer player = (ExoPlayer) simpleExoPlayerView.getPlayer();
                        Uri mediaUri = Uri.parse(sliderItem.getUrl());

                        MediaItem mediaItem =
                                MediaItem.fromUri(mediaUri);
                        DefaultHttpDataSource.Factory defaultHttpDataSourceFactory = new DefaultHttpDataSource.Factory();
                        MediaSource mediaSource =
                                new ProgressiveMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(mediaItem);
                        playbackPosition = player.getCurrentPosition();
                        currentWindow = player.getCurrentWindowIndex();
                        player.setMediaSource(mediaSource);
                        player.prepare();
                        player.setPlayWhenReady(true);
                        player.seekTo(0, 0);

                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    private static class ScreenSlidePagerAdapter extends PagerAdapter {
        private Context context;
        private ArrayList<SliderItem> urlList;
        ExoPlayer player;
        PlayerView simpleExoPlayerView;
        MediaSource mediaSource;
        TouchImageView imageView;
        Map<Integer, ProgressBar> progressBars;


        private ScreenSlidePagerAdapter(Context context, ArrayList<SliderItem> urlList) {
            this.context = context;
            this.urlList = urlList;
            progressBars = new HashMap<>();

        }

        private void hideProgressBar(int position){
            if (progressBars.containsKey(position)){
                progressBars.get(position).setVisibility(View.GONE);
                progressBars.remove(position);
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            View view = null;
            SliderItem model = urlList.get(position);
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
                simpleExoPlayerView = view.findViewById(R.id.video_view);
                simpleExoPlayerView.setTag("view" + position);
                player = new ExoPlayer.Builder(context).build();
                Uri mediaUri = Uri.parse(model.getUrl());
                        MediaItem mediaItem =
                        MediaItem.fromUri(mediaUri);
                DefaultHttpDataSource.Factory defaultHttpDataSourceFactory = new DefaultHttpDataSource.Factory();
                mediaSource =
                        new ProgressiveMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(mediaItem);
                simpleExoPlayerView.setPlayer(player);
                player.prepare(mediaSource, true, true);
                player.setPlayWhenReady(true);
                player.seekTo(0, 0);
            }
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return urlList.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return (view == o);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }


    }
}
