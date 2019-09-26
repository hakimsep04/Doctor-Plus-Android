package com.rit.doctorplus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.safetynet.HarmfulAppsData;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    // TextViews reference
    TextView screenLockScore;
    TextView osVersionScore;
    TextView unknownSrcScore;
    TextView harmfulAppsScore;
    TextView developerMenuScore;
    TextView basicScore;
    TextView ctsScore;
    TextView sysParam;
    TextView score;
    TextView result;

    // Button reference
    Button screenLockDetails;
    Button osVersionDetails;
    Button unknownSrcDetails;
    Button harmfulAppsDetails;
    Button developerMenuDetails;
    Button basicDetails;
    Button ctsDetails;
    Button scanBtn;
    Button resetBtn;

    // Checkbox reference
    CheckBox screenLockChk;
    CheckBox osVersionChk;
    CheckBox unknownSrcChk;
    CheckBox harmfulAppsChk;
    CheckBox developerMenuChk;
    CheckBox basicChk;
    CheckBox ctsChk;
    CheckBox completeChk;

    private static final String TAG = "MainActivity";
    private static int scoreCounter = 0;
    private static int checkedCounter = 0;
    private static HashSet<String> facts;
    private static HashMap<String, String> advicesMap;
    private static List<HarmfulAppsData> appList;
    private static String ctsAdvice = "";
    public static final String ADVICE_TEXT = "com.rit.doctorplus.advice.extratext";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        completeChk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                    screenLockChk.setChecked(isChecked);
                    osVersionChk.setChecked(isChecked);
                    unknownSrcChk.setChecked(isChecked);
                    harmfulAppsChk.setChecked(isChecked);
                    developerMenuChk.setChecked(isChecked);
                    basicChk.setChecked(isChecked);
                    ctsChk.setChecked(isChecked);
                }
            }
        );

       scanBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {

               if(!isNetworkConnected() && (unknownSrcChk.isChecked() || harmfulAppsChk.isChecked() || basicChk.isChecked() || ctsChk.isChecked())){
                   reset();
                   final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                   alertDialogBuilder.setMessage("Some of the metrics selected requires internet connection. Please connect your device to internet and continue.")
                           .setTitle("Error!");
                   alertDialogBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           alertDialogBuilder.setCancelable(true);
                           // User cancelled the dialog
                       }
                   });
                   alertDialogBuilder.create();
                   alertDialogBuilder.show();
               }else{
                   result.setText("Please wait...");
                   scanBtn.setEnabled(false);
                   if(screenLockChk.isChecked()){
                       checkScreenLock();
                       checkedCounter++;
                   }
                   if(osVersionChk.isChecked()){
                       checkOSVersion();
                       checkedCounter++;
                   }
                   if(unknownSrcChk.isChecked()){
                       checkVerifyApps();
                       checkedCounter++;
                   }
                   if(harmfulAppsChk.isChecked()){
                       potentialHarmfulApps();
                       checkedCounter++;
                   }
                   if(developerMenuChk.isChecked()){
                       checkDeveloperMenu();
                       checkedCounter++;
                   }
                   if(basicChk.isChecked() || ctsChk.isChecked()){
                       safetyNetAttestation();
                       checkedCounter++;
                   }
                   if(checkedCounter == 0){
                       reset();
                       final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                       alertDialogBuilder.setMessage("Please select metrics to test!")
                               .setTitle("Error!");
                       alertDialogBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               alertDialogBuilder.setCancelable(true);
                               // User cancelled the dialog
                           }
                       });
                       alertDialogBuilder.create();
                       alertDialogBuilder.show();

                   }
                   else if(!basicChk.isChecked() && !ctsChk.isChecked()) expertShell();

               }

           }
       });

       resetBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               reset();
           }
       });

       screenLockDetails.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               openDetailActivity(advicesMap.get("SCREEN LOCK ADVICE"));
           }
       });

       osVersionDetails.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               openDetailActivity(advicesMap.get("OS ADVICE"));
           }
       });

       unknownSrcDetails.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
                openDetailActivity(advicesMap.get("VERIFY APPS ADVICE"));
           }
       });

       harmfulAppsDetails.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
                openDetailActivity(advicesMap.get("HARMFUL APPS ADVICE"));
           }
       });

       developerMenuDetails.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               openDetailActivity(advicesMap.get("DEVELOPER OPTION MENU ADVICE"));
           }
       });

       basicDetails.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               openDetailActivity(advicesMap.get("BASIC TEST ADVICE"));
           }
       });

       ctsDetails.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               openDetailActivity(advicesMap.get("CTS TEST ADVICE"));
           }
       });


