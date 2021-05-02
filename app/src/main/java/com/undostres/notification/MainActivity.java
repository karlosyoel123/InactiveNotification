package com.undostres.notification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.Navigation;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;
import com.gamooga.targetact.client.CompanyIdNotInManifestException;
import com.gamooga.targetact.client.TargetActClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.installations.FirebaseInstallations;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.inbox.PushwooshInbox;
import com.pushwoosh.inbox.ui.PushwooshInboxUi;
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxActivity;
import com.undostres.notification.databinding.ActivityMainBinding;

import com.clevertap.android.sdk.CTInboxListener;
import com.clevertap.android.sdk.CTInboxStyleConfig;
/**/
//huawei
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.agconnect.config.LazyInputStream;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CTInboxListener, CTPushAmpListener {

    private static final String TAG = "MainActivity";

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseInAppMessaging mInAppMessaging;
    private CleverTapAPI cleverTapDefaultInstance;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Navigation.findNavController(this, R.id.nav_host_fragment)
                .setGraph(R.navigation.nav_graph_java);

        //clevertap
        cleverTapDefaultInstance  = CleverTapAPI.getDefaultInstance(this);
        cleverTapDefaultInstance.setCTPushAmpListener(this);

        //firebase
        fireBase();

        //pushwoosh
        pushWoosh();

        gamooga();

        cleverTap();

        comunes();

        //checkWhiteListNotification();
        huaweiServices();
        startInAppMsg();
    }

    void huaweiServices(){
        new Thread() {
            @Override
            public void run() {
                /*
                 */
                try {
                    String appId = AGConnectServicesConfig.fromContext(MainActivity.this).getString("client/app_id");
                    String token = HmsInstanceId.getInstance(MainActivity.this).getToken(appId, "HCM");
                    if(cleverTapDefaultInstance != null){
                        cleverTapDefaultInstance.pushHuaweiRegistrationId(token,true);
                    }
                    else{
                       // Log.e(TAG,"CleverTap is NULL");
                    }
                } catch (ApiException e) {
                    //Log.e(TAG, "get token failed, " + e);
                }
            }
        }.start();

        //huawei
        /**/AGConnectServicesConfig config = AGConnectServicesConfig.fromContext(this);
        config.overlayWith(new LazyInputStream(this) {
            public InputStream get(Context context) {
                try {
                        return context.getAssets().open("agconnect-services.json");
                } catch (IOException e) {
                    //Log.d("Error HUAWEI: ", e.getMessage());
                    return null;
                }
            }
        });

    }

    private void checkWhiteListNotification() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    private void comunes() {
        //pushwoosh
        PushwooshInbox.unreadMessagesCount(result -> {
            String data = "";
            if(result.getData().intValue()>0)
                data += result.getData().intValue();

            binding.txtUnreadPW.setText(data);
        });

    }

    @SuppressLint("MissingPermission")
    private void fireBase() {
        FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful()) {
                            //Log.d("Installations", "Installation ID: " + task.getResult());
                        } else {
                            //Log.e("Installations", "Unable to get Installation ID");
                        }
                    }
                });

        //FIAM
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mInAppMessaging = FirebaseInAppMessaging.getInstance();
        mInAppMessaging.setAutomaticDataCollectionEnabled(true);
        mInAppMessaging.setMessagesSuppressed(false);
        mFirebaseAnalytics.logEvent("engagement_party", new Bundle());
    }

    private void cleverTap() {
        startInAppMsg();
        CleverTapAPI.createNotificationChannel(getApplicationContext(),"test","tets",
                "Your Channel Description", NotificationManager.IMPORTANCE_MAX,true);

        CTInboxStyleConfig styleConfig = new CTInboxStyleConfig();
        styleConfig.setNavBarTitle("CleverTap Messages");
        styleConfig.setNavBarColor("#009fdd");
        //message inbox
        binding.btnInboxCT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startInAppMsg();
                cleverTapDefaultInstance.getAllInboxMessages();
                cleverTapDefaultInstance.showAppInbox(styleConfig);
            }
        });

    }

    private void startInAppMsg() {
        if (cleverTapDefaultInstance != null) {
            //Set the Notification Inbox Listener
            cleverTapDefaultInstance.setCTNotificationInboxListener(this);
            //Initialize the inbox and wait for callbacks on overridden methods
            cleverTapDefaultInstance.initializeInbox();
        }
    }

    private void gamooga() {
        //push notification
        TargetActClient tac = TargetActClient.getInstance();
        try {
            tac.initialize(getApplicationContext());
            String tok = tac.getVisitorID();
            //Log.d("Gamooga",tok);
            TargetActClient.getInstance().initialize(this,false);
            tac.doPushRegistration();


        } catch (CompanyIdNotInManifestException e) {
            e.printStackTrace();
            //Log.d("Error",e.getMessage());
        }
    }

    private void pushWoosh() {
        //message inbox
        binding.btnInboxPW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), InboxActivity.class));
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.inboxContainer, PushwooshInboxUi.INSTANCE.createInboxFragment())
                        .commitAllowingStateLoss();
            }
        });
        //push notification
        Pushwoosh.getInstance().registerForPushNotifications();
    }


    @Override
    public void inboxDidInitialize() {

    }

    @Override
    public void inboxMessagesDidUpdate() {
        Integer cont = cleverTapDefaultInstance.getInboxMessageUnreadCount();
        if(cont>0){
            binding.txtUnreadCT.setText(cont.toString());
        }else{
            binding.txtUnreadCT.setText(" ");
        }
      //  Toast.makeText(getApplicationContext(),"Updating", Toast.LENGTH_LONG).show();
    }

    //PLUSH APMLIFICATION CT
    @Override
    public void onPushAmpPayloadReceived(Bundle extras) {
        //Log.d("Amplification CT: ", extras+"");
    }

    @Override
    public void onBackPressed() {
        try {
            binding.btnInboxPW.setVisibility(View.VISIBLE);
            binding.btnInboxCT.setVisibility(View.VISIBLE);
            binding.txtUnreadCT.setVisibility(View.VISIBLE);
            binding.txtUnreadPW.setVisibility(View.VISIBLE);
            inboxMessagesDidUpdate();
            comunes();
        }catch (Exception e){
            //Log.d("Error",e.getMessage());
        }
        super.onBackPressed();
    }
}