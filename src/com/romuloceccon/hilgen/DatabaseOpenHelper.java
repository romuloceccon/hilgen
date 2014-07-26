package com.romuloceccon.hilgen;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper
{
    private static final int VERSION = 1;
    
    private static final String CREATE_TEMPLATES =
            "CREATE TABLE \"templates\" (" +
            "\"id\" INTEGER PRIMARY KEY, \"name\" TEXT, " +
            "\"outer\" TEXT, \"inner\" TEXT);";

    private Context context;
    
    public DatabaseOpenHelper(Context context)
    {
        super(context, "hilgen.db", null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(CREATE_TEMPLATES);
        
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("name", "default");
        values.put("outer", context.getString(R.string.template_default_outer));
        values.put("inner", context.getString(R.string.template_default_inner));
        
        db.insertOrThrow("templates", null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }
}
