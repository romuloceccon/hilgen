package com.romuloceccon.hilgen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;

import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.photosets.Photoset;
import com.googlecode.flickrjandroid.photosets.PhotosetsInterface;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class UserActivity extends Activity
{
    private static final String TAG = "HILGen";
    
    private static final String SCHEME = "com-romuloceccon-hilgen";
    
    private Authentication authentication;
    
    private Button button;
    private TextView textView;
    private BaseAdapter photosetsAdapter;
    private List<Photoset> photosets = new ArrayList<Photoset>();
    
    private class OAuthStartAuthenticationTask extends AsyncTask<Void, Integer, String>
    {
        private Exception error = null;
        
        @Override
        protected String doInBackground(Void... params)
        {
            try
            {
                return authentication.startAuthentication(SCHEME);
            }
            catch (IOException e)
            {
                Log.w(TAG, e);
                error = e;
                return null;
            }
            catch (FlickrException e)
            {
                Log.w(TAG, e);
                error = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String result)
        {
            if (result == null)
            {
                ActivityUtils.showToast(UserActivity.this,
                        getString(R.string.message_start_authentication_failed),
                        error);
                return;
            }
            
            redirectUserTo(result);
            updateState();
        }
    }
    
    private class OAuthFinishAuthenticationTask extends AsyncTask<String, Integer, Boolean>
    {
        private Exception error = null;
        
        @Override
        protected Boolean doInBackground(String... params)
        {
            try
            {
                return authentication.finishAuthentication(params[0], params[1]);
            }
            catch (IOException e)
            {
                Log.w(TAG, e);
                error = e;
                return null;
            }
            catch (FlickrException e)
            {
                Log.w(TAG, e);
                error = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result)
        {
            if (!result)
            {
                ActivityUtils.showToast(UserActivity.this,
                        getString(R.string.message_finish_authentication_failed),
                        error);
                return;
            }
            
            updateState();
        }
    }
    
    private class GetPhotosetsTask extends AsyncTask<Void, Integer, List<Photoset>>
    {
        private Exception error = null;
        
        @Override
        protected List<Photoset> doInBackground(Void... arg0)
        {
            OAuth oAuth = authentication.getOAuth();
            if (oAuth == null)
                return null;
            
            FlickrHelper.setOAuth(oAuth);
            
            List<Photoset> result = new ArrayList<Photoset>();
            PhotosetsInterface intf = FlickrHelper.getFlickr().getPhotosetsInterface();
            
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
                        result.add(p);
                    page += 1;
                } while (photosets.size() >= perPage);
                
                return result;
            }
            catch (IOException e)
            {
                Log.w(TAG, e);
                error = e;
                return null;
            }
            catch (FlickrException e)
            {
                Log.w(TAG, e);
                error = e;
                return null;
            }
            catch (JSONException e)
            {
                Log.w(TAG, e);
                error = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<Photoset> result)
        {
            if (result == null)
                ActivityUtils.showToast(UserActivity.this,
                        getString(R.string.message_get_photosets_failed), error);
            
            updatePhotosetList(result);
        }
    }
    
    public class Adapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return photosets.size();
        }

        @Override
        public Object getItem(int pos)
        {
            return photosets.get(pos);
        }

        @Override
        public long getItemId(int arg0)
        {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View result = convertView;
            if (result == null)
            {
                LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                result = li.inflate(R.layout.listview_item, parent, false);
            }
            
            TextView tv = (TextView) result.findViewById(R.id.item);
            Photoset p = photosets.get(position);
            tv.setText(String.format("%s (%d)", p.getTitle(), p.getPhotoCount()));
            
            return result;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        
        authentication = Authentication.getInstance(getApplicationContext(),
                FlickrHelper.getFlickr());
        
        button = (Button) findViewById(R.id.button_login);
        textView = (TextView) findViewById(R.id.text_login);
        
        ((Button) findViewById(R.id.button_templates))
                .setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(),
                        TemplateActivity.class));
            }
        });
        
        ((Button) findViewById(R.id.button_get_photosets))
                .setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                new GetPhotosetsTask().execute();
            }
        });
        
        photosetsAdapter = new Adapter();
        ListView listView = (ListView) findViewById(R.id.listview_photosets);
        listView.setAdapter(photosetsAdapter);
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id)
            {
                showPhotoset(photosets.get(position));
            }
        });
        
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
    
    private void updateState()
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
    
    private void updatePhotosetList(List<Photoset> list)
    {
        photosets.clear();
        if (list != null)
            photosets.addAll(list);
        photosetsAdapter.notifyDataSetChanged();
    }
    
    private void showPhotoset(Photoset photoset)
    {
        Intent intent = new Intent(getApplicationContext(), PhotosetActivity.class);
        intent.putExtra(PhotosetActivity.KEY_PHOTOSET, photoset);
        startActivity(intent);
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
}
