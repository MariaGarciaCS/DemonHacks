package com.example.demonhacks;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/*
 * Fetches recent train data from CTA's API
 *
 * Implemented JSON Parsing following aadigurung's solution:
 * https://stackoverflow.com/questions/33229869/get-json-data-from-url-using-android
 *
 */
public class JsonParser extends AsyncTask<String, String, String> {

    private ArrayList<Route> routeList;
    private String requestedStation;
    private static final String TAG = "JsonParser";

    public JsonParser(ArrayList<Route> routeList, String requestedStation) {
        this.routeList = routeList;
        this.requestedStation = requestedStation;
    }

    protected String doInBackground(String... params) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));

            String receiveString = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ((receiveString = reader.readLine()) != null) {
                stringBuilder.append(receiveString);
                Log.d(TAG, "doInBackground > " + receiveString);
            }

            return stringBuilder.toString();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // TODO Load Data into Recycler View

        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject jsonObject2 = jsonObject.getJSONObject("ctatt");

            JSONArray jsonArray = jsonObject2.getJSONArray("eta");
            int length = jsonArray.length();

            Log.d(TAG, String.format("doRead: Reading Data for %s trains", length));

            for (int i = 0; i < length; i++) {
                JSONObject trainJsonObj = jsonArray.getJSONObject(i);

                String arrivalTime = trainJsonObj.getString("arrT");
                String latitude = trainJsonObj.getString("lat");
                String longitude = trainJsonObj.getString("lon");
                Train train = new Train(arrivalTime, "", latitude, longitude);

                String color = trainJsonObj.getString("rt");
                String station = trainJsonObj.getString("staId");
                String destination = trainJsonObj.getString("destNm");

                ArrayList<Train> list = new ArrayList<>();
                list.add(train);

                Route route = new Route(color, station, destination, list);

                if (route.getStation().equals(requestedStation)) {
                    int index = routeList.indexOf(route);
                    if (index > -1) routeList.get(index).getTrains().add(train);
                    else routeList.add(route);
                }
            }
            Collections.sort(routeList, new SortByLine());
            for (Route r: routeList) {
                for (int j = 0; j < r.getTrains().size(); j++) {
                    Log.d(TAG, String.format("onPostExecute: %s %s %s", r.getStation(), r.getLine(), r.getTrains().get(j).getArrivalTime()));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}