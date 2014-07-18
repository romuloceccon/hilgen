package com.romuloceccon.hilgen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;

import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photos.PhotoUrl;
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
import android.widget.TextView;
import android.widget.Toast;

public class PhotosetActivity extends Activity
{
    private static final String TAG = "HILGen";
    
    public static final String KEY_PHOTOSET = "photoset";
    
    private Button buttonGetPhotos;
    private TextView textPhotos;
    
    private Photoset photoset;
    
    private class GetPhotosTask extends AsyncTask<Void, Integer, List<Photo>>
    {
        @Override
        protected List<Photo> doInBackground(Void... params)
        {
            List<Photo> result = new ArrayList<Photo>();
            PhotosetsInterface intf = FlickrHelper.getFlickr().getPhotosetsInterface();
            
            PhotoList photos;
            int page = 1;
            final int perPage = 10;
            
            try
            {
                do
                {
                    try
                    {
                        photos = intf.getPhotos(photoset.getId(), perPage, page).getPhotoList();
                    }
                    catch (FlickrException e)
                    {
                        if (e.getErrorCode().compareTo("1") == 0)
                            break;
                        throw e;
                    }
                    
                    for (Photo p: photos)
                        result.add(p);
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
        protected void onPostExecute(List<Photo> result)
        {
            if (result == null)
                showToast(getString(R.string.message_get_photos_failed));
            
            updatePhotosText(result);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photoset);
        
        buttonGetPhotos = (Button) findViewById(R.id.button_get_photos);
        textPhotos = (TextView) findViewById(R.id.text_photos);
        
        Bundle extras = getIntent().getExtras();
        photoset = (Photoset) extras.getSerializable(KEY_PHOTOSET);
        
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
    
    private void updatePhotosText(List<Photo> photos)
    {
        if (photos == null)
        {
            textPhotos.setText("");
            return;
        }
        
        StringBuilder builder = new StringBuilder();
        
        for (Photo p: photos)
        {
            Size size = p.getMediumSize();
            
            if (size != null)
                builder.append(getString(R.string.template_html_img,
                        getPhotoPageUrl(p),
                        p.getTitle(),
                        p.getMediumUrl(),
                        size.getWidth(),
                        size.getHeight()));
            else
                builder.append(getString(R.string.template_html_img_without_size,
                        getPhotoPageUrl(p),
                        p.getTitle(),
                        p.getMediumUrl()));
        }
        
        String text = builder.toString();
        
        textPhotos.setText(text);
        
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
        clipboard.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.label_clip_data_photo_list), text));
        
        showToast(getString(R.string.clipboard_text_set));
    }
    
    private String getPhotoPageUrl(Photo p)
    {
        Collection<PhotoUrl> urls = p.getUrls();
        
        if (p.getUrl() == null)
            Log.w(TAG, "NO URL!");
        
        if (urls == null)
        {
            Log.w(TAG, "NO PHOTO URLS!");
            return null;
        }
        
        for (PhotoUrl u: urls)
        {
            if (u.getType().compareTo("photopage") == 0)
                return u.getUrl();
        }
        
        return null;
    }
    
    private void showToast(CharSequence msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
