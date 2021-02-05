package com.github.wensimin.rikaisya.utils;

/**
 * 性能计时器
 */
public class PerformanceTimer {
    private long lastTime;
    private final long startTime;

    private static PerformanceTimer performanceTimer;

    private PerformanceTimer(long startTime) {
        this.startTime = startTime;
        this.lastTime = startTime;
    }

    public static void start() {
        performanceTimer = new PerformanceTimer(System.currentTimeMillis());
    }

    public static long cut() {
        if (performanceTimer == null) {
            return 0;
        } else {
            long currentTime = System.currentTimeMillis();
            long cutTime = currentTime - performanceTimer.lastTime;
            performanceTimer.lastTime = currentTime;
            return cutTime;
        }
    }

    public static long end() {
        if (performanceTimer == null) {
            return 0;
        } else {
            long currentTime = System.currentTimeMillis();
            long endTime = currentTime - performanceTimer.startTime;
            performanceTimer = null;
            return endTime;
        }
    }

}
