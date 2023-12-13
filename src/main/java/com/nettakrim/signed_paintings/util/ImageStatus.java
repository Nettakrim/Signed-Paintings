package com.nettakrim.signed_paintings.util;

import org.joml.Vector2i;

import java.util.ArrayList;

public class ImageStatus implements Comparable<ImageStatus> {
    public ArrayList<ResolutionStatus> resolutionStatuses;

    public String url;

    public boolean ready;

    public ImageStatus() {
        resolutionStatuses = new ArrayList<>();
    }

    public ImageStatus setUrl(String url) {
        this.url = url;
        return this;
    }

    public void addResolution(Vector2i pixels, long bytes, boolean isScaled) {
        resolutionStatuses.add(new ResolutionStatus(pixels, bytes, isScaled));
    }

    public long getTotalSize() {
        long bytes = 0;
        for (ResolutionStatus resolutionStatus : resolutionStatuses) {
            bytes += resolutionStatus.bytes;
        }
        return bytes;
    }

    @Override
    public int compareTo(ImageStatus other) {
        return Long.compare(getTotalSize(), other.getTotalSize());
    }

    public int getResolutionsCount() {
        return resolutionStatuses.size();
    }

    public record ResolutionStatus (Vector2i pixels, long bytes, boolean isScaled) implements Comparable<ResolutionStatus> {
        @Override
        public int compareTo(ResolutionStatus other) {
            return Long.compare(bytes, other.bytes);
        }
    }
}
