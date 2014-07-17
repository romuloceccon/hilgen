package com.romuloceccon.hilgen;

import com.googlecode.flickrjandroid.Flickr;

public class FlickrHelper
{
    private static Flickr instance = null;
    
    public static synchronized Flickr getFlickr()
    {
        if (instance == null)
            instance = new Flickr("5448446a4bf6e01575a04886bb481d61",
                    "7230d66b6e1ed0c6");
        
        return instance;
    }
}
