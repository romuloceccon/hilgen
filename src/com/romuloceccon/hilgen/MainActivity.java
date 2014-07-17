package com.romuloceccon.hilgen;

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
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        authentication = Authentication.getInstance(getApplicationContext(),
                FlickrHelper.getFlickr());
        
        button = (Button) findViewById(R.id.button_action);
        textView = (TextView) findViewById(R.id.text_state);
        
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
}
