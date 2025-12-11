package com.pm.urlshortenerbackend.dto;

import java.util.List;

/**
 * Author: Sathwik Pillalamarri
 * Date: 12/8/25
 * Project: url-shortener-backend
 */
public class DeviceStats {
    private List<CategoryStats> devices;

    public DeviceStats() {}

    public DeviceStats(List<CategoryStats> devices) {
        this.devices = devices;
    }

    public List<CategoryStats> getDevices() {
        return devices;
    }

    public void setDevices(List<CategoryStats> devices) {
        this.devices = devices;
    }
}
