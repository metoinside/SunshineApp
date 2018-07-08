package com.metoinside.sunshine;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private TextView mWeatherStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mWeatherStatus = findViewById(R.id.weather_status);

        String location = "94043,USA";
        new GetWeatherTask().execute(location);
    }

    // Async class
    public static class GetWeatherTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... strings) {
            if(strings.length == 0) {
                return null;
            }

            String location = strings[0];
            URL weatherOfLocation = buildUrl(location);

            try {
                String jsonWeatherLocationResponse = response(weatherOfLocation);
                return simpleJsonHandling(MainActivity.this, jsonWeatherLocationResponse);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if (strings != null) {
                for (String weaterString : strings) {
                    mWeatherStatus.append(weaterString + "\n\n\n");
                }
            }
        }
    }

    public static URL buildUrl(String location) {
        Uri builtUri = Uri.parse("https://andfun-weather.udacity.com/staticweather").buildUpon()
                .appendQueryParameter("q", location)
                .appendQueryParameter("mode", "json")
                .appendQueryParameter("units", "metric")
                .appendQueryParameter("cnt", Integer.toString(14))
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Log.v(MainActivity.class.getSimpleName(),"Built URI " + url);
        return url;
    }

    public static String response(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            }
            else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public static String[] simpleJsonHandling(Context context, String forecastJsonStr) throws JSONException {
        String[] parsedWeatherData = null;
        JSONObject forecastJson = new JSONObject(forecastJsonStr);

        //error checking
        if (forecastJson.has("cod")) {
            int errorCode = forecastJson.getInt("cod");
            switch (errorCode) {
                case HttpURLConnection.HTTP_OK:
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    return null;
                default:
                    return null;
            }
        }

        JSONArray weatherArray = forecastJson.getJSONArray("list");
        parsedWeatherData = new String[weatherArray.length()];

        long localDate = System.currentTimeMillis();
        long utcDate = SunshineAppDateUtils.getUTCDateFromLocal(localDate);
        long startDay = SunshineAppDateUtils.normalizeDate(utcDate);

        for (int i = 0; i < weatherArray.length(); i++) {
            String date;
            String highAndLow;

            // values to be collected
            long dateTimeMilis;
            double high;
            double low;
            String description;

            JSONObject dayForecast = weatherArray.getJSONObject(i);
            dateTimeMilis = startDay + SunshineAppDateUtils.DAY_IN_MILLIS * i;
            date = SunshineAppDateUtils.getFriendlyDateString(context, dateTimeMilis, false);

            JSONObject weatherObject = dayForecast.getJSONArray("weather").getJSONObject(0);
            description = weatherObject.getString("main");

            JSONObject temperatureObjet = dayForecast.getJSONObject("temp");
            high = temperatureObjet.getDouble("max");
            low = temperatureObjet.getDouble("min");
            highAndLow = formatHighLows(context, high, low);

            parsedWeatherData[i] = date + " - " + description + " - " + highAndLow;
        }
        return parsedWeatherData;
    }

    public static String formatHighLows(Context context, double high, double low) {
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String formattedHigh = formatTemperature(context, roundedHigh);
        String formattedLow = formatTemperature(context, roundedLow);

        return formattedHigh + " / " + formattedLow;
    }

    public static String formatTemperature(Context context, double temperature) {
        int temperatureFormatResourceId = R.string.format_temperature_celsius;
        return String.format(context.getString(temperatureFormatResourceId), temperature);
    }
}
