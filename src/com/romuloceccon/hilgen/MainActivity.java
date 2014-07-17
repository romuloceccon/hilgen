package com.romuloceccon.hilgen;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.auth.Permission;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthInterface;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity
{
    private static final String TAG = "HILGen";
    private static final String SCHEME = "hilgen";
    private static final String PREFS = "hilgen";
    
    private static final String KEY_OAUTH_USER_NAME = "oauth_user_name";
    private static final String KEY_OAUTH_USER_ID= "oauth_user_id";
    private static final String KEY_OAUTH_TOKEN = "oauth_token";
    private static final String KEY_OAUTH_TOKEN_SECRET = "oauth_token_secret";
    
    private static Flickr flickrInstance = null;
    
    private class OauthAuthenticationRequestTask extends AsyncTask<Void, Integer, String>
    {
        @Override
        protected String doInBackground(Void... params)
        {
            return requestOauthAuthentication();
        }
        
        @Override
        protected void onPostExecute(String result)
        {
            redirectUserTo(result);
        }
    }
    
    private class OauthAccessTokenRequestTask extends AsyncTask<String, Integer, OAuth>
    {

        @Override
        protected OAuth doInBackground(String... params)
        {
            return requestAccessToken(params[0], params[1], params[2]);
        }
        
        @Override
        protected void onPostExecute(OAuth result)
        {
            saveOAuth(result);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (loadOAuth().getToken() == null)
            new OauthAuthenticationRequestTask().execute();
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
        
        OAuth oauth = loadOAuth();
        User user = oauth.getUser();
        if (user != null)
            Log.i(TAG, String.format("Current logged in user: %s (%s)",
                    user.getUsername(), user.getId()));
    }
    
    private String requestOauthAuthentication()
    {
        String callbackUrl = SCHEME + "://callback";
        Flickr f = getFlickr();
        
        OAuthToken oauthToken;
        URL oauthUrl;
        
        try
        {
            oauthToken = f.getOAuthInterface().getRequestToken(callbackUrl);
        }
        catch (IOException e)
        {
            Log.w(TAG, e);
            return null;
        }
        catch (FlickrException e)
        {
            Log.w(TAG, e);
            return null;
        }
        
        saveOAuth(null, null, null, oauthToken.getOauthTokenSecret());
        
        try
        {
            oauthUrl = f.getOAuthInterface().buildAuthenticationUrl(
                    Permission.READ, oauthToken);
        }
        catch (MalformedURLException e)
        {
            Log.w(TAG, e);
            return null;
        }
        
        return oauthUrl.toString();
    }
    
    private OAuth requestAccessToken(String tokenSecret, String token,
            String verifier)
    {
        Flickr f = getFlickr();
        OAuthInterface oauthApi = f.getOAuthInterface();
        
        try
        {
            return oauthApi.getAccessToken(token, tokenSecret, verifier);
        }
        catch (IOException e)
        {
            Log.w(TAG, e);
            return null;
        }
        catch (FlickrException e)
        {
            Log.w(TAG, e);
            return null;
        }
    }
    
    private void saveOAuth(String userName, String userId, String token,
            String tokenSecret)
    {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS,
                Context.MODE_PRIVATE).edit();
        
        editor.putString(KEY_OAUTH_USER_NAME, userName);
        editor.putString(KEY_OAUTH_USER_ID, userId);
        editor.putString(KEY_OAUTH_TOKEN, token);
        editor.putString(KEY_OAUTH_TOKEN_SECRET, tokenSecret);
        editor.commit();
        
        Log.i(TAG, String.format("SAVED TOKEN: %s, %s, %s, %s", userName,
                userId, token, tokenSecret));
    }
    
    private void saveOAuth(OAuth oauth)
    {
        if (oauth == null)
        {
            Log.w(TAG, "Authorization failed");
            return;
        }
        
        User user = oauth.getUser();
        OAuthToken oauthToken = oauth.getToken();
        
        if (user == null || oauthToken == null)
        {
            Log.w(TAG, "Authorization failed");
            return;
        }
        
        String userName = user.getUsername();
        String userId = user.getId();
        String token = oauthToken.getOauthToken();
        String tokenSecret = oauthToken.getOauthTokenSecret();
        
        if (userName == null || userId == null || token == null ||
                tokenSecret == null)
        {
            Log.w(TAG, "Authorization failed");
            return;
        }
        
        saveOAuth(userName, userId, token, tokenSecret);
    }
    
    private OAuth loadOAuth()
    {
        SharedPreferences prefs = getSharedPreferences(PREFS,
                Context.MODE_PRIVATE);
        
        String userName = prefs.getString(KEY_OAUTH_USER_NAME, null);
        String userId = prefs.getString(KEY_OAUTH_USER_ID, null);
        String token = prefs.getString(KEY_OAUTH_TOKEN, null);
        String tokenSecret = prefs.getString(KEY_OAUTH_TOKEN_SECRET, null);
        
        OAuth result = new OAuth();
        
        if (userId != null)
        {
            User user = new User();
            user.setUsername(userName);
            user.setId(userId);
            result.setUser(user);
        }
        
        if (tokenSecret != null)
            result.setToken(new OAuthToken(token, tokenSecret));
        
        return result;
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
        
        OAuth oauth = loadOAuth();
        OAuthToken oauthToken = oauth.getToken();
                
        if (oauthToken == null)
        {
            Log.w(TAG, "Got a callback query, but no OAuth token secret is " +
                    "defined yet");
            return;
        }
        
        String tokenSecret = oauthToken.getOauthTokenSecret();
        
        if (tokenSecret == null)
        {
            Log.e(TAG, "Got a callback query and found a OAuth token without " +
                    "a token secret: looks like a bug");
            return;
        }
        
        new OauthAccessTokenRequestTask().execute(tokenSecret, token, verifier);
    }
    
    private Flickr getFlickr()
    {
        if (flickrInstance == null)
            flickrInstance = new Flickr("5448446a4bf6e01575a04886bb481d61",
                    "7230d66b6e1ed0c6");
        
        return flickrInstance;
    }
}
