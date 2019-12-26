package com.example.projecttracker;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class MyListView extends BaseAdapter {

    private LayoutInflater LI;
    private List<String> list_project_number;
    private List<String> list_course_name;
    private List<String> list_is_project_complete;

    public MyListView(Context context, List<String> listProjectId, List<String> listCourseName, List<String> listIsComplete){
        this.list_project_number = listProjectId;
        this.list_course_name = listCourseName;
        this.list_is_project_complete = listIsComplete;
        LI = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }



    @Override
    public int getCount() {
        return list_project_number.size();
    }

    @Override
    public Object getItem(int i) {
        return list_project_number.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if(view == null){
            view = LI.inflate(R.layout.activity_listview,null);
        }

        TextView Title = (TextView)view.findViewById(R.id.project_number);
        TextView DataTime = (TextView)view.findViewById(R.id.course_name);

        Title.setText("Project Number: " + list_project_number.get(i));
        DataTime.setText("Course Name: " + list_course_name.get(i));

        if(list_is_project_complete.get(i).equals("finished")){
            view.setBackgroundColor(Color.parseColor("#00FFFF"));
        }else {
            view.setBackgroundColor(Color.parseColor("#FF6347"));
        }


        return view;
    }

    public void addRecord(String projectName, String courseName, String isComplete){
        list_project_number.add(projectName);
        list_course_name.add(courseName);
        list_is_project_complete.add(isComplete);
    }

    public void removeRecord(int num){
        list_project_number.remove(num);
        list_course_name.remove(num);
        list_is_project_complete.remove(num);

    }
}
