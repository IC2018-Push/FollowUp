package com.ibm.bluemix.appid.android.sample.appid;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

import com.ibm.bluemix.appid.android.api.AppID;
import com.ibm.bluemix.appid.android.api.AppIDAuthorizationManager;
import com.ibm.bluemix.appid.android.api.tokens.IdentityToken;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import cz.msebera.android.httpclient.Header;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import cz.msebera.android.httpclient.entity.StringEntity;


/**
 * Created by Apple on 11/03/18.
 */

public class Home extends AppCompatActivity {

    private AppID appID;

    private AppIDAuthorizationManager appIDAuthorizationManager;
    private TokensPersistenceManager tokensPersistenceManager;
    private NoticeHelper noticeHelper;
    private MFPPush push = null;
    private MFPPushNotificationListener notificationListener = null;

    private JSONArray jaFoodSelection;
    private NoticeHelper.AuthState authState;

    private String userName = null;
    private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        appID = AppID.getInstance();

        appIDAuthorizationManager = new AppIDAuthorizationManager(appID);
        tokensPersistenceManager = new TokensPersistenceManager(this, appIDAuthorizationManager);

        noticeHelper = new NoticeHelper(this, appIDAuthorizationManager, tokensPersistenceManager);
        IdentityToken idt = appIDAuthorizationManager.getIdentityToken();

        //Getting information from identity token. This is information that is coming from the identity provider.
         userName = idt.getEmail() != null ? idt.getEmail().split("@")[0] : "Guest";
        if(idt.getName() != null)
            userName = idt.getName();

        regForPush();
    }

    private void regForPush () {

        new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText("Hello " + userName)
                .setContentText("Would you like to register for push Notifications Service?")
                .setConfirmText("OK")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        callPushRegister();

                    }
                })
                .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }

    private void callPushRegister () {

        BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_US_SOUTH);
        push = MFPPush.getInstance();
        push.initialize(getApplicationContext(),getResources().getString(R.string.pushAppGUID),getResources().getString(R.string.pushClientSecret));

        push.registerDevice(new MFPPushResponseListener<String>() {
            @Override
            public void onSuccess(String deviceId) {
                Log.i("PUSH","Device is registered with Push Service.");

                runOnUiThread(new Runnable() {
                    public void run() {
                        callFeedback();
                    }
                });
            }

            @Override
            public void onFailure(MFPPushException ex) {
                Log.i("PUSH","Error registering with Push Service...\n" + ex.toString()
                        + "Push notifications will not be received.");
            }
        });

        final Activity activity = this;

        notificationListener = new MFPPushNotificationListener() {

            @Override
            public void onReceive(final MFPSimplePushNotification message) {

                callSuccess("You got a Message !",message.getAlert());

            }
        };
        push.listen(notificationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (push != null) {
            push.listen(notificationListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (push != null) {
            push.hold();
        }

    }

    private void callFeedback () {
        new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText("Hello " + userName)
                .setContentText("How's your recent purchase , would you like to give us a feedback")
                .setConfirmText("OK")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        callReview();
                    }
                })
                .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }

    private void callReview () {
        final EditText editText = new EditText(this);
        final SweetAlertDialog reviewPage = new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText("Feedback")
                .setConfirmText("Ok")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        callCloudant(editText.getText().toString());
                        sweetAlertDialog.dismissWithAnimation();

                    }
                })
                .setCustomView(editText);
        reviewPage.show();


    }

    private void callSuccess (String titleText , String message) {
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText(titleText)
                .setContentText(message)
                .show();
    }

    //Cloudant call

    private void callCloudant (String feedbackText) {

        String BASE_URL = "https://" + getResources().getString(R.string.cloudantUserName) + ".cloudant.com/" + getResources().getString(R.string.cloudantDbname);
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.cloudantPermissionKey),getResources().getString(R.string.cloudantPermissionPassword));
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("notes", "Test api support");
            jsonParams.put("deviceIds", BMSClient.getInstance().getAuthorizationManager().getDeviceIdentity().getId());
            jsonParams.put("message", feedbackText);
            jsonParams.put("name", userName);
            jsonParams.put("productNumber",getRandomString(2));
            StringEntity entity = new StringEntity(jsonParams.toString());
            client.post(this, BASE_URL, entity, "application/json", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    System.out.print(statusCode);
                    callSuccess("Thanks !","We appreciate your valuable feedback");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    System.out.print(statusCode);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static String getRandomString(final int sizeOfRandomString)
    {
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(sizeOfRandomString);
        for(int i=0;i<sizeOfRandomString;++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }
}
