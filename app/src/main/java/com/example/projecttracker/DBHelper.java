package com.example.projecttracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private String NotePadTabelName = "Record";
    private Context myContext = null;
    private String SQL = "create table if not exists " + NotePadTabelName +
            "(_Id integer primary key autoincrement, " +
            "ProjectNum varchar," +
            "CourseName text," +
            "Complete varchar," +
            "DueDate varchar)";

    public DBHelper (Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub

        //Create the DB table
        db.execSQL(SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }

}
