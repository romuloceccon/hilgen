package com.romuloceccon.hilgen;

import java.io.IOException;
import java.util.Collection;

import org.json.JSONException;

import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photosets.Photoset;
import com.googlecode.flickrjandroid.photosets.PhotosetsInterface;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
    private static final String TAG = "HILGen";
    private static final String SCHEME = "com-romuloceccon-hilgen";
    
    private Authentication authentication;
    
    private Button button;
    private TextView textView;
    
    private Button buttonGetPhotosets;
    
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
            if (result == null)
            {
                showToast(getString(R.string.message_start_authentication_failed));
                return;
            }
            
            redirectUserTo(result);
            updateState();
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
            if (!result)
            {
                showToast(getString(R.string.message_finish_authentication_failed));
                return;
            }
            
            updateState();
        }
    }
    
    private class GetPhotosetsTask extends AsyncTask<Void, Integer, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... arg0)
        {
            PhotosetsInterface intf = FlickrHelper.getFlickr().getPhotosetsInterface();
            
            OAuth oAuth = authentication.getOAuth();
            if (oAuth == null)
                return false;
            
            try
            {
                Collection<Photoset> photosets;
                int page = 1;
                final int perPage = 10;
                
                do
                {
                    try
                    {
                        photosets = intf.getList(oAuth.getUser().getId(), perPage, page).getPhotosets();
                    }
                    catch (FlickrException e)
                    {
                        if (e.getErrorCode().compareTo("1") == 0) // empty page
                            break;
                        throw e;
                    }
                    
                    for (Photoset p: photosets)
                        dumpPhotoset(intf, p);
                    page += 1;
                } while (photosets.size() >= perPage);
                
                return true;
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
            catch (JSONException e)
            {
                Log.w(TAG, e);
                return false;
            }
        }
        
        private void dumpPhotoset(PhotosetsInterface intf, Photoset photoset)
                throws IOException, FlickrException, JSONException
        {
            Log.i(TAG, String.format("Photoset %s: %s", photoset.getId(), photoset.getTitle()));
            
            PhotoList photos;
            int page = 1;
            final int perPage = 10;
            
            do
            {
                Log.i(TAG, String.format("Getting page %d of set %s...", page, photoset.getId()));
                
                try
                {
                    photos = intf.getPhotos(photoset.getId(), perPage, page).getPhotoList();
                }
                catch (FlickrException e)
                {
                    if (e.getErrorCode().compareTo("1") == 0) // empty page
                        break;
                    throw e;
                }
                
                for (Photo p: photos)
                    Log.i(TAG, String.format("  Photo %s: %s", p.getId(), p.getMediumUrl()));
                
                page += 1;
            } while (photos.size() >= perPage);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        authentication = Authentication.getInstance(getApplicationContext(),
                FlickrHelper.getFlickr());
        
        button = (Button) findViewById(R.id.button_login);
        textView = (TextView) findViewById(R.id.text_login);
        
        buttonGetPhotosets = (Button) findViewById(R.id.button_get_photosets);
        buttonGetPhotosets.setOnClickListener(buttonGetPhotosetsListener);
        
        updateState();
    }
    
    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent = getIntent();
        String scheme = intent.getScheme();

        if (scheme != null && scheme.compareTo(SCHEME) == 0)
            handleFlickrCallback(intent);
    }
    
    private void redirectUserTo(String url)
    {
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
        
        new OAuthFinishAuthenticationTask().execute(
                getDataValue(data[0]), getDataValue(data[1]));
    }
    
    private String getDataValue(String q)
    {
        return q.substring(q.indexOf("=") + 1);
    }
    
    void showToast(CharSequence msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    
    void updateState()
    {
        switch (authentication.getState())
        {
        case Authentication.AUTHORIZED:
            button.setText(getString(R.string.button_logout));
            button.setOnClickListener(logoutAction);
            textView.setText(getString(R.string.text_logged_in, authentication.getOAuth().getUser().getUsername()));
            break;
        case Authentication.REQUESTING_AUTHORIZATION:
            button.setText(getString(R.string.button_cancel_authentication));
            button.setOnClickListener(logoutAction);
            textView.setText(getString(R.string.text_waiting_authentication));
            break;
        default:
            button.setText(getString(R.string.button_start_authentication));
            button.setOnClickListener(startAuthenticationAction);
            textView.setText(getString(R.string.text_logged_out));
            break;
        }
    }
    
    private OnClickListener startAuthenticationAction = new OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            new OAuthStartAuthenticationTask().execute();
        }
    };
    
    private OnClickListener logoutAction = new OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            authentication.logout();
            updateState();
        }
    };
    
    private OnClickListener buttonGetPhotosetsListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            new GetPhotosetsTask().execute();
        }
    };
}
