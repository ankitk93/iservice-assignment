package iservice.task.android.com.iserviceassignment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import iservice.task.android.com.iserviceassignment.utils.DirectionsJSONParser;
import iservice.task.android.com.iserviceassignment.utils.GpsTracker;
import iservice.task.android.com.iserviceassignment.utils.HttpRequest;
import iservice.task.android.com.iserviceassignment.utils.ImageUtil;

public class MainActivity extends AppCompatActivity {

    GpsTracker mGpsTracker;
    double mLatitude , mLongitude;
    Button mGetLocationButton , mClickImageButton ,mPostDataButton ;
    GoogleMap mGoogleMap;

    LatLng origin ,destination;
    ArrayList<LatLng> points = null;
    PolylineOptions lineOptions = null;
    String locationRoutes;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap mCameraImageBitmap;
    private String mCameraImagePath;
    private ImageView mImageView;

    //post data variables
    String image_string , LocationCoordinates , locationPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getLocation();

        mImageView = (ImageView)findViewById(R.id.iv_userImage);
        mClickImageButton = (Button)findViewById(R.id.btn_capture);
        mClickImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntetn = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(cameraIntetn.resolveActivity(getPackageManager()) != null){
                    File photoFile = null;
                    try{
                        photoFile = createImageFile();
                    }catch(IOException ex){
                        ex.printStackTrace();
                    }

                    if (photoFile != null){
                        cameraIntetn.putExtra(MediaStore.EXTRA_OUTPUT , Uri.fromFile(photoFile));
                        startActivityForResult(cameraIntetn,REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });

        mGetLocationButton = (Button)findViewById(R.id.btn_currentLocation);
        mGetLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getLocation();
                String address = "iService, Koramangala ,Bangalore ,karnataka 560034";
                getLocationFromAddress(getBaseContext(),address);
                Toast.makeText(getBaseContext(),"location" + destination +"",Toast.LENGTH_LONG).show();

                origin = new LatLng(mLatitude,mLongitude);
                String url = getDirectionsUrl(origin,destination);
                DownloadTask task = new DownloadTask();
                task.execute(url,null,null);
            }
        });

        mPostDataButton = (Button)findViewById(R.id.btn_postData);
        mPostDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImageView.setImageBitmap(ImageUtil.StringToBitMap(imageString()));
                updateToSpreadsheet();
            }
        });
    }

    public void updateToSpreadsheet() {
        boolean connectionAvailable= isNetworkAvailable();
        if(connectionAvailable== true)
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    postData();
                }
            });
            t.start();
            Toast.makeText(getBaseContext(), "saved in Spreadsheet", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(getBaseContext(), "NO proper connection", Toast.LENGTH_LONG).show();
        }
    }

    public String imageString(){
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.me);
        String imageString = ImageUtil.bitMapToString(mCameraImageBitmap);

        Log.d("image string", "Image string :" + imageString);

        return imageString.toString();
    }

    public void getLocation(){
        mGpsTracker = new GpsTracker(MainActivity.this);

        if (mGpsTracker.canGetLocation()){
            mLatitude = mGpsTracker.getLatitude();
            mLongitude = mGpsTracker.getLongitude();
            //Toast.makeText(getBaseContext(),"Latitude : " + Double.toString(mLatitude)+" , longitude : " +Double.toString(mLongitude),Toast.LENGTH_LONG).show();
        }else {
            mGpsTracker.showSettingsAlert();
        }
    }

    public void postData(){
        String fullUrl = getString(R.string.new_spreasheet);
        HttpRequest mReq = new HttpRequest();

        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.me);
        String imageString = imageString();
        image_string = imageString.substring(0,10000);

        LocationCoordinates = "Latitude : " + Double.toString(mLatitude)+" , longitude : " +Double.toString(mLongitude);
        Log.d("Location Coordinates" , LocationCoordinates);
        Log.d("image string" , image_string);

        locationPath = locationRoutes.substring(0,9000);
        //Log.d("location path",locationPath);
        //String locationPath = "fkyhxgcyghjgfxicgfxthkgcgchg";
        String data = getString(R.string.image_string) + URLEncoder.encode(image_string) + "&" +
                getString(R.string.geo_lat_long) + URLEncoder.encode(LocationCoordinates) + "&" + getString(R.string.location_string) + URLEncoder.encode(locationPath);
        String response = mReq.sendPost(fullUrl,data);
    }

    // network available
    private boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public File createImageFile() throws IOException{
        //create image file name
        String timeStamp = new SimpleDateFormat("yyyyMMDD_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storeDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(imageFileName,".jpg" , storeDirectory);

        mCameraImagePath = "file:"+image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            try{
                mCameraImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver() , Uri.parse(mCameraImagePath));
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }

    // location path between origin and destination
    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();
            Log.d("data" , data);

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {
        private ProgressDialog dialog;
        // Downloading data in non-ui thread

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("background data" , data);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
                locationRoutes = jObject.toString();
                Log.d("routes" , routes.toString());
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            /*ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;*/
            MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(2);
                lineOptions.color(Color.RED);
                Log.d("location points" , points.toString());
            }

            // Drawing polyline in the Google Map for the i-th route
            //map.addPolyline(lineOptions);
        }
    }

    /**
     * method to get latitude and longitude from address
     */
    public LatLng getLocationFromAddress(Context context,String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress,10);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            destination = new LatLng(location.getLatitude(), location.getLongitude() );

        } catch (Exception ex) {

            ex.printStackTrace();
        }

        return destination;
    }
}
