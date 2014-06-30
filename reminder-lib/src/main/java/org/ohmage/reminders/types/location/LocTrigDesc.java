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
package org.ohmage.reminders.types.location;

import android.content.Context;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.reminders.base.TrigDesc;
import org.ohmage.reminders.config.LocTrigConfig;
import org.ohmage.reminders.utils.SimpleTime;

import java.lang.reflect.Type;

/*
 * The class which can parse and store the JSON string of location 
 * trigger description. 
 * 
 * Example of a location trigger description:
 * 
 * {
 *         "location": "Home",
 *         "min_reentry_interval": 30,
 *         "time_range": {
 *             "start": "11:00",
 *             "end": "13:00",
 *             "trigger_always": true
 *         }
 * }
 */
public class LocTrigDesc implements TrigDesc {

    private static final String KEY_LOCATION = "location";
    private static final String KEY_TIME_RANGE = "time_range";
    private static final String KEY_START = "start";
    private static final String KEY_END = "end";
    private static final String KEY_TRIGGER_ALWAYS = "trigger_always";
    private static final String KEY_MIN_INTERVAL_REENTRY = "min_reentry_interval";

    private String mLocation;
    private SimpleTime mStartTime = new SimpleTime();
    private SimpleTime mEndTime = new SimpleTime();
    private boolean mRangeEnabled = false;
    private boolean mTriggerAlways = false;
    private int mMinInterval = LocTrigConfig.MIN_REENTRY;

    private void initialize() {
        mLocation = null;
        mTriggerAlways = false;
        mRangeEnabled = false;
        mMinInterval = 0;
    }

    /*
     * Parse a location trigger description
     * and load the parameters into this object.
     */
    public boolean loadString(String desc) {

        initialize();

        if (desc == null) {
            return false;
        }

        try {
            JSONObject jDesc = new JSONObject(desc);

            mLocation = jDesc.getString(KEY_LOCATION);
            mMinInterval = jDesc.getInt(KEY_MIN_INTERVAL_REENTRY);

            if (jDesc.has(KEY_TIME_RANGE)) {

                JSONObject jRange = jDesc.getJSONObject(KEY_TIME_RANGE);

                if (!mStartTime.loadString(jRange.getString(KEY_START))) {
                    return false;
                }

                if (!mEndTime.loadString(jRange.getString(KEY_END))) {
                    return false;
                }

                mTriggerAlways = jRange.getBoolean(KEY_TRIGGER_ALWAYS);

                mRangeEnabled = true;
            }

        } catch (JSONException e) {
            return false;
        }

        return true;
    }

    /*
     * Get the location of the trigger
     */
    public String getLocation() {
        return mLocation;
    }

    /*
     * Set the location of the trigger
     */
    public void setLocation(String loc) {
        mLocation = loc;
    }

    /*
     * Get the minimum re-entry interval for this
     * location trigger
     */
    public int getMinReentryInterval() {
        return mMinInterval;
    }

    /*
     * Get the global setting for minimum re-entry interval
     */
    public static int getGlobalMinReentryInterval(Context context) {
        return LocTrigConfig.MIN_REENTRY;
    }

    /*
     * Set the minimum re-entry interval for this trigger
     */
    public void setMinReentryInterval(int interval) {
        mMinInterval = interval;
    }

    /*
     * Check if there is a time range enabled for this trigger
     */
    public boolean isRangeEnabled() {
        return mRangeEnabled;
    }

    /*
     * Set whether there is a time range
     */
    public void setRangeEnabled(boolean enable) {
        mRangeEnabled = enable;
    }

    /*
     * Get the start time if a time range is enabled
     */
    public SimpleTime getStartTime() {
        return new SimpleTime(mStartTime);
    }

    /*
     * Set the start time.
     */
    public void setStartTime(SimpleTime time) {
        mStartTime.copy(time);
    }

    /*
     * Get the end time if a time range is enabled
     */
    public SimpleTime getEndTime() {
        return new SimpleTime(mEndTime);
    }

    /*
     * Set the end time
     */
    public void setEndTime(SimpleTime time) {
        mEndTime.copy(time);
    }

    /*
     * Check if this trigger is set to go off
     * at the end of time range even if the
     * location is not reached.
     */
    public boolean shouldTriggerAlways() {
        return mTriggerAlways;
    }

    /*
     * Set whether this trigger must go off
     * at the end of time range even if the
     * location is not reached.
     */
    public void setTriggerAlways(boolean enable) {
        mTriggerAlways = enable;
    }

    /*
     * Convert the trigger description represented by
     * this object to a JSON string.
     */
    public String toString() {
        JSONObject jDesc = new JSONObject();

        try {
            jDesc.put(KEY_LOCATION, mLocation);
            jDesc.put(KEY_MIN_INTERVAL_REENTRY, mMinInterval);

            if (mRangeEnabled) {
                JSONObject jRange = new JSONObject();

                jRange.put(KEY_START, mStartTime.toString(false));
                jRange.put(KEY_END, mEndTime.toString(false));
                jRange.put(KEY_TRIGGER_ALWAYS, mTriggerAlways);

                jDesc.put(KEY_TIME_RANGE, jRange);
            }
        } catch (JSONException e) {
            return null;
        }

        return jDesc.toString();
    }

    /*
     * Validate the trigger description associated
     * with this object.
     */
    public boolean validate() {

        if (mLocation == null || mLocation.length() == 0) {
            return false;
        }

        if (mTriggerAlways && !mRangeEnabled) {
            return false;
        }

        if (mRangeEnabled && !mEndTime.isAfter(mStartTime)) {
            return false;
        }

        return true;
    }

    public static class LocTrigDescDeserializer implements JsonDeserializer<LocTrigDesc> {

        @Override
        public LocTrigDesc deserialize(JsonElement json, Type type,
                                       JsonDeserializationContext context) throws JsonParseException {

            JsonObject jDesc = (JsonObject) json;
            LocTrigDesc desc = new LocTrigDesc();

            desc.initialize();

            desc.mLocation = jDesc.get(KEY_LOCATION).getAsString();
            desc.mMinInterval = jDesc.get(KEY_MIN_INTERVAL_REENTRY).getAsInt();

            if (jDesc.has(KEY_TIME_RANGE)) {

                JsonObject jRange = jDesc.get(KEY_TIME_RANGE).getAsJsonObject();

                if (!desc.mStartTime.loadString(jRange.get(KEY_START).getAsString())) {
                    throw new JsonParseException("Start time must be included");
                }

                if (!desc.mEndTime.loadString(jRange.get(KEY_END).getAsString())) {
                    throw new JsonParseException("End time must be included");
                }

                desc.mTriggerAlways = jRange.get(KEY_TRIGGER_ALWAYS).getAsBoolean();

                desc.mRangeEnabled = true;
            }

            return desc;
        }
    }
}
