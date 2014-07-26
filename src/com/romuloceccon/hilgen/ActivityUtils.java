package com.romuloceccon.hilgen;

import android.content.Context;
import android.widget.Toast;

public class ActivityUtils
{
    public static void showToast(Context ctx, CharSequence msg)
    {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }
    
    public static void showToast(Context ctx, CharSequence msg, Exception e)
    {
        if (e == null)
        {
            showToast(ctx, msg);
            return;
        }
        
        String excMsg = msg + ": (" + e.getClass().getName() + ") " + e.getMessage();
        showToast(ctx, excMsg);
    }
}
