package com.example.android.sunshine.app;

import android.graphics.BitmapFactory;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

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
        byte[] data = weatherIconAsset.getData();
        SunshineWatchFace.currentConditionsImage = BitmapFactory.decodeByteArray(data, 0, data.length);
    }
}
