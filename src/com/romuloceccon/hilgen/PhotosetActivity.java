package com.romuloceccon.hilgen;

import java.io.IOException;
import java.util.Collection;

import org.json.JSONException;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photos.PhotosInterface;
import com.googlecode.flickrjandroid.photos.Size;
import com.googlecode.flickrjandroid.photosets.Photoset;
import com.googlecode.flickrjandroid.photosets.PhotosetsInterface;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class PhotosetActivity extends Activity
{
    private static final String TAG = "HILGen";
    
    public static final String KEY_PHOTOSET = "photoset";
    public static final String KEY_TEMPLATE = "template";
    
    private Authentication authentication;
    
    private RadioGroup radioSize;
    private Button buttonGetPhotos;
    private TextView textPhotos;
    private TextView textProgress;
    
    private Photoset photoset;
    private String templateString;
    private Generator generator = null;
    
    private class GetPhotosTask extends AsyncTask<Void, Integer, Generator>
    {
        @Override
        protected Generator doInBackground(Void... params)
        {
            OAuth oAuth = authentication.getOAuth();
            if (oAuth == null)
                return null;
            
            FlickrHelper.setOAuth(oAuth);
            
            Generator result = new Generator();
            
            Flickr f = FlickrHelper.getFlickr();
            PhotosetsInterface photosetsIntf = f.getPhotosetsInterface();
            PhotosInterface photosIntf = f.getPhotosInterface();
            
            PhotoList photos;
            int page = 1;
            int count = 0;
            final int perPage = 10;
            
            try
            {
                do
                {
                    try
                    {
                        photos = photosetsIntf.getPhotos(photoset.getId(), perPage, page).getPhotoList();
                    }
                    catch (FlickrException e)
                    {
                        if (e.getErrorCode().compareTo("1") == 0)
                            break;
                        throw e;
                    }
                    
                    for (Photo t: photos)
                    {
                        Photo p = photosIntf.getPhoto(t.getId());
                        Collection<Size> s = null;
                        
                        try
                        {
                            s = photosIntf.getSizes(t.getId());
                        }
                        catch (FlickrException e)
                        {
                            // getSizes fails for private photos
                            if (e.getErrorCode().compareTo("1") != 0)
                                throw e;
                        }
                        
                        result.addPhotoSizes(p, s);
                        
                        publishProgress(++count);
                    }
                    
                    page += 1;
                } while (photos.size() >= perPage);
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
            catch (JSONException e)
            {
                Log.w(TAG, e);
                return null;
            }
            
            return result;
        }
        
        @Override
        protected void onPostExecute(Generator result)
        {
            if (result == null)
                showToast(getString(R.string.message_get_photos_failed));
            
            generator = result;
            updateProgress(null);
            updatePhotosText();
        }
        
        @Override
        protected void onProgressUpdate(Integer... values)
        {
            updateProgress(values[0]);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photoset);
        
        authentication = Authentication.getInstance(getApplicationContext(),
                FlickrHelper.getFlickr());
        
        radioSize = (RadioGroup) findViewById(R.id.radio_size);
        buttonGetPhotos = (Button) findViewById(R.id.button_get_photos);
        textPhotos = (TextView) findViewById(R.id.text_photos);
        textProgress = (TextView) findViewById(R.id.text_progress);
        
        for (String s: Generator.getLabels())
        {
            final RadioButton rb = new RadioButton(this);
            rb.setText(s);
            radioSize.addView(rb);
        }
        
        radioSize.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                updatePhotosText();
            }
        });
        
        Bundle extras = getIntent().getExtras();
        photoset = (Photoset) extras.getSerializable(KEY_PHOTOSET);
        templateString = extras.getString(KEY_TEMPLATE);
        
        if (photoset != null)
        {
            setTitle(getString(R.string.title_activity_photoset,
                    photoset.getId(), photoset.getTitle()));
            
            buttonGetPhotos.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    new GetPhotosTask().execute();
                }
            });
        }
    }
    
    private void updatePhotosText()
    {
        if (generator == null)
        {
            textPhotos.setText("");
            return;
        }
        
        String text = generator.build(templateString, getSelectedSizeName());
        
        textPhotos.setText(text);
        
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.label_clip_data_photo_list), text));
        
        showToast(getString(R.string.clipboard_text_set));
    }
    
    private String getSelectedSizeName()
    {
        int rbId = radioSize.getCheckedRadioButtonId();
        RadioButton rb = (RadioButton) findViewById(rbId);
        if (rb == null && radioSize.getChildCount() > 0)
            rb = (RadioButton) radioSize.getChildAt(0);
        if (rb == null)
            return null;
        return rb.getText().toString();
    }
    
    private void updateProgress(Integer count)
    {
        textProgress.setVisibility(count == null ? View.GONE : View.VISIBLE);
        textProgress.setText(count == null ? "" : getString(R.string.progress_get_photos, count));
    }
    
    private void showToast(CharSequence msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
