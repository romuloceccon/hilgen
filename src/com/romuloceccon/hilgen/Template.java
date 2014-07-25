package com.romuloceccon.hilgen;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Template
{
    private static final String TABLE_NAME = "templates";
    
    public static void getAll(Context ctx, List<Template> list)
    {
        DatabaseOpenHelper helper = new DatabaseOpenHelper(ctx);
        SQLiteDatabase sqlite = helper.getReadableDatabase();
        try
        {
            String[] columns = { "id", "name", "outer", "inner" };
            Cursor c = sqlite.query(TABLE_NAME, columns, null, null, null, null, "id");
            try
            {
                c.moveToFirst();
                list.clear();
                
                while (!c.isAfterLast())
                {
                    Template t = new Template();
                    t.id = c.getLong(c.getColumnIndexOrThrow("id"));
                    t.name = getStringValue(c, "name");
                    t.outer = getStringValue(c, "outer");
                    t.inner = getStringValue(c, "inner");
                    list.add(t);
                    
                    c.moveToNext();
                }
            }
            finally
            {
                c.close();
            }
        }
        finally
        {
            sqlite.close();
        }
    }
    
    public static void delete(Context ctx, List<Template> list, Template item)
    {
        if (!list.remove(item) || item.id == null)
            return;
        
        DatabaseOpenHelper helper = new DatabaseOpenHelper(ctx);
        SQLiteDatabase sqlite = helper.getWritableDatabase();

        sqlite.beginTransaction();
        try
        {
            sqlite.delete(TABLE_NAME, "id = ?",
                    new String[] { String.valueOf(item.id) });
            sqlite.setTransactionSuccessful();
        }
        finally
        {
            sqlite.endTransaction();
        }
    }
    
    private static String getStringValue(Cursor c, String columnName)
    {
        int i = c.getColumnIndexOrThrow(columnName);
        if (c.isNull(i))
            return "";
        else
            return c.getString(i);
    }
    
    private Long id;
    private String name;
    private String outer;
    private String inner;
    
    @Override
    public String toString()
    {
        return name;
    }
    
    public boolean isNew()
    {
        return id == null;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String value)
    {
        name = value;
    }
    
    public String getOuter()
    {
        return outer;
    }
    
    public void setOuter(String value)
    {
        outer = value;
    }
    
    public String getInner()
    {
        return inner;
    }
    
    public void setInner(String value)
    {
        inner = value;
    }
    
    public void save(Context ctx, List<Template> list)
    {
        DatabaseOpenHelper helper = new DatabaseOpenHelper(ctx);
        SQLiteDatabase sqlite = helper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("outer", outer);
        values.put("inner", inner);
        
        sqlite.beginTransaction();
        try
        {
            if (id == null)
                id = sqlite.insertOrThrow(TABLE_NAME, null, values);
            else
                sqlite.update(TABLE_NAME, values, "id = ?",
                        new String[] { String.valueOf(id) });
            sqlite.setTransactionSuccessful();
        }
        finally
        {
            sqlite.endTransaction();
        }
        
        if (!list.contains(this))
            list.add(this);
    }
}
