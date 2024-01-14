package com.zeuskartik.mediaslider;

public class SliderItem {
    private final String url;
    private final String type;
    private final String description;

    public SliderItem(String url, String type, String description) {
        this.url = url;
        this.type = type;
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
