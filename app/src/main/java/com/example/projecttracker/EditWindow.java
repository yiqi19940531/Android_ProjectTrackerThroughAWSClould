package com.example.projecttracker;

import android.app.DatePickerDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Thread.sleep;

public class EditWindow extends AppCompatActivity {

    public static final int MAIN_STATE = 0;
    public static final int ENTER_STATE = 1;
    public static final int CHANGE_STATE = 2;
    private int state = -1;
    public static final String BUCKET_NAME = "assign3-20190318180838-deployment";
    public static final String IDENTITY_POLLED_ID = "ca-central-1:35fdbbbc-ca6f-46b7-a33f-3a250cf93df6";

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        super.addContentView(view, params);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mePlayer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mePlayer.pause();
    }

    private String id = "";
    private String projectNumber = "";
    private String courseName = "";
    private String courseNumber = "";
    private String instructName = "";
    private String projectDescription = "";
    private String dueDate = "";
    private String isComplete = "";
    private int menuPosition = 0;
    private Calendar calendar;
    private boolean isFileExist;
    private String loadedContextFromS3 = "";

    private Button saveBtn;
    private Button completeBtn;
    private Button selectDate;
    private EditText project_number;
    private EditText course_name;
    private EditText course_number;
    private EditText instruct_name;
    private EditText project_description;
    private EditText showSelectDate;

    private DBManager DBmanager = null;
    private Cursor cursor = null;

    private MediaPlayer mePlayer;
    private AudioManager aManager;
    private Context mContext;

    private CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_window);
        inti();
    }

    public class saveButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {

            projectNumber = project_number.getText().toString();

            if(state == ENTER_STATE) {
                if (!checkProjectNumberUniqueness(projectNumber)) {
                    Toast.makeText(getApplicationContext(), "Project Number Already used, Please change!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            courseName = course_name.getText().toString();
            courseNumber = course_number.getText().toString();
            instructName = instruct_name.getText().toString();
            projectDescription = project_description.getText().toString();

            try{
                DBmanager.openDB();
                if(state == ENTER_STATE){
                    DBmanager.insert(projectNumber,courseName,isComplete,dueDate);
                }
                if(state == CHANGE_STATE){
                    DBmanager.update(Integer.parseInt(id),projectNumber,courseName,isComplete,dueDate);
                }
                DBmanager.closeDB();
            }catch (Exception e){
                e.printStackTrace();
            }

            saveDataToLocalFile();
            uploadFileToS3(projectNumber);
            Intent intent = new Intent();
            intent.setClass(EditWindow.this,MainActivity.class);
            EditWindow.this.startActivity(intent);


        }
    }

    public class selectDateClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            DatePickerDialog dialog = new DatePickerDialog(EditWindow.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            calendar.set(Calendar.YEAR, year);
                            calendar.set(Calendar.MONTH, month);
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            dueDate = sdf.format(calendar.getTime());
                            showSelectDate.setText(dueDate);
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        }
    }

    public class completeButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            setCompleteBtn(isComplete);
        }
    }

    private void inti(){

        aManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        mePlayer = MediaPlayer.create(EditWindow.this, R.raw.audio2);
        mePlayer.setLooping(true);

        project_number = (EditText)findViewById(R.id.projectnum);
        course_name = (EditText)findViewById(R.id.coursename);
        course_number = (EditText)findViewById(R.id.course_number);
        instruct_name = (EditText)findViewById(R.id.instructor_name);
        project_description = (EditText)findViewById(R.id.project_description);
        saveBtn = (Button)findViewById(R.id.save_button);
        completeBtn = (Button)findViewById(R.id.complete_button);
        selectDate = (Button)findViewById(R.id.select_date_button);
        showSelectDate = (EditText)findViewById(R.id.show_date);
        mContext = getApplicationContext();
        project_number.setFocusable(true);
        isFileExist = true;
        loadedContextFromS3 = "";

        credentialsProviders();

        dueDate = "2019-4-30";
        showDueData(dueDate);

        DBmanager = new DBManager(this);

        Intent intent = getIntent();
        state = intent.getIntExtra("state",ENTER_STATE);

        saveBtn.setOnClickListener(new saveButtonClickListener());
        completeBtn.setOnClickListener(new completeButtonClickListener());
        selectDate.setOnClickListener(new selectDateClickListener());
        isComplete = "unfinished";

        if(state == CHANGE_STATE){

            project_number.setFocusable(false);
            id = intent.getStringExtra("Id");
            projectNumber = intent.getStringExtra("ProNum");
            courseName = intent.getStringExtra("CouName");
            dueDate = intent.getStringExtra("DueDate");
            isComplete = intent.getStringExtra("Complete");
            menuPosition = intent.getIntExtra("menuPosition",0);

            // read file from S3 bucket
            readDataFromS3Bucket();

            try {
                sleep(2000); // stop main thread for 2 second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(!(loadedContextFromS3.equals(""))){
                String [] splitResult = loadedContextFromS3.split("#");
                course_number.setText(splitResult[2]);
                instruct_name.setText(splitResult[3]);
                project_description.setText(splitResult[4]);

            }else {
                Toast.makeText(getApplicationContext(), "File not exist in S3!", Toast.LENGTH_LONG).show();
            }


            project_number.setText(projectNumber);
            course_name.setText(courseName);

            showDueData(dueDate);

            if(isComplete.equals("finished")){
                completeBtn.setText(isComplete);
                completeBtn.setBackgroundColor(Color.parseColor("#00FFFF"));
            }

        }

    }

    private void credentialsProviders(){
          credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                  IDENTITY_POLLED_ID, // Identity pool ID
                Regions.CA_CENTRAL_1 // Region
        );
    }

    private void setCompleteBtn (String str){

        if(str.equals("unfinished")){
            isComplete = "finished";
            completeBtn.setText(isComplete);
            completeBtn.setBackgroundColor(Color.parseColor("#00FFFF"));
        }
        else {
            isComplete = "unfinished";
            completeBtn.setText(isComplete);
            completeBtn.setBackgroundColor(Color.parseColor("#FF6347"));
        }

    }

    private void showDueData(String dueDateStr){

        SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = sdf.parse(dueDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar = Calendar.getInstance();
        calendar.setTime(date);
        showSelectDate.setText(dueDateStr);

    }

    private void saveDataToLocalFile(){

        FileHelper fHelper = new FileHelper(mContext);
        String fileName = projectNumber + ".txt";
        String fileContent = projectNumber + "#" + courseName + "#" + courseNumber + "#" + instructName + "#" + projectDescription + "#"
                + isComplete + "#" + dueDate;

        try {
            fHelper.save(fileName, fileContent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void readDataFromS3Bucket(){

        new Thread(new Runnable() {
            public void run(){
                try{
                    AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.US_EAST_1));
                    String filePath = "amplify-appsync-files/" + projectNumber + ".txt";
                    S3Object s3Object = s3Client.getObject(BUCKET_NAME, filePath);

                    StringBuilder result = new StringBuilder();
                    InputStream objectData = s3Object.getObjectContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object.getObjectContent()));
                    String line;
                    while ((line = reader.readLine()) != null){
                        result.append(line);
                    }
                    loadedContextFromS3 = result.toString();
                    isFileExist = true;

                }catch (Exception e){
                    isFileExist = false;
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void uploadFileToS3(String fileName){

        FileHelper fHelper = new FileHelper(mContext);
        String localFilePath = fHelper.getFilePath(mContext);
        localFilePath = localFilePath + "/" + fileName + ".txt";
        String awsBucketPath = "amplify-appsync-files/" + fileName + ".txt";

        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
        final TransferObserver observer = transferUtility.upload(
                BUCKET_NAME,  //this is the bucket name on S3
                awsBucketPath, //this is the path and name
                new File(localFilePath), //path to the file locally
                CannedAccessControlList.PublicRead //to make the file public
        );
    }

    //If unique return true, else return false
    private boolean checkProjectNumberUniqueness(String projectNum){
        DBmanager.openDB();
        cursor = DBmanager.getAll();
        cursor.moveToFirst();
        int count = cursor.getCount();
        ArrayList<String> projectNumbers = new ArrayList<String>();
        for(int i=0; i < count; i++){
            projectNumbers.add(cursor.getString(cursor.getColumnIndex("ProjectNum")));
            cursor.moveToNext();
        }

        DBmanager.closeDB();
        if(projectNumbers.contains(projectNum)){
            return false;
        }
        return true;
    }



}
