package com.romuloceccon.hilgen;

import java.io.IOException;
import java.net.MalformedURLException;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.auth.Permission;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthInterface;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Authentication
{
    private static final String TAG = "AUTH";
    
    private static final String OAUTH_PREFS = "oauth";
    
    private static final String KEY_STATE = "state";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ID= "user_id";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_TOKEN_SECRET = "token_secret";
    
    public static final int UNAUTHORIZED = 0;
    public static final int REQUESTING_AUTHORIZATION = 1;
    public static final int AUTHORIZED = 2;
    
    private static Authentication instance = null;
    
    private OAuthInterface oAuthApi;
    
    private SharedPreferences prefs;
    
    private int state = 0;
    private String oAuthTokenSecret = null;
    private OAuth oAuth = null;
    
    public static synchronized Authentication getInstance(Context ctx, Flickr flickr)
    {
        if (instance == null)
            instance = new Authentication(ctx, flickr);
        return instance;
    }
    
    private Authentication(Context ctx, Flickr flickr)
    {
        oAuthApi = flickr.getOAuthInterface();
        prefs = ctx.getSharedPreferences(OAUTH_PREFS, Context.MODE_PRIVATE);
        
        loadState();
    }
    
    public int getState()
    {
        return state;
    }
    
    public String getOAuthTokenSecret()
    {
        return oAuthTokenSecret; 
    }
    
    public OAuth getOAuth()
    {
        return oAuth;
    }
    
    public String startAuthentication(String scheme)
    {
        // We should allow starting the authentication process either in the
        // UNAUTHORIZED or REQUESTING_AUTHORIZATION states; otherwise we risk
        // getting stuck in the REQUESTING_AUTHORIZATION state in case of
        // a failure
        if (state == AUTHORIZED)
            return null;
        
        String callbackUrl = scheme + "://callback";
        
        OAuthToken oAuthToken;
        String result;
        
        try
        {
            oAuthToken = oAuthApi.getRequestToken(callbackUrl);
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
        
        try
        {
            result = oAuthApi.buildAuthenticationUrl(
                    Permission.READ, oAuthToken).toString();
        }
        catch (MalformedURLException e)
        {
            Log.w(TAG, e);
            return null;
        }
        
        String tokenSecret = oAuthToken.getOauthTokenSecret();
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_STATE, REQUESTING_AUTHORIZATION);
        editor.putString(KEY_TOKEN_SECRET, tokenSecret);
        if (!editor.commit())
            return null;
        
        setVariables(REQUESTING_AUTHORIZATION, tokenSecret, null);
        return result;
    }
    
    public boolean finishAuthentication(String token, String verifier)
    {
        if (state != REQUESTING_AUTHORIZATION)
            return false;
        
        OAuth oAuth;
        
        try
        {
            oAuth = oAuthApi.getAccessToken(token, oAuthTokenSecret, verifier);
        }
        catch (IOException e)
        {
            Log.w(TAG, e);
            return false;
        }
        catch (FlickrException e)
        {
            Log.w(TAG, e);
            return false;
        }
        
        User user = oAuth.getUser();
        OAuthToken oAuthToken = oAuth.getToken();
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_STATE, AUTHORIZED);
        editor.putString(KEY_USER_NAME, user.getUsername());
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_TOKEN, oAuthToken.getOauthToken());
        editor.putString(KEY_TOKEN_SECRET, oAuthToken.getOauthTokenSecret());
        if (!editor.commit())
            return false;
        
        setVariables(AUTHORIZED, null, oAuth);
        return true;
    }
    
    public boolean logout()
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_STATE);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_TOKEN_SECRET);
        if (!editor.commit())
            return false;
        setVariables(UNAUTHORIZED, null, null);
        return true;
    }
    
    private void loadState()
    {
        switch (prefs.getInt(KEY_STATE, 0))
        {
        case REQUESTING_AUTHORIZATION:
            setVariables(REQUESTING_AUTHORIZATION,
                    prefs.getString(KEY_TOKEN_SECRET, ""), null);
            break;
        case AUTHORIZED:
            setVariables(AUTHORIZED, null, loadOAuth());
            break;
        default:
            setVariables(UNAUTHORIZED, null, null);
        }
    }
    
    private OAuth loadOAuth()
    {
        String userName = prefs.getString(KEY_USER_NAME, "");
        String userId = prefs.getString(KEY_USER_ID, "");
        String token = prefs.getString(KEY_TOKEN, "");
        String tokenSecret = prefs.getString(KEY_TOKEN_SECRET, "");
        
        OAuth result = new OAuth();
        
        User user = new User();
        user.setUsername(userName);
        user.setId(userId);
        
        result.setUser(user);
        result.setToken(new OAuthToken(token, tokenSecret));
        
        return result;
    }
    
    private void setVariables(int state, String oAuthTokenSecret, OAuth oAuth)
    {
        this.state = state;
        this.oAuthTokenSecret = oAuthTokenSecret;
        this.oAuth = oAuth;
        FlickrHelper.setOAuth(oAuth);
    }
}
