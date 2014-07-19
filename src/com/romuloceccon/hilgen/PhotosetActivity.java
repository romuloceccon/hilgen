package com.romuloceccon.hilgen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.widget.TextView;
import android.widget.Toast;

public class PhotosetActivity extends Activity
{
    private static final String TAG = "HILGen";
    
    public static final String KEY_PHOTOSET = "photoset";
    public static final String KEY_TEMPLATE = "template";
    
    private Authentication authentication;
    
    private Button buttonGetPhotos;
    private TextView textPhotos;
    private TextView textProgress;
    
    private Photoset photoset;
    private String templateString;
    
    private class GetPhotosTask extends AsyncTask<Void, Integer, List<Photo>>
    {
        @Override
        protected List<Photo> doInBackground(Void... params)
        {
            OAuth oAuth = authentication.getOAuth();
            if (oAuth == null)
                return null;
            
            FlickrHelper.setOAuth(oAuth);
            
            List<Photo> result = new ArrayList<Photo>();
            
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
                        
                        try
                        {
                            p.setSizes(photosIntf.getSizes(t.getId()));
                        }
                        catch (FlickrException e)
                        {
                            // getSizes fails for private photos
                            if (e.getErrorCode().compareTo("1") != 0)
                                throw e;
                        }
                        
                        result.add(p);
                        
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
        protected void onPostExecute(List<Photo> result)
        {
            if (result == null)
                showToast(getString(R.string.message_get_photos_failed));
            
            updatePhotosText(result);
            updateProgress(null);
        }
        
        @Override
        protected void onProgressUpdate(Integer... values)
        {
            updateProgress(values[0]);
        }
    }
    
    private static class Template
    {
        private Pattern pattern;
        private Map<String, String> substitutions;
        
        public Template(String regex)
        {
            pattern = Pattern.compile(regex);
            substitutions = new HashMap<String, String>();
        }
        
        public void clearSubstitutions()
        {
            substitutions.clear();
        }
        
        public void setSubstitution(String name, String value)
        {
            substitutions.put(name, value);
        }
        
        public String substitute(String input)
        {
            StringBuilder sb = new StringBuilder();
            Matcher matcher = pattern.matcher(input);
            int pos = 0;
            
            while (matcher.find())
            {
                sb.append(input.substring(pos, matcher.start()));
                
                String key = matcher.group(1);
                String text = matcher.group(2);
                
                if (substitutions.containsKey(key))
                {
                    String value = substitutions.get(key);
                    if (value != null)
                        sb.append(text.replaceFirst("\\{\\}", value));
                }
                else
                    sb.append(matcher.group(0));
                
                pos = matcher.end();
            }
            
            sb.append(input.substring(pos));
            
            return sb.toString();
        }
    }
    
    private static class ImageSize
    {
        public String source;
        public String width;
        public String height;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photoset);
        
        authentication = Authentication.getInstance(getApplicationContext(),
                FlickrHelper.getFlickr());
        
        buttonGetPhotos = (Button) findViewById(R.id.button_get_photos);
        textPhotos = (TextView) findViewById(R.id.text_photos);
        textProgress = (TextView) findViewById(R.id.text_progress);
        
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
    
    private void updatePhotosText(List<Photo> photos)
    {
        if (photos == null)
        {
            textPhotos.setText("");
            return;
        }
        
        Template template = new Template("\\$(\\w+)\\{\\{(.*?)\\}\\}");
        StringBuilder builder = new StringBuilder();
        
        for (Photo p: photos)
        {
            template.clearSubstitutions();
            
            ImageSize size = getImageSize(p);
            
            template.setSubstitution("T", p.getTitle());
            template.setSubstitution("U", p.getUrl());
            template.setSubstitution("S", size.source);
            template.setSubstitution("W", size.width);
            template.setSubstitution("H", size.height);
            
            builder.append(template.substitute(templateString));
        }
        
        String text = builder.toString();
        
        textPhotos.setText(text);
        
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.label_clip_data_photo_list), text));
        
        showToast(getString(R.string.clipboard_text_set));
    }
    
    private ImageSize getImageSize(Photo p)
    {
        if (isChecked(R.id.radio_size_square))
            return getImageSize(p.getSquareSize());
        else if (isChecked(R.id.radio_size_large_square))
            return getImageSize(p.getLargeSquareUrl(), p.getLargeSquareSize());
        else if (isChecked(R.id.radio_size_thumbnail))
            return getImageSize(p.getThumbnailUrl(), p.getThumbnailSize());
        else if (isChecked(R.id.radio_size_small))
            return getImageSize(p.getSmallUrl(), p.getSmallSize());
        else if (isChecked(R.id.radio_size_small_320))
            return getImageSize(p.getSmall320Url(), null);
        else if (isChecked(R.id.radio_size_medium))
            return getImageSize(p.getMediumUrl(), p.getMediumSize());
        else if (isChecked(R.id.radio_size_medium_640))
            return getImageSize(p.getMedium640Url(), null);
        else if (isChecked(R.id.radio_size_medium_800))
            return getImageSize(p.getMedium800Url(), null);
        else if (isChecked(R.id.radio_size_large))
            return getImageSize(p.getLargeUrl(), p.getLargeSize());
        else if (isChecked(R.id.radio_size_large_1600))
            return getImageSize(p.getLarge1600Url(), null);
        else if (isChecked(R.id.radio_size_large_2048))
            return getImageSize(p.getLarge2048Url(), null);
        else if (isChecked(R.id.radio_size_original))
            return getImageSize(p.getOriginalSize());
        
        return getImageSize(null, null);
    }
        
    private ImageSize getImageSize(Size s)
    {
        ImageSize result = new ImageSize();
        
        if (s != null)
        {
            result.source = s.getSource();
            result.width = String.valueOf(s.getWidth());
            result.height = String.valueOf(s.getHeight());
        }
        
        return result;
    }
    
    private ImageSize getImageSize(String source, Size size)
    {
        ImageSize result = new ImageSize();
        
        result.source = source;
        
        if (size != null)
        {
            result.width = String.valueOf(size.getWidth());
            result.height = String.valueOf(size.getHeight());
        }
        
        return result;
    }
    
    private boolean isChecked(int id)
    {
        RadioButton button = (RadioButton) findViewById(id);
        return button.isChecked();
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
