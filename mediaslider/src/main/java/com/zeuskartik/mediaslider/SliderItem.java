package com.zeuskartik.mediaslider;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

public class SliderItem implements Parcelable {
    private final String url;
    private final String type;
    private final String description;

    public SliderItem(String url, String type, String description) {
        this.url = url;
        this.type = type;
        this.description = description;
    }

    protected SliderItem(Parcel in) {
        url = in.readString();
        type = in.readString();
        description = in.readString();
    }

    public static final Creator<SliderItem> CREATOR = new Creator<SliderItem>() {
        @Override
        public SliderItem createFromParcel(Parcel in) {
            return new SliderItem(in);
        }

        @Override
        public SliderItem[] newArray(int size) {
            return new SliderItem[size];
        }
    };

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(type);
        dest.writeString(description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SliderItem that = (SliderItem) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
