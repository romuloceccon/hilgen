package com.romuloceccon.hilgen;

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

    public DatabaseOpenHelper(Context context)
    {
        super(context, "hilgen.db", null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(CREATE_TEMPLATES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }
}
