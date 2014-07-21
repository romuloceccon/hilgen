package com.romuloceccon.hilgen;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.Size;

public class Generator
{
    private static final String TAG = "HILGen";
    
    public static class PhotoSizes
    {
        private final Photo photo;
        private final Collection<Size> sizes;
        
        public PhotoSizes(Photo photo, Collection<Size> sizes)
        {
            this.photo = photo;
            this.sizes = sizes;
        }
        
        public Photo getPhoto()
        {
            return photo;
        }
        
        public Collection<Size> getSizes()
        {
            return sizes;
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
    
    private static class ImageInfo
    {
        public String title;
        public String url;
        public String source;
        public String width;
        public String height;
    }
    
    private static class UrlMethod
    {
        public int code;
        public java.lang.reflect.Method method;
    }
    
    private static final Class<?>[] urlMethodTypes = new Class[] { };
    private static final Map<String, UrlMethod> urlMethods;
    private static final List<String> labels;
    
    private UrlMethod urlMethod;
    
    public Generator(String sizeLabel)
    {
        urlMethod = urlMethods.containsKey(sizeLabel) ?
                urlMethods.get(sizeLabel) : null;
    }
    
    public String build(String templateString,
            Collection<PhotoSizes> photoSizesCollection)
    {
        Template template = new Template("\\$(\\w+)\\{\\{(.*?)\\}\\}");
        StringBuilder builder = new StringBuilder();
        
        for (PhotoSizes p: photoSizesCollection)
        {
            template.clearSubstitutions();
            
            ImageInfo info = getImageInfo(p);
            
            template.setSubstitution("T", info.title);
            template.setSubstitution("U", info.url);
            template.setSubstitution("S", info.source);
            template.setSubstitution("W", info.width);
            template.setSubstitution("H", info.height);
            
            builder.append(template.substitute(templateString));
        }
        
        return builder.toString();
    }
    
    private ImageInfo getImageInfo(PhotoSizes photoSizes)
    {
        ImageInfo result = new ImageInfo();
        
        Photo p = photoSizes.photo;
        result.title = p.getTitle();
        result.url = p.getUrl();
        
        Collection<Size> sizes = photoSizes.sizes;
        Size s = sizes != null ? findSize(sizes) : null;
        
        if (s != null)
        {
            result.source = s.getSource();
            result.width = String.valueOf(s.getWidth());
            result.height = String.valueOf(s.getHeight());
        }
        else
        {
            try
            {
                result.source = (String) urlMethod.method.invoke(p);
            }
            catch (IllegalArgumentException e)
            {
                Log.wtf(TAG, e);
            }
            catch (IllegalAccessException e)
            {
                Log.wtf(TAG, e);
            }
            catch (InvocationTargetException e)
            {
                Log.wtf(TAG, e);
            }
        }
        
        return result;
    }
    
    public static List<String> getLabels()
    {
        return labels;
    }
    
    private Size findSize(Collection<Size> sizes)
    {
        for (Size s: sizes)
            if (s.getLabel() == urlMethod.code)
                return s;
        return null;
    }
    
    private static void addSize(String sizeLabel, int code,
            String urlMethodName)
    {
        UrlMethod u = new UrlMethod();
        
        try
        {
            u.method = com.googlecode.flickrjandroid.photos.Photo
                    .class.getMethod(urlMethodName, urlMethodTypes);
        }
        catch (NoSuchMethodException e)
        {
            u.method = null;
        }
        
        u.code = code;
        urlMethods.put(sizeLabel, u);
        labels.add(sizeLabel);
    }
    
    static
    {
        urlMethods = new HashMap<String, UrlMethod>();
        labels = new ArrayList<String>();
        
        addSize("Square", Size.SQUARE, "getSquareUrl");
        addSize("Large Square", Size.LARGE_SQUARE, "getLarge SquareUrl");
        addSize("Thumbnail", Size.THUMB, "getThumbnailUrl");
        addSize("Small", Size.SMALL, "getSmallUrl");
        addSize("Small 320", Size.SMALL_320, "getSmall 320Url");
        addSize("Medium", Size.MEDIUM, "getMediumUrl");
        addSize("Medium 640", Size.MEDIUM_640, "getMedium 640Url");
        addSize("Medium 800", Size.MEDIUM_800, "getMedium 800Url");
        addSize("Large", Size.LARGE, "getLargeUrl");
        addSize("Large 1600", Size.LARGE_1600, "getLarge 1600Url");
        addSize("Large 2048", Size.LARGE_2048, "getLarge 2048Url");
        addSize("Original", Size.ORIGINAL, "getOriginalUrl");
    }
}
