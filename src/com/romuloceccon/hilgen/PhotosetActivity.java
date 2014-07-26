package com.romuloceccon.hilgen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class PhotosetActivity extends Activity
{
    private static final String TAG = "HILGen";
    
    public static final String KEY_PHOTOSET = "photoset";
    
    private static final String KEY_SELECTED_SIZE = "select_size";
    private static final String KEY_SELECTED_TEMPLATE = "selected_template";
    
    private Authentication authentication;
    private String[] sizeLabels;
    private List<Template> templates = new ArrayList<Template>();
    private String currentSize = null;
    private Template currentTemplate = null;
    
    private SharedPreferences prefs;
    
    private Spinner spinnerSize;
    private Spinner spinnerTemplate;
    private Button buttonGetPhotos;
    private TextView textPhotos;
    private TextView textProgress;
    
    private ArrayAdapter<CharSequence> sizesAdapter;
    private ArrayAdapter<Template> templatesAdapter;
    
    private Photoset photoset;
    private Generator generator = null;
    
    private class GetPhotosTask extends AsyncTask<Void, Integer, Generator>
    {
        private Exception error = null;
        
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
            
            return result;
        }
        
        @Override
        protected void onPostExecute(Generator result)
        {
            if (result == null)
                ActivityUtils.showToast(PhotosetActivity.this,
                        getString(R.string.message_get_photos_failed), error);
            
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
        sizeLabels = Generator.getLabels();
        Template.getAll(getApplicationContext(), templates);
        
        prefs = getPreferences(Context.MODE_PRIVATE);
        
        spinnerSize = (Spinner) findViewById(R.id.spinner_size);
        spinnerTemplate = (Spinner) findViewById(R.id.spinner_template);
        buttonGetPhotos = (Button) findViewById(R.id.button_get_photos);
        textPhotos = (TextView) findViewById(R.id.text_photos);
        textProgress = (TextView) findViewById(R.id.text_progress);
        
        sizesAdapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item, sizeLabels);
        spinnerSize.setAdapter(sizesAdapter);
        
        templatesAdapter = new ArrayAdapter<Template>(this,
                android.R.layout.simple_spinner_item, templates);
        spinnerTemplate.setAdapter(templatesAdapter);
        
        spinnerSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v,
                    int position, long id)
            {
                currentSize = sizeLabels[position];
                updatePhotosText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
                currentSize = null;
                updatePhotosText();
            }
        });
        
        spinnerTemplate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id)
            {
                currentTemplate = templates.get(position);
                updatePhotosText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                currentTemplate = null;
                updatePhotosText();
            }
        });
        
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
        
        if (sizeLabels.length > 0)
        {
            currentSize = prefs.getString(KEY_SELECTED_SIZE, null);
            int posSize = -1;
            if (currentSize != null)
            {
                posSize = getSizeLabelPos(currentSize);
            }
            if (posSize == -1)
            {
                posSize = 0;
                currentSize = sizeLabels[0];
            }
            spinnerSize.setSelection(posSize);
        }
        
        if (templates.size() > 0)
        {
            currentTemplate = Template.getById(getApplicationContext(),
                    prefs.getInt(KEY_SELECTED_TEMPLATE, 1));
            int posTemplate = -1;
            if (currentTemplate != null)
            {
                posTemplate = getTemplatePos(currentTemplate);
            }
            if (posTemplate == -1)
            {
                posTemplate = 0;
                currentTemplate = templates.get(0);
            }
            spinnerTemplate.setSelection(posTemplate);
        }
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        
        SharedPreferences.Editor editor = prefs.edit();
        int posSize = spinnerSize.getSelectedItemPosition();
        if (posSize >= 0 && posSize < sizeLabels.length)
            editor.putString(KEY_SELECTED_SIZE, sizeLabels[posSize]);
        else
            editor.remove(KEY_SELECTED_SIZE);
        int posTemplate = spinnerTemplate.getSelectedItemPosition();
        if (posTemplate >= 0 && posTemplate < templates.size())
        {
            editor.putInt(KEY_SELECTED_TEMPLATE, templates.get(posTemplate).getId());
        }
        else
            editor.remove(KEY_SELECTED_TEMPLATE);
        editor.apply();
    }
    
    private void updatePhotosText()
    {
        if (generator == null)
        {
            textPhotos.setText("");
            return;
        }
        
        if (currentSize == null || currentTemplate == null)
        {
            ActivityUtils.showToast(this,
                    getString(R.string.message_invalid_size_or_template));
            return;
        }
        
        String text = generator.build(currentTemplate.getInner(),
                currentSize == null ? sizeLabels[0] : currentSize);
        text = currentTemplate.getOuter().replaceFirst("\\{\\}", text);
        
        textPhotos.setText(text);
        
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.label_clip_data_photo_list), text));
        
        ActivityUtils.showToast(this, getString(R.string.clipboard_text_set));
    }
    
    private void updateProgress(Integer count)
    {
        textProgress.setVisibility(count == null ? View.GONE : View.VISIBLE);
        textProgress.setText(count == null ? "" : getString(R.string.progress_get_photos, count));
    }
    
    private int getSizeLabelPos(String name)
    {
        int len = sizeLabels.length;
        for (int i = 0; i < len; i++)
            if (name.equals(sizeLabels[i]))
                return i;
        return -1;
    }
    
    private int getTemplatePos(Template template)
    {
        int len = templates.size();
        for (int i = 0; i < len; i++)
            if (template.getId() == templates.get(i).getId())
                return i;
        return -1;
    }
}
