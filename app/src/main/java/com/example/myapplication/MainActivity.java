package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AndroidClarified";
    private GoogleSignInClient googleSignInClient;
    private SignInButton googleSıgnInButton;
    private Button signOut;
    private TextToSpeech textToSpeechOperator;
    private TextView targetMailAddress, mailSubject, mailContent;
    private boolean speakFinished = false;
    private boolean firstApplicationRunBool = false;
    private int choiceCounter = 0;
    private boolean isSendEmail = false;
    private Bundle utteranceBundle;
    private GoogleAccountCredential mCredential;
    private Gmail mService;
    private String activeUSer;
    private static final String[] SCOPES = {
            GmailScopes.GMAIL_LABELS,
            GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_INSERT,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.MAIL_GOOGLE_COM
    };
    final int constant = 100;
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case constant: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.GET_ACCOUNTS)){

        }
        else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.GET_ACCOUNTS},constant);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(MainActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        utteranceBundle = new Bundle();
        utteranceBundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID");

        targetMailAddress = (TextView)findViewById(R.id.targetMailAddress);
        mailSubject = (TextView)findViewById(R.id.mailSubject);
        mailContent = (TextView)findViewById(R.id.mailContent);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getResources().getString(R.string.client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName(getResources().getString(R.string.app_name))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        textToSpeechOperator = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int resultStatus) {

                if (resultStatus == TextToSpeech.SUCCESS) {
                    int result = textToSpeechOperator.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA) {
                        Log.e("TextToSpeech", "Language is not supported");
                    }

                    textToSpeechOperator.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            new Thread()
                            {
                                public void run()
                                {
                                    MainActivity.this.runOnUiThread(new Runnable()
                                    {
                                        public void run()
                                        {

                                            Toast.makeText(MainActivity.this, "Speak Finished", Toast.LENGTH_SHORT).show();
                                            speakFinished = true;
                                        }
                                    });
                                }
                            }.start();


                        }

                        @Override
                        public void onStop(String utteranceId,boolean interrupted) {

                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });
                    textToSpeechOperator.setSpeechRate(0.85f);
                    firstApplicationRunBool = true;
                    if (!googleSignInCheckOrSignIn()) {
                        firstApplicationRunBool = true;
                        textToSpeechFunction("Welcome! Please sign-in into your gmail account by pressing approximately, center of your screen. Say Sign Out for sign in into another account");
                        googleSignInIntoAccount();


                    } else {
                        textToSpeechFunction("Welcome Back! Voice for mail is ready. After each operation click anywhere to speak. Please Say One for Sending an Email. Or Say Two for inbox operations");
                        firstApplicationRunBool = false;
                    }

                } else {
                    Log.e("TextToSpeech", "Initialisation Failed!");
                }

            }
        });



    }
    private boolean googleSignInCheckOrSignIn(){

        GoogleSignInAccount alreadyloggedAccount = GoogleSignIn.getLastSignedInAccount(this);
        if(alreadyloggedAccount != null){
            // TODO if already signed-in return true; else false

            activeUSer = alreadyloggedAccount.getEmail();
            return true;

        }

        return false;
    }
    private void googleSignInIntoAccount(){
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 101);


    }
    private void signOutFromAccount(){
        googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(MainActivity.this, "Signed Out", Toast.LENGTH_SHORT).show();
                textToSpeechFunction("You signed out. Please sign-in into your gmail account by pressing approximately, center of your screen.");
                googleSignInIntoAccount();
            }
        });
    }
    private void textToSpeechFunction(String text) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeechOperator.speak(text, TextToSpeech.QUEUE_FLUSH, utteranceBundle, "UniqueID");
        } else {
            textToSpeechOperator.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void onLoggedIn(GoogleSignInAccount googleSignInAccount) {
        /*Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.GOOGLE_ACCOUNT, googleSignInAccount);
        startActivity(intent);
        finish();*/
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 999) {

            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> resultList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (resultList.get(0).equals("restart")) {
                    textToSpeechFunction("Application Restarting.");
                    // exitFromApp();
                }
                else if(resultList.get(0).equals( "sign out")){
                    choiceCounter = 0;
                    signOutFromAccount();

                }else {
                    if (!firstApplicationRunBool) {
                        Toast.makeText(MainActivity.this, resultList.get(0), Toast.LENGTH_SHORT).show();
                        switch (choiceCounter) {
                            case 0:
                                textToSpeechFunction("Welcome ! After each operation click anywhere to speak. Please Say One for Sending an Email. Or Say Two for inbox operations");

                                break;
                            case 1:
                                String choice = resultList.get(0);

                                if(choice.equals("1")||choice.equals("One")||choice.equals("one")||choice.equals("Von")||choice.equals("von")||choice.equals("van")||choice.equals("Van")||choice.equals("won")||choice.equals("Won")||choice.equals("want")) // Main choice for sending or receiving Emails
                                {
                                    textToSpeechFunction( "Please say the target mail address");
                                    isSendEmail = true;
                                }
                                else if(choice.equals( "2")||choice.equals( "Two")||choice.equals("two")||choice.equals("To")||choice.equals("to")||choice.equals("tow")||choice.equals("Tow")||choice.equals("too")){
                                    textToSpeechFunction( "This feature is currently unavailable. Say 1 for sending an email.");
                                    choiceCounter--;
                                    isSendEmail = false;
                                }
                                else{
                                    textToSpeechFunction( "You choose . . . You didn't have a choice");
                                    choiceCounter--;
                                }
                                break;
                            case 2:
                                if(isSendEmail){
                                    String to;
                                    to = resultList.get(0).replaceAll("underscore", "_");
                                    to = to.replaceAll("\\s+", "");
                                    to = to + "@gmail.com";
                                    targetMailAddress.setText(to);
                                    textToSpeechFunction("Please Say the Subject.");


                                }
                                else{

                                }

                                break;
                            case 3:
                                if(isSendEmail){
                                    mailSubject.setText(resultList.get(0));
                                    textToSpeechFunction("Please Say The Message");
                                    break;
                                }else{

                                }
                                break;
                            case 4:
                                if(isSendEmail){
                                    mailContent.setText(resultList.get(0));
                                    textToSpeechFunction("Please Confirm fallowing information. Target Mail Address is. " + targetMailAddress.getText().toString() + "\n . Mail Subject is. : " + mailSubject.getText().toString() + "\n . and the mail content is.  : " + mailContent.getText().toString() + "\n . Please Say Yes to confirm");
                                    break;
                                }else{

                                }
                                break;
                            case 5:
                                if(isSendEmail){
                                    String choice2 = resultList.get(0);

                                    if(choice2.equals("yes")){
                                        /*try {
                                            MimeMessage theMessage = createEmail("safacelik13@gmail.com","m.enver.akkoc@gmail.com",mailSubject.getText().toString(),mailContent.getText().toString());
                                            sendMessage(mService,"me",theMessage);
                                        } catch (MessagingException e) {
                                            e.printStackTrace();
                                        }
                                        catch (IOException o){
                                            o.printStackTrace();
                                        }*/
                                        textToSpeechFunction("Your email sent.");
                                        choiceCounter=0;

                                    }
                                    else if (choice2.equals("no")){
                                        textToSpeechFunction("Directing to main menu");
                                        choiceCounter = 0;
                                    }
                                    else {
                                        textToSpeechFunction("Sorry ! I could not understand ! Please only say yes or no");
                                        choiceCounter --;
                                    }
                                }else{


                                }
                                break;
                            default:
                                break;


                        }

                    }


                }
            } else {

                textToSpeechFunction("Please say again.");
                choiceCounter--;
            }

        }
        else if (resultCode == Activity.RESULT_OK && requestCode == 101)
            try {
                // The Task returned from this call is always completed, no need to attach
                // a listener.
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onLoggedIn(account);
                activeUSer = account.getEmail();
                textToSpeechFunction(" You signed in into " + account.getDisplayName());
                textToSpeechFunction(" Voice for mail is ready. After each operation click anywhere to speak. Please Say One for Sending an Email. Or Say Two for inbox operations");

                firstApplicationRunBool = false;
            } catch (ApiException e) {
                // The ApiException status code indicates the detailed failure reason.
                Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            }


    }
    public void onAnyViewClicked(View view) {
        if (speakFinished&&!firstApplicationRunBool) {
            speakFinished = false;
            choiceCounter++;
            recognize();

        }
    }

    private void recognize() {
        Intent recognizeIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        recognizeIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Accordingly");
        recognizeIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
        recognizeIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 6000);
        recognizeIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizeIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        try {
            startActivityForResult(recognizeIntent, 999);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Device does not support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }


    public static MimeMessage createEmail(String to,
                                          String from,
                                          String subject,
                                          String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }
    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
    public static Message sendMessage(Gmail service,
                                      String userId,
                                      MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message;
    }
    @Override
    public void onStart() {
        super.onStart();

    }

}
