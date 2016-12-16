package sg.com.sgpwr.sgpwrpsiapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

public class MapsWithPSIActivity extends FragmentActivity implements OnMapReadyCallback {

    // widget
    private GoogleMap mMap;

    // API data
    private ArrayList<HashMap<String, String>> region_metadata_arrlist;
    private ArrayList<HashMap<String, String>> items_arrlist;

    // icon HashMap
    private HashMap<String, String> icon_map;

    // API
    private final String STR_URL = "https://api.data.gov.sg/v1/environment/psi?";
    private final String STR_URL_DATA = "https://api.data.gov.sg/v1/environment/psi?date=2016-12-13";
    private final String STR_DATE = "date=";
    private final String API_KEY = "api-key";
    private final String API_VALUE = "FaaAsN5AB6gFfxbVafXkFGfiHIMGAVc0";

    // Debug tag
    private final String TAG = "MapsWithPSIActivityTAG";
    private final String TEST_TAG = "TestTAG";

    // Connection detector class
    private ConnectionDetector cd;

    // flag for Internet connection status
    private Boolean isInternetPresent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_with_psi);

        // put icon into Hashmap with K, V
        setIconHashMap();

        // creating connection detector class instance
        cd = new ConnectionDetector(getApplicationContext());

        // get Internet status
        isInternetPresent = cd.isConnectingToInternet();

        if (isInternetPresent) {

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            new callPsiApi_HttpAsyncTask().execute(STR_URL + STR_DATE + getCurrentDate());


            Log.d(TAG, "getCurrentDate= " + getCurrentDate());
            Log.d(TAG, getString(R.string.internet_connection));

        } else {

            showSnackBarRed(Snackbar.make(findViewById(android.R.id.content), getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG));
            Log.d(TAG, getString(R.string.no_internet_connection));
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void setIconHashMap(){

        icon_map = new HashMap<String, String>();
        icon_map.put(getString(R.string.south_key), String.valueOf(R.drawable.south));
        icon_map.put(getString(R.string.north_key), String.valueOf(R.drawable.north));
        icon_map.put(getString(R.string.east_key), String.valueOf(R.drawable.east));
        icon_map.put(getString(R.string.central_key), String.valueOf(R.drawable.central));
        icon_map.put(getString(R.string.west_key), String.valueOf(R.drawable.west));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // first time loading, show location as singapore
        LatLng currentLocation = new LatLng(1.35735, 103.82);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 10));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    // show information to user
    public void showSnackBarWhite(Snackbar snackbar) {
        View sbView = snackbar.getView();
        TextView tvSnackBar = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        tvSnackBar.setTextColor(Color.WHITE);
        snackbar.show();
    }

    // show information to user with color RED
    public void showSnackBarRed(Snackbar snackbar) {
        View sbView = snackbar.getView();
        TextView tvSnackBar = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        tvSnackBar.setTextColor(Color.RED);
        snackbar.show();
    }

    // get current date time e.g., 2016-12-09T01:50:00
    public String getCurrentDateTime() {

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");  //MM/dd/yyyy HH:mm:ss
        String strDate = sdf.format(calendar.getTime());

        return strDate;
    }

    // get current date time e.g., 2016-12-09
    public String getCurrentDate() {

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  //MM/dd/yyyy HH:mm:ss
        String strDate = sdf.format(calendar.getTime());

        return strDate;
    }

    // convert from API response data to String
    public static String convertInputStreamToString(InputStream inputStream) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    // call API to get PSI data
    private class callPsiApi_HttpAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... urls) {

            return callPsiApi_GET(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {

            Log.d(TAG, "@onPostExecute=> result= " + result);
            if (result != null) {

                JSONObject whole_Json_Obj = null;
                try {
                    whole_Json_Obj = new JSONObject(result);

                    if (whole_Json_Obj != null) {

                        /*************************************************items************************************************************/
                        JSONArray items_json_arr = whole_Json_Obj.getJSONArray(getString(R.string.items_key));
                        if (items_json_arr != null) {

                            items_arrlist = new ArrayList<HashMap<String, String>>();

                            if (items_json_arr.length() > 0) {

                                HashMap<String, String> items_map = new HashMap<String, String>();

                                //////////////////////////// take LAST json Object of items_json_arr ////////////////////////////

                                // time_stamp (String)
                                JSONObject items_json_obj = items_json_arr.getJSONObject(items_json_arr.length() - 1);
                                items_map.put(getString(R.string.update_timestamp_key), items_json_obj.getString(getString(R.string.update_timestamp_key)));
                                items_map.put(getString(R.string.timestamp_key), items_json_obj.getString(getString(R.string.timestamp_key)));


                                JSONObject readings_json_obj = items_json_obj.getJSONObject(getString(R.string.readings_key));
                                JSONObject psi_twenty_four_hourly_json_obj = readings_json_obj.getJSONObject(getString(R.string.psi_twenty_four_hourly_key));

                                // psi value (int to String)
                                items_map.put(getString(R.string.south_psi_value), String.valueOf(psi_twenty_four_hourly_json_obj.getInt(getString(R.string.south_key))));
                                items_map.put(getString(R.string.north_psi_value), String.valueOf(psi_twenty_four_hourly_json_obj.getInt(getString(R.string.north_key))));
                                items_map.put(getString(R.string.east_psi_value), String.valueOf(psi_twenty_four_hourly_json_obj.getInt(getString(R.string.east_key))));
                                items_map.put(getString(R.string.central_psi_value), String.valueOf(psi_twenty_four_hourly_json_obj.getInt(getString(R.string.central_key))));
                                items_map.put(getString(R.string.west_psi_value), String.valueOf(psi_twenty_four_hourly_json_obj.getInt(getString(R.string.west_key))));


                                items_arrlist.add(items_map);
                            }
                            // going to ouput whether get the correct data or not
                            show_items_arrlist();

                        } else {

                            showSnackBarRed(Snackbar.make(findViewById(android.R.id.content), getString(R.string.return_data_null), Snackbar.LENGTH_LONG));
                            Log.d(TAG, getString(R.string.return_data_null));
                            Log.d(TAG, "Return Items Json Array= " + items_json_arr.toString());
                        }
                        /*************************************************items************************************************************/

                        /*************************************************region_metadata************************************************************/
                        JSONArray region_metadata_json_arr = whole_Json_Obj.getJSONArray(getString(R.string.region_metadata_key));
                        if (region_metadata_json_arr != null) {

                            region_metadata_arrlist = new ArrayList<HashMap<String, String>>();
                            for (int i = 0; i < region_metadata_json_arr.length(); i++) {

                                HashMap<String, String> region_metadata_map = new HashMap<String, String>();

                                JSONObject region_metadata_json_obj = region_metadata_json_arr.getJSONObject(i);

                                // name:south (String)
                                if ((region_metadata_json_obj.getString(getString(R.string.name_key))).equals(getString(R.string.national_value))) {

                                    Log.d(TEST_TAG, "no need to add \"national\" data to region_metadata_arrlist");
                                }
                                else {

                                    region_metadata_map.put(getString(R.string.name_key), region_metadata_json_obj.getString(getString(R.string.name_key)));

                                // latitude, longitude (Double to String)
                                JSONObject location_json_obj = region_metadata_json_obj.getJSONObject(getString(R.string.label_location_key));
                                region_metadata_map.put(getString(R.string.latitude_key), String.valueOf(location_json_obj.getDouble(getString(R.string.latitude_key))));
                                region_metadata_map.put(getString(R.string.longitude_key), String.valueOf(location_json_obj.getDouble(getString(R.string.longitude_key))));

                                region_metadata_arrlist.add(region_metadata_map);
                                }
                            }
                            // going to ouput whether get the correct data or not
                            // plot the location data on Google Map
                            get_region_metadata_arrlist();

                        } else {

                            showSnackBarRed(Snackbar.make(findViewById(android.R.id.content), getString(R.string.return_data_null), Snackbar.LENGTH_LONG));
                            Log.d(TAG, getString(R.string.return_data_null));
                            Log.d(TAG, "Return Region Metadata Array= " + region_metadata_json_arr.toString());
                        }

                        /*************************************************region_metadata************************************************************/


                    } else {

                        showSnackBarRed(Snackbar.make(findViewById(android.R.id.content), getString(R.string.return_data_null), Snackbar.LENGTH_LONG));
                        Log.d(TAG, getString(R.string.return_data_null));
                        Log.d(TAG, "Return Whole Json Object= " + whole_Json_Obj.toString());
                    }

                } catch (JSONException e) {

                    showSnackBarRed(Snackbar.make(findViewById(android.R.id.content), getString(R.string.json_exception_catch), Snackbar.LENGTH_LONG));
                    Log.d(TAG, getString(R.string.json_exception_catch));
                    e.printStackTrace();
                }
            } else {

                showSnackBarRed(Snackbar.make(findViewById(android.R.id.content), getString(R.string.return_data_null), Snackbar.LENGTH_LONG));
                Log.d(TAG, getString(R.string.return_data_null));
            }
        }
    }
    public String callPsiApi_GET(String strUrl) {

        InputStream inputStream = null;
        String result = "";

        HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        DefaultHttpClient client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpClient = new DefaultHttpClient(mgr, client.getParams());

// Set verifier
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

// Example send http request
        HttpGet httpGet = new HttpGet(strUrl);

        httpGet.setHeader("Content-type", "application/json");
        httpGet.setHeader(API_KEY, API_VALUE);

        try {

            HttpResponse httpResponse = httpClient.execute(httpGet);

            inputStream = httpResponse.getEntity().getContent();

            if (inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

            Log.d(TAG, "@callPsiApi_GET => result = " + result);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    // get location data and call to parseRegionMetadata()
    public void get_region_metadata_arrlist() {

        if (region_metadata_arrlist.size() > 0) {
            for (int i = 0; i < region_metadata_arrlist.size(); i++) {

                String name = region_metadata_arrlist.get(i).get(getString(R.string.name_key));
                String strLati = region_metadata_arrlist.get(i).get(getString(R.string.latitude_key));
                String strLongi = region_metadata_arrlist.get(i).get(getString(R.string.longitude_key));

                // parse data and plot data on Google Map with Marker
                showDataonGoogleMap(name, Double.parseDouble(strLati), Double.parseDouble(strLongi));
                Log.d(TEST_TAG, "i= " + i + " & Name= " + name + " & strLati= " + strLati + " & strLongi= " + strLongi);
            }
        } else {

            Log.d(TEST_TAG, "region_metadata_arrlist.size()= " + region_metadata_arrlist.size());
        }

    }

    // call get_items_arrlist() to get psi data and show
    public void showDataonGoogleMap(String regionName, Double regionLati, Double regionLongi) {

        LatLng regionLocation = new LatLng(regionLati, regionLongi);
        if (regionName.equals(getString(R.string.national_value))) {

            Log.d(TEST_TAG, "Double Check => national data");

        } else {

            Log.d(TEST_TAG, "regionName= " + regionName + " & psiValue= " + get_items_arrlist(regionName));

            mMap.addMarker(new MarkerOptions()
                            .position(regionLocation)
                            .title(regionName + "\'s readings:")
                            .snippet(getString(R.string.psi_twenty_four_hourly_key) + ": " + get_items_arrlist(regionName))
                            .icon(BitmapDescriptorFactory.fromResource(Integer.parseInt(icon_map.get(regionName))))
            );
        }
    }

    public String get_items_arrlist(String regionName) {

        if (items_arrlist.size() > 0) {
            for (int i = 0; i < items_arrlist.size(); i++) {


                String psiValue = items_arrlist.get(i).get(regionName+"_psi");
                return psiValue;
            }
        } else {

            Log.d(TEST_TAG, "region_metadata_arrlist.size()= " + region_metadata_arrlist.size());
        }
        return null;
    }

    public void show_items_arrlist() {

        if (items_arrlist.size() > 0) {
            for (int i = 0; i < items_arrlist.size(); i++) {

                String update_timestamp = items_arrlist.get(i).get(getString(R.string.update_timestamp_key));
                String timestamp = items_arrlist.get(i).get(getString(R.string.timestamp_key));

                String strSouth_PsiValue = items_arrlist.get(i).get(getString(R.string.south_psi_value));
                String strNorth_PsiValue = items_arrlist.get(i).get(getString(R.string.north_psi_value));
                String strEast_PsiValue = items_arrlist.get(i).get(getString(R.string.east_psi_value));
                String strCentral_PsiValue = items_arrlist.get(i).get(getString(R.string.central_psi_value));
                String strWest_PsiValue = items_arrlist.get(i).get(getString(R.string.west_psi_value));

                Log.d(TEST_TAG, "i= " + i + " & update_timestamp= " + update_timestamp
                        + " & timestamp= " + timestamp
                        + " & strSouth_PsiValue= " + strSouth_PsiValue
                        + " & strNorth_PsiValue= " + strNorth_PsiValue
                        + " & strEast_PsiValue= " + strEast_PsiValue
                        + " & strCentral_PsiValue= " + strCentral_PsiValue
                        + " & strWest_PsiValue= " + strWest_PsiValue
                );
            }
        } else {

            Log.d(TEST_TAG, "region_metadata_arrlist.size()= " + region_metadata_arrlist.size());
        }
    }

}
