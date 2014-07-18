package com.romuloceccon.hilgen;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.RequestContext;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

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
    
    public static synchronized void setOAuth(OAuth oAuth)
    {
        RequestContext requestContext = RequestContext.getRequestContext();
        
        if (oAuth == null)
        {
            requestContext.setOAuth(null);
            return;
        }
        
        OAuthToken oAuthToken = oAuth.getToken();
        User user = oAuth.getUser();
        
        OAuth copy = new OAuth();
        
        OAuthToken oAuthTokenCopy = new OAuthToken(oAuthToken.getOauthToken(),
                oAuthToken.getOauthTokenSecret());
        User userCopy = new User();
        userCopy.setUsername(user.getUsername());
        userCopy.setId(user.getId());
        
        copy.setUser(userCopy);
        copy.setToken(oAuthTokenCopy);
        requestContext.setOAuth(copy);
    }
}
