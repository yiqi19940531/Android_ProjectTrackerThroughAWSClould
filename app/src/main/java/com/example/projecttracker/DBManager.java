package com.example.projecttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBManager {

    private SQLiteDatabase mySQLiteDatabase = null;
    private DBHelper dbHelper = null;
    private String DBName = "Notepad7.db";

    private Context myContext = null;

    public DBManager (Context context){
        myContext = context;
    }

    //close DB method
    public void closeDB(){
        mySQLiteDatabase.close();
        dbHelper.close();
    }

    //open DB method
    public void openDB(){
        try{
            dbHelper = new DBHelper(myContext,DBName,null,1);
            if(dbHelper == null){
                Log.v("Error: ", "Can not open DB");
                return;
            }
            mySQLiteDatabase = dbHelper.getWritableDatabase();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    //Insert data to Table
    public long insert (String projectNumber,String courseName,String isComplete,String dueDate){
        long tag = -1;
        try{
            ContentValues contentvalue = new ContentValues();
            contentvalue.put("ProjectNum", projectNumber);
            contentvalue.put("CourseName", courseName);
            contentvalue.put("Complete",isComplete);
            contentvalue.put("DueDate", dueDate);
            tag = mySQLiteDatabase.insert("Record",null,contentvalue);
        }catch (Exception e){
            tag = -1;
            e.printStackTrace();
        }
        return tag;
    }

    //Delete data in Table
    public int delete(long id){
        int tag = 0;
        try{
            tag = mySQLiteDatabase.delete("Record","_Id=?", new String[] {id + ""});

        }catch (Exception e){
            e.printStackTrace();
            tag = -1;
        }
        return tag;
    }

    //Update data in Table
    public int update(int id,String projectNumber,String courseName,String isComplete,String dueDate){
        int tag = 0;
        try{
            ContentValues contentvalue = new ContentValues();
            contentvalue.put("ProjectNum", projectNumber);
            contentvalue.put("CourseName", courseName);
            contentvalue.put("Complete",isComplete);
            contentvalue.put("DueDate", dueDate);
            tag = mySQLiteDatabase.update("Record", contentvalue, "_Id=?",new String [] {id + ""});
        }catch (Exception e){
            e.printStackTrace();
            tag = -1;
        }
        return tag;
    }

    //Get all Nodes
    public Cursor getAll(){
        Cursor c = null;
        try{
            String SQL = "select * from record";
            c = mySQLiteDatabase.rawQuery(SQL,null);
        }catch (Exception e){
            e.printStackTrace();
            c = null;
        }
        return c;
    }

    //Get note by Id
    public Cursor getById(int id){
        Cursor c = null;
        try{
            String SQL = "select * from record where _id='" + id +"'";
            c = mySQLiteDatabase.rawQuery(SQL,null);
        }catch (Exception e){
            e.printStackTrace();
            c = null;
        }
        return c;
    }

}
