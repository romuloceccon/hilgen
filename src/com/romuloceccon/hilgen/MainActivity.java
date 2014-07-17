package com.romuloceccon.hilgen;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.people.User;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity
{
    private static final String TAG = "HILGen";
    private static final String SCHEME = "hilgen";
    
    private static Flickr flickrInstance = null;
    
    private Authentication authentication;
    
    private class OAuthStartAuthenticationTask extends AsyncTask<Void, Integer, String>
    {
        @Override
        protected String doInBackground(Void... params)
        {
            return authentication.startAuthentication(SCHEME);
        }
        
        @Override
        protected void onPostExecute(String result)
        {
            redirectUserTo(result);
        }
    }
    
    private class OAuthFinishAuthenticationTask extends AsyncTask<String, Integer, Boolean>
    {
        @Override
        protected Boolean doInBackground(String... params)
        {
            return authentication.finishAuthentication(params[0], params[1]);
        }
        
        @Override
        protected void onPostExecute(Boolean result)
        {
            Log.i(TAG, result ? "OAuth success" : "OAuth failure");
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        authentication = Authentication.getInstance(getApplicationContext(),
                getFlickr());
        
        if (authentication.getState() == Authentication.UNAUTHORIZED)
            new OAuthStartAuthenticationTask().execute();
    }
    
    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent = getIntent();
        String scheme = intent.getScheme();

        if (scheme != null && scheme.compareTo(SCHEME) == 0)
        {
            handleFlickrCallback(intent);
            return;
        }
        
        if (authentication.getState() == Authentication.AUTHORIZED)
        {
            User user = authentication.getOAuth().getUser();
            Log.i(TAG, String.format("Current logged in user: %s (%s)",
                    user.getUsername(), user.getId()));
        }
    }
    
    private void redirectUserTo(String url)
    {
        if (url == null)
        {
            Log.w(TAG, "Could not build authentication url");
            return;
        }
        
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
    
    private void handleFlickrCallback(Intent intent)
    {
        Uri uri = intent.getData();
        String query = uri.getQuery();
        String[] data = query.split("&");
        
        if (data == null || data.length != 2)
        {
            Log.w(TAG, "Invalid callback query");
            return;
        }
        
        String token = data[0].substring(data[0].indexOf("=") + 1);
        String verifier = data[1].substring(data[1].indexOf("=") + 1);
        
        new OAuthFinishAuthenticationTask().execute(token, verifier);
    }
    
    private Flickr getFlickr()
    {
        if (flickrInstance == null)
            flickrInstance = new Flickr("5448446a4bf6e01575a04886bb481d61",
                    "7230d66b6e1ed0c6");
        
        return flickrInstance;
    }
}
