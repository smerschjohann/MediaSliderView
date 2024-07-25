package com.zeuskartik.mediaslider;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class MediaSliderConfiguration implements Parcelable {
    private final boolean isTitleVisible;
    private final boolean isMediaCountVisible;
    private final boolean isNavigationVisible;
    private final String title;
    private final String titleBackgroundColor;
    private final String titleTextColor;
    private final int startPosition;
    private final int interval;
    private final boolean onlyUseThumbnails;
    private final boolean isVideoSoundEnable;

    public MediaSliderConfiguration(boolean isTitleVisible,
                                    boolean isMediaCountVisible,
                                    boolean isNavigationVisible,
                                    String title,
                                    String titleBackgroundColor,
                                    String titleTextColor,
                                    int startPosition,
                                    int interval,
                                    boolean onlyUseThumbnails,
                                    boolean isVideoSoundEnable) {
        this.isTitleVisible = isTitleVisible;
        this.isMediaCountVisible = isMediaCountVisible;
        this.isNavigationVisible = isNavigationVisible;
        this.title = title;
        this.titleBackgroundColor = titleBackgroundColor;
        this.titleTextColor = titleTextColor;
        this.startPosition = startPosition;
        this.interval = interval;
        this.onlyUseThumbnails = onlyUseThumbnails;
        this.isVideoSoundEnable = isVideoSoundEnable;
    }

    protected MediaSliderConfiguration(Parcel in) {
        isTitleVisible = in.readByte() != 0;
        isMediaCountVisible = in.readByte() != 0;
        isNavigationVisible = in.readByte() != 0;
        title = in.readString();
        titleBackgroundColor = in.readString();
        titleTextColor = in.readString();
        startPosition = in.readInt();
        interval = in.readInt();
        onlyUseThumbnails = in.readByte() != 0;
        isVideoSoundEnable = in.readByte() != 0;
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

    public boolean isTitleVisible() {
        return isTitleVisible;
    }

    public boolean isMediaCountVisible() {
        return isMediaCountVisible;
    }

    public boolean isNavigationVisible() {
        return isNavigationVisible;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleBackgroundColor() {
        return titleBackgroundColor;
    }

    public String getTitleTextColor() {
        return titleTextColor;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getInterval() {
        return interval;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByte((byte) (isTitleVisible ? 1 : 0));
        dest.writeByte((byte) (isMediaCountVisible ? 1 : 0));
        dest.writeByte((byte) (isNavigationVisible ? 1 : 0));
        dest.writeString(title);
        dest.writeString(titleBackgroundColor);
        dest.writeString(titleTextColor);
        dest.writeInt(startPosition);
        dest.writeInt(interval);
        dest.writeByte((byte) (onlyUseThumbnails ? 1 : 0));
        dest.writeByte((byte) (isVideoSoundEnable ? 1 : 0));
    }
}
