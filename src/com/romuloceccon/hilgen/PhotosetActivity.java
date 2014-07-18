package com.romuloceccon.hilgen;

import com.googlecode.flickrjandroid.photosets.Photoset;

import android.app.Activity;
import android.os.Bundle;

public class PhotosetActivity extends Activity
{
    public static final String KEY_PHOTOSET = "photoset";
    
    private Photoset photoset = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photoset);
        
        Bundle extras = getIntent().getExtras();
        photoset = (Photoset) extras.getSerializable(KEY_PHOTOSET);
        
        if (photoset != null)
        {
            setTitle(getString(R.string.title_activity_photoset,
                    photoset.getId(), photoset.getTitle()));
        }
    }
}