//        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext())
//                == ConnectionResult.SUCCESS) {
//            textView.setText("Success");
//            // The SafetyNet Attestation API is available.
//        } else {
//            // Prompt user to update Google Play services.
//            textView.setText("Failed");
//        }
    }

    private void openDetailActivity(String advice){
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(ADVICE_TEXT, advice);
        startActivity(intent);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            //You can replace it with your name
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    private void checkScreenLock(){
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager.isDeviceSecure()){
            facts.add("SCREEN LOCKED");
        }else{
            facts.add("SCREEN NOT LOCKED");
        }
    }

    private void checkOSVersion() {
        if(Build.VERSION.SDK_INT < 28){
            facts.add("OLD OS VERSION");
        } else if(Build.VERSION.SDK_INT == 28) {
            facts.add("PREVIOUS OS VERSION");
        }else {
            facts.add("LATEST OS VERSION");
        }
    }

    private void checkDeveloperMenu() {
        int value = Settings.Secure.getInt(getApplicationContext().getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        if(value > 0){
            facts.add("DEVELOPER MENU ENABLED");
        } else {
            facts.add("DEVELOPER MENU DISABLED");
        }
    }

    private void checkVerifyApps() {
        SafetyNet.getClient(this)
                .isVerifyAppsEnabled()
                .addOnCompleteListener(new OnCompleteListener<SafetyNetApi.VerifyAppsUserResponse>() {
                    @Override
                    public void onComplete(Task<SafetyNetApi.VerifyAppsUserResponse> task) {
                        if (task.isSuccessful()) {
                            SafetyNetApi.VerifyAppsUserResponse result = task.getResult();
                            if (result.isVerifyAppsEnabled()) {
                                facts.add("VERIFY APPS ENABLED");
                            } else {
                                facts.add("VERIFY APPS DISABLED");
                            }
                        } else {
                            facts.add("VERIFY APPS ERROR");
                        }
                    }
                });
    }

    private void potentialHarmfulApps() {
        SafetyNet.getClient(this)
                .listHarmfulApps()
                .addOnCompleteListener(new OnCompleteListener<SafetyNetApi.HarmfulAppsResponse>() {
                    @Override
                    public void onComplete(Task<SafetyNetApi.HarmfulAppsResponse> task) {
                        Log.d(TAG, "Received listHarmfulApps() result");

                        if (task.isSuccessful()) {
                            SafetyNetApi.HarmfulAppsResponse result = task.getResult();

                            appList = result.getHarmfulAppsList();
                            if (appList.isEmpty()) {
                                facts.add("NO HARMFUL APPS FOUND");
                                Log.d(TAG, "No harmful apps found");
                            } else {
                                facts.add("HARMFUL APPS FOUND!");
                            }
                        } else {
                            facts.add("HARMFUL APPS ERROR");
                        }
                    }
                });
    }

    private void safetyNetAttestation(){
        byte[] nonce = UUID.randomUUID().toString().getBytes();
        String API_KEY = "AIzaSyBhHwdZZ604TADOKElLMIaRYjEIXQZZDZM";

        SafetyNet.getClient(this).attest(nonce, API_KEY)
                .addOnSuccessListener(this,
                        new OnSuccessListener<SafetyNetApi.AttestationResponse>() {
                            @Override
                            public void onSuccess(SafetyNetApi.AttestationResponse response) {
                                // Indicates communication with the service was successful.
                                String[] JWSResults = decodeJWS(response.getJwsResult()).split(",");

                                for(String result: JWSResults){
                                    if(result.contains("ctsProfileMatch")){
                                        if(result.contains("false")) facts.add("CTS FAILED");
                                        else facts.add("CTS PASSED");
                                    }
                                    if(result.contains("basicIntegrity")){
                                        if(result.contains("false")) facts.add("BASIC FAILED");
                                        else facts.add("BASIC PASSED");
                                    }
                                    if(result.contains("advice")) ctsAdvice = result.substring(0, result.length()-1);
                                }

                                Log.d(TAG, decodeJWS(response.getJwsResult()));
                                Log.d(TAG, ctsAdvice);
                                expertShell();
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        facts.add("ATTESTATION API ERROR!");
                        // An error occurred while communicating with the service.
                        if (e instanceof ApiException) {
                            // An error with the Google Play services API contains some
                            // additional details.
                            ApiException apiException = (ApiException) e;
                            // You can retrieve the status code using the
                            // apiException.getStatusCode() method.
                        } else {
                            // A different, unknown type of error occurred.
                            Log.d(TAG, "Error: " + e.getMessage());
                        }
                        expertShell();
                    }
                });
    }

    private String decodeJWS(String jwsResult) {
        if (jwsResult == null) {
            return null;
        }
        final String[] jwtParts = jwsResult.split("\\.");
        if (jwtParts.length == 3) {
            String decodedPayload = new String(Base64.decode(jwtParts[1], Base64.DEFAULT));
            return decodedPayload;
        } else {
            return null;
        }
    }

    private void init(){
        // Connecting textview components
        screenLockScore = (TextView) findViewById(R.id.score1);
        osVersionScore = (TextView) findViewById(R.id.score2);
        unknownSrcScore = (TextView) findViewById(R.id.score3);
        harmfulAppsScore = (TextView) findViewById(R.id.score4);
        developerMenuScore = (TextView) findViewById(R.id.score5);
        basicScore = (TextView) findViewById(R.id.score6);
        ctsScore = (TextView) findViewById(R.id.score7);
        sysParam = (TextView) findViewById(R.id.sysParam);
        score = (TextView) findViewById(R.id.score);
        result = (TextView) findViewById(R.id.result);

        // Connecting button reference
        screenLockDetails = (Button) findViewById(R.id.detail1);
        osVersionDetails = (Button) findViewById(R.id.detail2);
        unknownSrcDetails = (Button) findViewById(R.id.detail3);
        harmfulAppsDetails = (Button) findViewById(R.id.detail4);
        developerMenuDetails = (Button) findViewById(R.id.detail5);
        basicDetails = (Button) findViewById(R.id.detail6);
        ctsDetails = (Button) findViewById(R.id.detail7);
        scanBtn = (Button) findViewById(R.id.scan);
        resetBtn = (Button) findViewById(R.id.reset);
        resetBtn.setEnabled(false);

        // Connecting Checkbox reference
        screenLockChk = (CheckBox) findViewById(R.id.ScreenLock);
        osVersionChk = (CheckBox) findViewById(R.id.OS);
        unknownSrcChk = (CheckBox) findViewById(R.id.sources);
        harmfulAppsChk = (CheckBox) findViewById(R.id.harmfulApps);
        developerMenuChk = (CheckBox) findViewById(R.id.developerMenu);
        basicChk = (CheckBox) findViewById(R.id.basic);
        ctsChk = (CheckBox) findViewById(R.id.cts);
        completeChk = (CheckBox) findViewById(R.id.all);

        //Initializing facts list
        facts = new HashSet<>();
        advicesMap = new HashMap<>();
    }

    public void reset(){
        scoreCounter = 0;
        checkedCounter = 0;
        facts.clear();
        advicesMap.clear();
        result.setTextColor(Color.BLACK);
        result.setText("Result");
        scanBtn.setEnabled(true);
        screenLockChk.setChecked(false);
        osVersionChk.setChecked(false);
        unknownSrcChk.setChecked(false);
        harmfulAppsChk.setChecked(false);
        developerMenuChk.setChecked(false);
        basicChk.setChecked(false);
        ctsChk.setChecked(false);
        completeChk.setChecked(false);
        screenLockScore.setText("0");
        osVersionScore.setText("0");
        unknownSrcScore.setText("0");
        harmfulAppsScore.setText("0");
        developerMenuScore.setText("0");
        basicScore.setText("0");
        ctsScore.setText("0");
        basicScore.setTextColor(Color.BLACK);
        harmfulAppsScore.setTextColor(Color.BLACK);
        resetBtn.setEnabled(false);

    }

    public void expertShell(){
        if (facts.contains("SCREEN LOCKED")){
            screenLockScore.setText("1/1");
            advicesMap.put("SCREEN LOCK ADVICE", "SCREEN LOCK ADVICE \n\nYour device has screen lock and therefore it is safe.\nRecommended: Password is the most secure lock");
            scoreCounter += 1;
        }
        if(facts.contains("SCREEN NOT LOCKED")){
            screenLockScore.setText("0/1");
            advicesMap.put("SCREEN LOCK ADVICE", "SCREEN LOCK ADVICE \n\nYour device is not secure! You do not have a screen lock. You device can leak sensitive data. Please go to settings now to set a screen lock for your device." +
                    "\nRecommended: Password is the most secure lock");
        }
        if(facts.contains("OLD OS VERSION")){
            osVersionScore.setText("0/2");
            advicesMap.put("OS ADVICE", "OS ADVICE \n\nYour device have old operating system, please update to latest to stay away from dangerous vulnerabilities.");
        }
        if(facts.contains("PREVIOUS OS VERSION")){
            osVersionScore.setText("1/2");
            advicesMap.put("OS ADVICE", "OS ADVICE \n\nYour device have previous version of operating system, please update to latest to stay away from dangerous vulnerabilities.");
            scoreCounter += 1;
        }
        if(facts.contains("LATEST OS VERSION")){
            osVersionScore.setText("2/2");
            advicesMap.put("OS ADVICE", "OS ADVICE \n\nYour device operating system is up to date. Your device is secure against known vulnerabilities");
            scoreCounter += 2;
        }
        if(facts.contains("VERIFY APPS ENABLED")){
            unknownSrcScore.setText("1/1");
            advicesMap.put("VERIFY APPS ADVICE", "VERIFY APPS ADVICE \n\nYour device have enabled verify apps feature. If there is any harmful app installed in your device this feature will detect and notify to uninstall the app.");
            scoreCounter += 1;
        }
        if(facts.contains("VERIFY APPS DISABLED")){
            unknownSrcScore.setText("0/1");
            advicesMap.put("VERIFY APPS ADVICE", "VERIFY APPS ADVICE \n\nGoogle monitors and profiles the behavior of Android apps." +
                    " If the Verify Apps feature detects a potentially dangerous app, all users who have installed the app are notified and encouraged to promptly uninstall the app. " +
                    "This process protects the security and privacy of these users. " +
                    "Your device has not enabled Verify apps feature. Please go to settings to enable this feature.");
        }
        if(facts.contains("VERIFY APPS ERROR")){
            unknownSrcScore.setText("NA");
            advicesMap.put("VERIFY APPS ADVICE", "VERIFY APPS ADVICE \n\nThere has been some error in checking verify apps feature in your device. Please check your network connection and try again");
        }
        if(facts.contains("NO HARMFUL APPS FOUND")){
            harmfulAppsScore.setText("2/2");
            advicesMap.put("HARMFUL APPS ADVICE", "HARMFUL APPS ADVICE \n\nNo harmful applications were found in your device. Your device is secure from harmful applications.");
            scoreCounter += 2;
        }
        if(facts.contains("HARMFUL APPS FOUND!")){
            if (appList.size() > 2){
                harmfulAppsScore.setText("-2");
                harmfulAppsScore.setTextColor(Color.RED);
                scoreCounter -= 2;
            }else {
                harmfulAppsScore.setText("0/2");
            }
            int counter = 1;
            String result = "HARMFUL APPS ADVICE\n";
            for (HarmfulAppsData harmfulApp : appList) {
                result += "\n" + counter + ". " + harmfulApp.apkPackageName;
                counter++;
            }
            advicesMap.put("HARMFUL APPS ADVICE", result);
        }
        if(facts.contains("HARMFUL APPS ERROR")){
            harmfulAppsScore.setText("NA");
            advicesMap.put("HARMFUL APPS ADVICE", "HARMFUL APPS ADVICE \n\nThere was an error checking harmful apps list in your device. Ensure verify apps feature is enabled and please try again.");
        }
        if(facts.contains("DEVELOPER MENU ENABLED")){
            developerMenuScore.setText("0/1");
            advicesMap.put("DEVELOPER OPTION MENU ADVICE", "DEVELOPER OPTION MENU ADVICE \n\n" +
                    "Your device has enabled developer option menu. This could potentially lead to leakage of sensitive information. Please disable developer option in settings");
        }
        if(facts.contains("DEVELOPER MENU DISABLED")){
            developerMenuScore.setText("1/1");
            advicesMap.put("DEVELOPER OPTION MENU ADVICE", "DEVELOPER OPTION MENU ADVICE \n\n" +
                    "Your device has disabled developer option menu. Your device is secure.");
            scoreCounter += 1;
        }
        if(facts.contains("BASIC PASSED")){
            basicScore.setText("1/1");
            scoreCounter += 1;
            advicesMap.put("BASIC TEST ADVICE", "BASIC TEST ADVICE \n\nYour device passed the compatibility test.");
        }
        if(facts.contains("BASIC FAILED")){
            basicScore.setText("-1");
            basicScore.setTextColor(Color.RED);
            scoreCounter -= 1;
            String result = "BASIC TEST ADVICE\n";
            String []failedTests = ctsAdvice.split(":");
            String []advices = failedTests[1].split(",");
            int index = 1;
            for(String adv : advices){
                result += "\n" + index + ". " + adv;
                index++;
            }
            advicesMap.put("BASIC TEST ADVICE", result);

        }
        if(facts.contains("CTS PASSED")){
            ctsScore.setText("2/2");
            scoreCounter += 2;
            advicesMap.put("CTS TEST ADVICE", "CTS TEST ADVICE \n\nYour device passed the compatibility test.");
        }
        if(facts.contains("CTS FAILED")){
            ctsScore.setText("0/2");
            String result = "CTS TEST ADVICE\n";
            String []failedTests = ctsAdvice.split(":");
            String []advices = failedTests[1].split(",");
            int index = 1;
            for(String adv : advices){
                result += "\n" + index + ". " + adv;
                index++;
            }
            advicesMap.put("CTS TEST ADVICE", result);
        }

        if(checkedCounter == 6){
            if(scoreCounter < 1){
                result.setText("Score total: " + scoreCounter + " . DANGER!");
                result.setTextColor(Color.RED);
            }
            else if(scoreCounter > 0 && scoreCounter < 3){
                result.setText("Score total: " + scoreCounter + " . CRITICAL!");
                result.setTextColor(Color.parseColor("#FFA500"));
            }
            else if(scoreCounter > 2 && scoreCounter < 6){
                result.setText("Score total: " + scoreCounter + " . LOW!");
                result.setTextColor(Color.parseColor("#FFD300"));
            }
            else if(scoreCounter > 5 && scoreCounter < 9){
                result.setText("Score total: " + scoreCounter + " . MEDIUM!");
                result.setTextColor(Color.parseColor("#FDFF00"));
            }
            else if(scoreCounter > 8 && scoreCounter < 11){
                result.setText("Score total: " + scoreCounter + " . PERFECT!");
                result.setTextColor(Color.GREEN);
            }
        }else {
            result.setText("Done! Click details to know more");
        }



        resetBtn.setEnabled(true);
    }

}
