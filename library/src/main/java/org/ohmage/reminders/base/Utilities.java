/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.ohmage.reminders.base;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utilities {

    public static void delete(File f) {
        if (f == null)
            return;
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        f.delete();
    }

    /**
     * utility method for converting dp to pixels, since the setters only take
     * pixel values :\
     * 
     * @param dp value
     * @return
     */
    public static int dpToPixels(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static Location getCurrentLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if(locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
            return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        else {
            Location gps = null;
            Location network = null;
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if(gps != null && network != null)
                return (gps.getTime() <= network.getTime()) ? gps : network;
            else if(gps == null)
                return network;
            else
                return gps;
        }
    }

    public static int setAlpha(int color, int alpha) {
        return color + (alpha << 24);
    }
}
