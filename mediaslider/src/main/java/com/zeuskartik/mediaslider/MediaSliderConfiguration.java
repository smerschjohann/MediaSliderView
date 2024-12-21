package com.zeuskartik.mediaslider;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.EnumSet;

public class MediaSliderConfiguration implements Parcelable {
    private final EnumSet<DisplayOptions> displayOptions;
    private final int startPosition;
    private final int interval;
    private final boolean onlyUseThumbnails;
    private final boolean isVideoSoundEnable;
    private final LoadMore loadMore;

    public MediaSliderConfiguration(EnumSet<DisplayOptions> displayOptions,
                                    int startPosition,
                                    int interval,
                                    boolean onlyUseThumbnails,
                                    boolean isVideoSoundEnable,
                                    @Nullable LoadMore loadMore) {
        this.displayOptions = displayOptions;
        this.startPosition = startPosition;
        this.interval = interval;
        this.onlyUseThumbnails = onlyUseThumbnails;
        this.isVideoSoundEnable = isVideoSoundEnable;
        this.loadMore = loadMore;
    }

    protected MediaSliderConfiguration(Parcel in) {
        displayOptions = (EnumSet<DisplayOptions>) in.readSerializable();
        startPosition = in.readInt();
        interval = in.readInt();
        onlyUseThumbnails = in.readByte() != 0;
        isVideoSoundEnable = in.readByte() != 0;
        loadMore = (LoadMore) in.readSerializable();
    }

    public static final Creator<MediaSliderConfiguration> CREATOR = new Creator<MediaSliderConfiguration>() {
        @Override
        public MediaSliderConfiguration createFromParcel(Parcel in) {
            return new MediaSliderConfiguration(in);
        }

        @Override
        public MediaSliderConfiguration[] newArray(int size) {
            return new MediaSliderConfiguration[size];
        }
    };

    public boolean isOnlyUseThumbnails() {
        return onlyUseThumbnails;
    }

    public boolean isVideoSoundEnable() {
        return isVideoSoundEnable;
    }

    public boolean isClockVisible() {
        return displayOptions.contains(DisplayOptions.CLOCK);
    }

    public boolean isTitleVisible() {
        return displayOptions.contains(DisplayOptions.TITLE);
    }

    public boolean isSubtitleVisible() {
        return displayOptions.contains(DisplayOptions.SUBTITLE);
    }

    public boolean isDateVisible() {
        return displayOptions.contains(DisplayOptions.DATE);
    }

    public boolean isMediaCountVisible() {
        return displayOptions.contains(DisplayOptions.MEDIA_COUNT);
    }

    public boolean isGradiantOverlayVisible() {
        return (isMediaCountVisible() || isDateVisible() || isClockVisible() || isTitleVisible() || isSubtitleVisible()) && displayOptions.contains(DisplayOptions.GRADIENT_OVERLAY);
    }

    public boolean isNavigationVisible() {
        return displayOptions.contains(DisplayOptions.NAVIGATION);
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getInterval() {
        return interval;
    }

    public boolean slideItemIntoView() {
        return displayOptions.contains(DisplayOptions.ANIMATE_ASST_SLIDE);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeSerializable(displayOptions);
        dest.writeInt(startPosition);
        dest.writeInt(interval);
        dest.writeByte((byte) (onlyUseThumbnails ? 1 : 0));
        dest.writeByte((byte) (isVideoSoundEnable ? 1 : 0));
        dest.writeSerializable(loadMore);
    }

    @Nullable
    public LoadMore getLoadMore() {
        return loadMore;
    }
}
