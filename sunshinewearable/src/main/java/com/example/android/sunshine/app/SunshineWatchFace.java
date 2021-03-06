/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;



public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface TIME_TYPEFACE = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface DATE_TYPEFACE = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final Typeface HIGH_TEMP_TYPEFACE = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface LOW_TEMP_TYPEFACE = Typeface.create("sans-serif-light", Typeface.NORMAL);

    private static final long INTERACTIVE_UPDATE_RATE_MS = 60000;

    private static final int MSG_UPDATE_TIME = 0;

    static Bitmap currentConditionsImage;

    static int highTemp = Integer.MIN_VALUE;
    static int lowTemp = Integer.MIN_VALUE;

    private static Engine engineInstance;

    public static void updateWatchFace() {
        if(engineInstance != null){
            engineInstance.invalidate();
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine {

        final Handler engineUpdateHandler = new EngineHandler(this);

        final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                currentTime = Calendar.getInstance(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
            }
        };
        boolean registeredTimeZoneReceiver = false;

        Paint backgroundPaint;
        Paint timePaint;
        Paint datePaint;
        Paint linePaint;
        Paint highTempPaint;
        Paint lowTempPaint;

        boolean isAmbient;
        Calendar currentTime;

        float timeOffsetY;
        float dateOffsetY;
        float lineOffsetY;
        float currentWeatherOffsetY;

        boolean supportsLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            engineInstance = this;

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = SunshineWatchFace.this.getResources();

            timeOffsetY = resources.getDimension(R.dimen.digital_y_offset);
            dateOffsetY = resources.getDimension(R.dimen.date_y_offset);
            lineOffsetY = resources.getDimension(R.dimen.line_y_offset);
            currentWeatherOffsetY = resources.getDimension(R.dimen.current_weather_y_offset);

            initializeBackgroundPaint();
            initializeTimePaint();
            initializeDatePaint();
            initializeLinePaint();
            initializeHighTempPaint();
            initializeLowTempPaint();

            currentTime = Calendar.getInstance();
            notifyHandheld();
        }

        private void initializeBackgroundPaint(){
            backgroundPaint = new Paint();
            backgroundPaint.setColor(ContextCompat.getColor(SunshineWatchFace.this, R.color.blue_background));
        }

        private void initializeTimePaint(){
            timePaint = new Paint();
            timePaint.setColor(ContextCompat.getColor(SunshineWatchFace.this, R.color.digital_text));
            timePaint.setTypeface(TIME_TYPEFACE);
            timePaint.setAntiAlias(true);
        }

        private void initializeLinePaint(){
            linePaint = new Paint();
            linePaint.setColor(ContextCompat.getColor(SunshineWatchFace.this, R.color.digital_text));
            linePaint.setAlpha(128);
            linePaint.setAntiAlias(true);
        }

        private void initializeDatePaint(){
            datePaint = new Paint();
            datePaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.digital_text));
            datePaint.setTypeface(DATE_TYPEFACE);
            datePaint.setAlpha(180);
            datePaint.setAntiAlias(true);
        }

        private void initializeHighTempPaint(){
            highTempPaint = new Paint();
            highTempPaint.setColor(ContextCompat.getColor(SunshineWatchFace.this, R.color.digital_text));
            highTempPaint.setTypeface(HIGH_TEMP_TYPEFACE);
            highTempPaint.setAntiAlias(true);
        }

        private void initializeLowTempPaint(){
            lowTempPaint = new Paint();
            lowTempPaint.setColor(ContextCompat.getColor(SunshineWatchFace.this, R.color.digital_text));
            lowTempPaint.setTypeface(LOW_TEMP_TYPEFACE);
            lowTempPaint.setAlpha(180);
            lowTempPaint.setAntiAlias(true);
        }

        private void notifyHandheld(){
            PutDataMapRequest dataSyncRequest = PutDataMapRequest.create("/SunshineWatchFaceListenerService/Sync");

            dataSyncRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());

            PutDataRequest dataPutRequest = dataSyncRequest.asPutDataRequest();

            GoogleApiClient googleApiClient = getGoogleApiClient();
            googleApiClient.connect();

            Wearable.DataApi.putDataItem(googleApiClient, dataPutRequest);
            googleApiClient.disconnect();
        }

        private GoogleApiClient getGoogleApiClient(){
            return new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .build();
        }

        @Override
        public void onDestroy() {
            engineUpdateHandler.removeMessages(MSG_UPDATE_TIME);
            engineInstance = null;
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                currentTime.setTimeZone(TimeZone.getDefault());
                currentTime.setTime(new Date());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(timeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();

            float textSize = resources.getDimension(isRound ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float dateTextSize = resources.getDimension(isRound ? R.dimen.round_date_text_size : R.dimen.square_date_text_size);
            float highTempTextSize = resources.getDimension(isRound ? R.dimen.round_high_temp_text_size : R.dimen.square_high_temp_text_size);
            float lowTempTextSize = resources.getDimension(isRound ? R.dimen.round_low_temp_text_size : R.dimen.square_low_temp_text_size);

            timePaint.setTextSize(textSize);
            datePaint.setTextSize(dateTextSize);
            highTempPaint.setTextSize(highTempTextSize);
            lowTempPaint.setTextSize(lowTempTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            supportsLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (isAmbient != inAmbientMode) {
                isAmbient = inAmbientMode;
                if (supportsLowBitAmbient) {
                    timePaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            drawBackground(canvas, bounds);

            drawTime(canvas, bounds);
            if(!isInAmbientMode()){
                drawDate(canvas, bounds);
                drawDividerLine(canvas, bounds);
                drawWeatherIcon(canvas, bounds);
                drawHighAndLowTemp(canvas, bounds);
            }
        }

        private void drawBackground(Canvas canvas, Rect bounds){
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), backgroundPaint);
            }
        }

        private void drawTime(Canvas canvas, Rect bounds){
            currentTime.setTimeInMillis(System.currentTimeMillis());
            String timeText = String.format("%d:%02d", currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE));
            canvas.drawText(timeText, bounds.centerX() - timePaint.measureText(timeText) / 2, timeOffsetY, timePaint);
        }

        private void drawDate(Canvas canvas, Rect bounds){
            String dateText = String.format("%s, %s %02d %04d",
                    currentTime.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()),
                    currentTime.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                    currentTime.get(Calendar.DAY_OF_MONTH),
                    currentTime.get(Calendar.YEAR));
            canvas.drawText(dateText, bounds.centerX() - datePaint.measureText(dateText) / 2, dateOffsetY, datePaint);
        }

        private void drawDividerLine(Canvas canvas, Rect bounds){
            canvas.drawLine(bounds.width() * .25f, lineOffsetY, bounds.width() * .75f, lineOffsetY, linePaint);
        }

        private void drawWeatherIcon(Canvas canvas, Rect bounds){
            if(currentConditionsImage != null){
                int imageWidth = getApplicationContext().getResources().getDimensionPixelSize(R.dimen.weather_icon_size);
                canvas.drawBitmap(currentConditionsImage, (bounds.width() * .15f) - imageWidth / 2, currentWeatherOffsetY - 45, null);
            }
        }

        private void drawHighAndLowTemp(Canvas canvas, Rect bounds){
            if(highTemp == Integer.MIN_VALUE || lowTemp == Integer.MIN_VALUE){
                return;
            }

            String tempFormat = "%d\u00b0";

            String highTempString = String.format(tempFormat, highTemp);
            String lowTempString = String.format(tempFormat, lowTemp);

            canvas.drawText(highTempString, bounds.centerX() - highTempPaint.measureText(highTempString) / 2, currentWeatherOffsetY, highTempPaint);
            canvas.drawText(lowTempString, (bounds.width() * .75f) - lowTempPaint.measureText(lowTempString) / 2, currentWeatherOffsetY, lowTempPaint);
        }


        /**
         * Starts the {@link #engineUpdateHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            engineUpdateHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                engineUpdateHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                engineUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
