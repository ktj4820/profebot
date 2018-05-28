package com.example.ezequielborenstein.profebot;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class GlobalHelper {

    private static AppCompatActivity mainActivity;

    public static void setMainActivity(AppCompatActivity activity) {
        if(mainActivity == null){
            mainActivity = activity;
        }
    }

    public static void setUpMainMenuShortCut(TextView shortCut){
        shortCut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View title) {
                Intent intent = new Intent(title.getContext(), mainActivity.getClass());
                mainActivity.startActivity(intent);
            }
        });
    }
}