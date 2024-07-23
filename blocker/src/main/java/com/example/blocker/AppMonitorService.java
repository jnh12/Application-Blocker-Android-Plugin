package com.example.blocker;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppMonitorService extends Service {
    private Handler handler = new Handler();
    private Runnable runnable;

    private static final long INTERVAL = 5000; //checks every 5 seconds for opened packages
    private String blockedPackage = "com.facebook.katana";
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
            return START_NOT_STICKY;
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        checkForegroundApp();
                    }
                });
                handler.postDelayed(runnable, INTERVAL);
            }
        };
        handler.post(runnable);
        return START_STICKY;
    }

    private boolean hasUsageStatsPermission() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long beginTime = endTime - INTERVAL;
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);
        return usageStatsList != null && !usageStatsList.isEmpty();
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void checkForegroundApp() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long beginTime = endTime - INTERVAL;
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);

        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
            for (UsageStats usageStats : usageStatsList) {
                sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!sortedMap.isEmpty()) {
                String topPackageName = sortedMap.get(sortedMap.lastKey()).getPackageName();
                if (topPackageName.equals(blockedPackage)) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("your.unity.package.name");
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        executorService.shutdown();
    }
}
