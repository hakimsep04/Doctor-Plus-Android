package com.rit.doctorplus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import static com.rit.doctorplus.MainActivity.ADVICE_TEXT;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        TextView adviceTextView = (TextView) findViewById(R.id.advice);
        Intent intent = getIntent();
        String advice = intent.getStringExtra(ADVICE_TEXT);

        adviceTextView.setText(advice);
    }
}
