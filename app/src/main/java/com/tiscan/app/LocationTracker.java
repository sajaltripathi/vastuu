package com.tiscan.app;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import androidx.core.content.ContextCompat;
import java.util.List;

/* JADX INFO: loaded from: classes3.dex */
public class LocationTracker implements LocationListener {
    private static final long GPS_INTERVAL_MS = 750;
    private static final long NETWORK_INTERVAL_MS = 2500;
    private static final long STALE_MS = 120000;
    private Location best;
    private final Context context;
    private final Listener listener;
    private final LocationManager lm;
    private boolean started;

    public interface Listener {
        void onLocation(Location location);

        default void onLocationStatus(String status) {
        }
    }

    public LocationTracker(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.lm = (LocationManager) context.getSystemService("location");
    }

    public synchronized void start() {
        if (this.started) {
            return;
        }
        boolean z = true;
        boolean fine = ContextCompat.checkSelfPermission(this.context, "android.permission.ACCESS_FINE_LOCATION") == 0;
        boolean coarse = ContextCompat.checkSelfPermission(this.context, "android.permission.ACCESS_COARSE_LOCATION") == 0;
        if (!fine && !coarse) {
            this.listener.onLocationStatus("Location permission needed");
            return;
        }
        try {
            this.best = chooseBestLastKnown(fine);
            if (this.best != null) {
                this.listener.onLocation(this.best);
            }
            boolean gps = fine && this.lm.isProviderEnabled("gps");
            boolean network = this.lm.isProviderEnabled("network");
            if (gps) {
                this.lm.requestLocationUpdates("gps", GPS_INTERVAL_MS, 0.0f, this);
            }
            if (network) {
                this.lm.requestLocationUpdates("network", NETWORK_INTERVAL_MS, 0.0f, this);
            }
            if (!gps && !network) {
                z = false;
            }
            this.started = z;
            if (this.started) {
                Listener listener = this.listener;
                if (gps) {
                    listener.onLocationStatus("Acquiring high-accuracy GPS…");
                } else {
                    listener.onLocationStatus("GPS unavailable — using coarse network location");
                }
            } else {
                this.listener.onLocationStatus("Turn on Location/GPS");
            }
        } catch (SecurityException e) {
            this.listener.onLocationStatus("Location permission unavailable");
        } catch (Exception e2) {
            this.listener.onLocationStatus("Location service unavailable");
        }
    }

    private Location chooseBestLastKnown(boolean fine) {
        Location candidate = null;
        try {
            List<String> providers = this.lm.getProviders(true);
            for (String provider : providers) {
                if (fine || !"gps".equals(provider)) {
                    try {
                        Location l = this.lm.getLastKnownLocation(provider);
                        if (l != null && !isStale(l) && (candidate == null || better(l, candidate))) {
                            candidate = l;
                        }
                    } catch (SecurityException e) {
                    }
                }
            }
        } catch (Exception e2) {
        }
        return candidate;
    }

    @Override // android.location.LocationListener
    public synchronized void onLocationChanged(Location location) {
        if (location != null) {
            if (location.getAccuracy() > 0.0f) {
                if (this.best == null || better(location, this.best)) {
                    this.best = location;
                    this.listener.onLocation(location);
                }
            }
        }
    }

    private static boolean better(Location n, Location old) {
        long dt;
        if (n.getElapsedRealtimeNanos() > 0 && old.getElapsedRealtimeNanos() > 0) {
            dt = (n.getElapsedRealtimeNanos() - old.getElapsedRealtimeNanos()) / 1000000;
        } else {
            dt = n.getTime() - old.getTime();
        }
        boolean muchNewer = dt > 20000;
        boolean newer = dt > 0;
        float accGain = old.getAccuracy() - n.getAccuracy();
        if ((!muchNewer || n.getAccuracy() > Math.max(50.0f, old.getAccuracy() * 1.5f)) && accGain < 3.0f) {
            return newer && n.getAccuracy() <= old.getAccuracy() + 1.5f;
        }
        return true;
    }

    private static boolean isStale(Location l) {
        if (l.getElapsedRealtimeNanos() > 0) {
            long ageMs = (SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos()) / 1000000;
            return ageMs > STALE_MS;
        }
        long ageMs2 = System.currentTimeMillis();
        return ageMs2 - l.getTime() > STALE_MS;
    }

    public synchronized void stop() {
        try {
            this.lm.removeUpdates(this);
        } catch (Exception e) {
        }
        this.started = false;
    }

    @Override // android.location.LocationListener
    public void onProviderEnabled(String provider) {
        stop();
        start();
    }

    @Override // android.location.LocationListener
    public void onProviderDisabled(String provider) {
        if ("gps".equals(provider)) {
            this.listener.onLocationStatus("GPS disabled — location may be coarse");
        }
    }
}
