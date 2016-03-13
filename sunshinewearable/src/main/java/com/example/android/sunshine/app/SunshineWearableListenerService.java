package com.example.android.sunshine.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;

public class SunshineWearableListenerService extends WearableListenerService {

    public static final String WEATHER_UPDATE_REQUEST = "/SunshineWearableListenerService/WeatherData";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents){

        for(int i = 0; i < dataEvents.getCount(); i++){
            DataEvent event = dataEvents.get(i);

            if(event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals(WEATHER_UPDATE_REQUEST)) {
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                SunshineWatchFace.highTemp = dataMap.getInt("high");
                SunshineWatchFace.lowTemp = dataMap.getInt("low");
                Asset weatherIconAsset = dataMap.getAsset("weatherIcon");
                loadBitmapFromAsset(weatherIconAsset);
            }
        }
    }

    private void loadBitmapFromAsset(Asset weatherIconAsset){
        if(null == weatherIconAsset){
            return;
        }

        final GoogleApiClient googleApiClient = getGoogleApiClient();
        googleApiClient.connect();
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                googleApiClient, weatherIconAsset).await().getInputStream();
        googleApiClient.disconnect();

        if (assetInputStream == null) {
            return;
        }

        SunshineWatchFace.currentConditionsImage = BitmapFactory.decodeStream(assetInputStream);
    }

    private GoogleApiClient getGoogleApiClient(){
        return new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }

    public Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }
}
