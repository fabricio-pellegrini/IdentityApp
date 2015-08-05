package com.pellegrini.identityapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = MainActivity.class.getName();
    private GoogleApiClient mApiClient;

    private SignInButton mSignInBtn;
    private Button mSignOutBtn, mRevokeBtn;
    private TextView mStatusTxt;

    private int mSignInProgress;
    private static final int STATE_SIGNED_IN = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;

    private static final int RC_SIGN_IN = 0;

    private static final int DIALOG_PLAY_SERVICES_ERROR = 0;

    private PendingIntent mSignInIntent;
    private int mSignInError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mSignInBtn = (SignInButton) findViewById(R.id.sign_in_btn);
        mSignOutBtn = (Button) findViewById(R.id.sign_out_btn);
        mRevokeBtn = (Button) findViewById(R.id.revoke_btn);
        mStatusTxt = (TextView) findViewById(R.id.status_txt);

        mSignInBtn.setOnClickListener(this);
        mSignOutBtn.setOnClickListener(this);
        mRevokeBtn.setOnClickListener(this);

        mApiClient = buildApiClient();
    }

    private GoogleApiClient buildApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(new Scope(Scopes.PROFILE))
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mApiClient.isConnected()) {
            mApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.i(TAG, "onConnected");

        mSignInBtn.setEnabled(false);
        mSignOutBtn.setEnabled(true);
        mRevokeBtn.setEnabled(true);

        mSignInProgress = STATE_SIGNED_IN;

        Person currentUser = Plus.PeopleApi.getCurrentPerson(mApiClient);
        mStatusTxt.setText(String.format("Signed In to G+ as %s", currentUser.getDisplayName()));

    }

    @Override
    public void onConnectionSuspended(int cause) {

        mApiClient.connect();
        Log.i(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

        Log.i(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());

        if(mSignInProgress != STATE_IN_PROGRESS){
            mSignInIntent = result.getResolution();
            mSignInError = result.getErrorCode();

            if(mSignInProgress == STATE_SIGN_IN){
                resolveSignInError();
            }
        }

        onSignedOut();

    }

    private void onSignedOut() {
        mSignInBtn.setEnabled(true);
        mSignOutBtn.setEnabled(false);
        mRevokeBtn.setEnabled(false);

        mStatusTxt.setText("Signed out");
    }

    private void resolveSignInError() {

        if(mSignInIntent != null){
            try{
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e){
                Log.i(TAG, "Sign in intent could not be sent: "
                + e.getLocalizedMessage());
                mSignInProgress = STATE_SIGN_IN;
            }
        } else {
            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }
    }

    @Override
    public void onClick(View v){

        if(!mApiClient.isConnecting()) {
            switch (v.getId()) {
                case R.id.sign_in_btn:
                    mStatusTxt.setText("Signing In");
                    resolveSignInError();
                    break;
                case R.id.sign_out_btn:
//                    mStatusTxt.setText("Disconnected");
                    Plus.AccountApi.clearDefaultAccount(mApiClient);
                    mApiClient.disconnect();
                    mApiClient.connect();
                    break;
                case R.id.revoke_btn:
//                    mStatusTxt.setText("Revoked");
                    Plus.AccountApi.clearDefaultAccount(mApiClient);
                    Plus.AccountApi.revokeAccessAndDisconnect(mApiClient);
                    mApiClient = buildApiClient();
                    mApiClient.connect();
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK ){
                mSignInProgress = STATE_SIGN_IN;
            } else {
                mSignInProgress = STATE_SIGNED_IN;
            }

            if(!mApiClient.isConnecting()){
                mApiClient.connect();
            }
        }
    }
}
