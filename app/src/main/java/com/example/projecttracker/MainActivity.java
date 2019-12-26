package com.example.projecttracker;

import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AbsListView.OnScrollListener {

    @Override
    protected void onStop() {
        mePlayer.pause();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mePlayer.start();
    }

    //use for connect S3 bucket
    public static final String BUCKET_NAME = "assign3-20190318180838-deployment";
    public static final String IDENTITY_POLLED_ID = "ca-central-1:35fdbbbc-ca6f-46b7-a33f-3a250cf93df6";

    private Button addNewBtn;
    private Button showAllUncompletedBtn;
    private Button aboutAuthorBtn;

    private MyListView mylistview;
    private ListView listview;
    private View addItemButtonView;
    private View longListItemView;

    private DBManager dbManager = null;
    private Cursor cursor = null;

    public static final int MAIN_STATE = 0;
    public static final int ENTER_STATE = 1;
    public static final int CHANGE_STATE = 2;
    public static final long TOW_DAY_MILLIS = 172800000;
    private int id = -1;

    private AlertDialog alert = null;
    private AlertDialog.Builder builder = null;
    private Context myContext = null;

    private MediaPlayer mePlayer;
    private AudioManager aManager;

    private String deleteFileName = "";

    private CognitoCachingCredentialsProvider credentialsProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set audio player
        aManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        mePlayer = MediaPlayer.create(MainActivity.this, R.raw.audio);
        mePlayer.setLooping(true);

        myContext = MainActivity.this;
        addItemButtonView = getLayoutInflater().inflate(R.layout.addnewbutton,null);
        addNewBtn = (Button) addItemButtonView.findViewById(R.id.addNewButton);
        showAllUncompletedBtn = (Button)findViewById(R.id.report_unfinished);
        aboutAuthorBtn = (Button)findViewById(R.id.about_author);

        dbManager = new DBManager(this);

        listview = (ListView)findViewById(R.id.list_item);

        listview.addFooterView(addItemButtonView);

        credentialsProviders();

        dbManager.openDB();
        cursor = dbManager.getAll();
        cursor.moveToFirst();
        int count = cursor.getCount();

        ArrayList<String> projectNumbers = new ArrayList<String>();
        ArrayList<String> courseNames = new ArrayList<String>();
        ArrayList<String> isCompletes = new ArrayList<String>();
        ArrayList<String> dueDates = new ArrayList<String>();

        for(int i=0; i < count; i++){
            projectNumbers.add(cursor.getString(cursor.getColumnIndex("ProjectNum")));
            courseNames.add(cursor.getString(cursor.getColumnIndex("CourseName")));
            isCompletes.add(cursor.getString(cursor.getColumnIndex("Complete")));
            dueDates.add(cursor.getString(cursor.getColumnIndex("DueDate")));
            cursor.moveToNext();
        }

        dbManager.closeDB();

        try {
            showTowDayAlert(projectNumbers, dueDates, isCompletes, count);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        mylistview = new MyListView(this,projectNumbers,courseNames,isCompletes);

        listview.setAdapter(mylistview);
        listview.setOnScrollListener((AbsListView.OnScrollListener) this);
        listview.setOnCreateContextMenuListener(new myDropdownList());

        addNewBtn.setOnClickListener(new addNewItemClick());
        showAllUncompletedBtn.setOnClickListener(new showAllUncompletedClick());
        aboutAuthorBtn.setOnClickListener(new aboutAuthorClick() );
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {

    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {

    }

    public class addNewItemClick implements View.OnClickListener{

        @Override
        public void onClick(View view) {

            Intent i = new Intent();
            i.putExtra("state",ENTER_STATE);
            i.setClass(MainActivity.this,EditWindow.class);
            MainActivity.this.startActivity(i);

        }
    }

    public class aboutAuthorClick implements View.OnClickListener{

        @Override
        public void onClick(View view) {

            String authorsMessage = "Yu Zhu " + "Email: Yuzhu402@uottawa.ca" + "\r\n"
                    + "Yiqi Zhang" + " Email: yzhan603@uottawa.ca";

            builder = new AlertDialog.Builder(myContext);
            alert = builder.setTitle("Authors message: ")
                    .setMessage(authorsMessage)
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    }).create();
            alert.show();

            showAlertDialog(authorsMessage,"Authors message: ");

        }
    }

    private class showAllUncompletedClick implements View.OnClickListener{

        @Override
        public void onClick(View view) {

            dbManager.openDB();
            cursor = dbManager.getAll();
            cursor.moveToFirst();
            int count = cursor.getCount();
            String tempMessage = "";
            int tempCount = 0;
            String itemMessage = "";

            for(int i=0; i < count; i++){

                if(cursor.getString(cursor.getColumnIndex("Complete")).equals("unfinished")){
                    String temp = "Item " + (i+1) + " ," + "Project Number: " + cursor.getString(cursor.getColumnIndex("ProjectNum")) + "\r\n";
                    itemMessage = itemMessage + temp;
                    tempCount++;
                }
                cursor.moveToNext();
            }

            tempMessage = "You have " + (tempCount) + " project(s) uncompleted." + "\r\n" + itemMessage;

            showAlertDialog(tempMessage,"Uncompleted Projects: ");

            dbManager.closeDB();

        }

    }

    public class myDropdownList implements View.OnCreateContextMenuListener{

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

            final AdapterView.AdapterContextMenuInfo context = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
            contextMenu.setHeaderTitle("");
            contextMenu.add(0,0,0,"Change Item");
            contextMenu.add(0,1,0,"Delete Item");
            contextMenu.add(0,2,0,"Quick Report");
            //contextMenu.add(0,2,0,"Quick View");

        }
    }

    public boolean onContextItemSelected (MenuItem menuItem){
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)menuItem.getMenuInfo();
        dbManager.openDB();
        switch (menuItem.getItemId()){
            case 0:
                try {
                    //Change Item case
                    cursor.moveToPosition(menuInfo.position);
                    Intent i = new Intent();
                    i.putExtra("Id", cursor.getString(cursor.getColumnIndex("_Id")));
                    i.putExtra("ProNum", cursor.getString(cursor.getColumnIndex("ProjectNum")));
                    i.putExtra("CouName", cursor.getString(cursor.getColumnIndex("CourseName")));
                    i.putExtra("DueDate", cursor.getString(cursor.getColumnIndex("DueDate")));
                    i.putExtra("Complete", cursor.getString(cursor.getColumnIndex("Complete")));
                    i.putExtra("menuPosition",menuInfo.position);
                    i.putExtra("state",CHANGE_STATE);
                    i.setClass(MainActivity.this,EditWindow.class);
                    MainActivity.this.startActivity(i);
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case 1:
                try{
                    //Delete Item case
                    cursor.moveToPosition(menuInfo.position);
                    deleteFileName = cursor.getString(cursor.getColumnIndex("ProjectNum"));
                    int i = dbManager.delete(Long.parseLong(cursor.getString(cursor.getColumnIndex("_Id"))));
                    deleteFileInBucket();
                    refreshListView();
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case 2:
                try{
                    //Quick View
                    cursor.moveToPosition(menuInfo.position);
                    Intent i = new Intent();
                    String tempProjectNum = cursor.getString(cursor.getColumnIndex("ProjectNum"));
                    String tempCourseName = cursor.getString(cursor.getColumnIndex("CourseName"));
                    String tempDueDate = cursor.getString(cursor.getColumnIndex("DueDate"));
                    String tempIsComplete = cursor.getString(cursor.getColumnIndex("Complete"));
                    String tempMessage = "Project Number: " + tempProjectNum + "\r\n"
                                       + "Course Name: " + tempCourseName + "\r\n"
                                       + "Due Date: " + tempDueDate + "\r\n"
                                       + "Statue: " + tempIsComplete;
                    showAlertDialog(tempMessage,"Project Statue: ");
                }catch (Exception e){
                    e.printStackTrace();
                }
            default:;
        }
        dbManager.closeDB();
        return  super.onContextItemSelected(menuItem);

    }

    //This method is to show two day alert
    private void showTowDayAlert(List<String> projectNumbers, List<String> dueDates, List<String> isCompletes, int count) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String currentTime = format.format(Calendar.getInstance().getTime());
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(currentTime));

        long currentMillis = currentCalendar.getTimeInMillis();
        String finalMessage = "";
        int tempCount = 0;
        for( int i=0; i < count; i++){
            String dueDate = dueDates.get(i);
            Calendar dueCalendar = Calendar.getInstance();
            dueCalendar.setTime( new SimpleDateFormat("yyyy-MM-dd").parse(dueDate));
            long dueDateMillis = dueCalendar.getTimeInMillis();
            if((dueDateMillis - currentMillis <= TOW_DAY_MILLIS) && (dueDateMillis - currentMillis > 0 ) && (isCompletes.get(i).equals("unfinished"))){
                tempCount++;
                String temp = "Item " + (i+1) + ", " + "Project Number: " + projectNumbers.get(i) + "\r\n";
                finalMessage = finalMessage + temp;
            }

        }
        finalMessage = "You have " + (tempCount) + " Project(s) due date within two days. \r\n" + finalMessage;
        showAlertDialog(finalMessage,"Urgent Project: ");

    }

    private void showAlertDialog(String mes, String title){
        builder = new AlertDialog.Builder(myContext);
        alert = builder.setTitle(title)
                .setMessage(mes)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).create();
        alert.show();
    }

    private void credentialsProviders(){
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                IDENTITY_POLLED_ID, // Identity pool ID
                Regions.CA_CENTRAL_1 // Region
        );
    }

    private void deleteFileInBucket(){

        new Thread(new Runnable() {
            public void run(){

                String filePath = "amplify-appsync-files/" + deleteFileName + ".txt";
                AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.US_EAST_1));
                s3Client.deleteObject(BUCKET_NAME,filePath);


        }
    }).start();

    }

    private void refreshListView(){

        dbManager.openDB();
        cursor = dbManager.getAll();
        cursor.moveToFirst();
        int count = cursor.getCount();

        ArrayList<String> projectNumbers = new ArrayList<String>();
        ArrayList<String> courseNames = new ArrayList<String>();
        ArrayList<String> isCompletes = new ArrayList<String>();
        ArrayList<String> dueDates = new ArrayList<String>();

        for(int i=0; i < count; i++){
            projectNumbers.add(cursor.getString(cursor.getColumnIndex("ProjectNum")));
            courseNames.add(cursor.getString(cursor.getColumnIndex("CourseName")));
            isCompletes.add(cursor.getString(cursor.getColumnIndex("Complete")));
            dueDates.add(cursor.getString(cursor.getColumnIndex("DueDate")));
            cursor.moveToNext();
        }

        dbManager.closeDB();

        mylistview = new MyListView(this,projectNumbers,courseNames,isCompletes);

        listview.setAdapter(mylistview);
        listview.setOnScrollListener((AbsListView.OnScrollListener) this);
        listview.setOnCreateContextMenuListener(new myDropdownList());
    }

}
