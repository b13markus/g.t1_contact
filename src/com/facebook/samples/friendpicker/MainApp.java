/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.friendpicker;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.*;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class MainApp extends FragmentActivity {
    private static final int PICK_FRIENDS_ACTIVITY = 1;
    private Button pickFriendsButton;
    private Button login;
    private TextView resultsTextView;
    private UiLifecycleHelper lifecycleHelper;
    boolean pickFriendsWhenSessionOpened;
    public TextView getName;
    String facebookId;
    Button getInformanion;
    TextView userInfoTextView;
    String userFacebookId;
    private ProfilePictureView profilePictureView;
    DBHelper dbHelper;

    private boolean mLogged;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHandler = new Handler();

        dbHelper = new DBHelper(this);
        resultsTextView = (TextView) findViewById(R.id.resultsTextView);

        userInfoTextView = (TextView) findViewById(R.id.users_name);

        profilePictureView = (ProfilePictureView) findViewById(R.id.selection_profile_pic);

        pickFriendsButton = (Button) findViewById(R.id.pickFriendsButton);
        pickFriendsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickFriends();
            }
        });

        getInformanion = (Button) findViewById(R.id.getInfo);

        lifecycleHelper = new UiLifecycleHelper(this, new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                onSessionStateChanged(session, state, exception);
            }
        });
        lifecycleHelper.onCreate(savedInstanceState);

        getName = (TextView) findViewById(R.id.users_name);
        login = (Button) findViewById(R.id.login);

        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                logIn(!mLogged);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        displaySelectedFriends(RESULT_OK);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_FRIENDS_ACTIVITY:
                displaySelectedFriends(resultCode);
                break;
            default:
                Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
                makeMeRequest(Session.getActiveSession());
                break;
        }
    }

    private boolean ensureOpenSession() {
        if (Session.getActiveSession() == null ||
                !Session.getActiveSession().isOpened()) {
            Session.openActiveSession(this, true, new Session.StatusCallback() {
                @Override
                public void call(Session session, SessionState state, Exception exception) {
                    if (state == SessionState.OPENED) {
                        onSessionStateChanged(session, state, exception);
                        onLoggedIn();
                    } else {
                        onLoggedOut();
                    }
                }
            });
            return false;
        }
        return true;
    }

    private void onSessionStateChanged(Session session, SessionState state, Exception exception) {
        if (pickFriendsWhenSessionOpened && state.isOpened()) {
            pickFriendsWhenSessionOpened = false;
        }
        Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

            @Override
            public void onCompleted(GraphUser user, Response response) {
                if (user != null) {
                    // Display the parsed user info
                    userInfoTextView.setText(buildUserInfoDisplay(user));
                }
            }
        });
    }

    private void displaySelectedFriends(int resultCode) {
        String results = "";
        ListApp application = (ListApp) getApplication();

        Collection<GraphUser> selection = application.getSelectedUsers();
        if (selection != null && selection.size() > 0) {
            ArrayList<String> names = new ArrayList<String>();
            for (GraphUser user : selection) {
                names.add(user.getName());
            }
            results = TextUtils.join(", ", names);
        } else {
            results = "<No friends selected>";
        }
        resultsTextView.setText(results);
    }

    private void onClickPickFriends() {
        startPickFriendsActivity();
    }

    private void startPickFriendsActivity() {
        if (ensureOpenSession()) {
            Intent intent = new Intent(this, PickFriendsActivity.class);
            PickFriendsActivity.populateParameters(intent, null, true, true);
            startActivityForResult(intent, PICK_FRIENDS_ACTIVITY);
        } else {
            pickFriendsWhenSessionOpened = true;
        }
    }


    private void makeMeRequest(final Session session) {
        Request request = Request.newMeRequest(session,
                new Request.GraphUserCallback() {

                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        // If the response is successful
                        if (session == Session.getActiveSession()) {
                            if (user != null) {
                                facebookId = user.getUsername();
                            }
                        }
                        if (response.getError() != null) {
                            // Handle error
                        }
                    }
                }
        );
        request.executeAsync();
    }

    private String buildUserInfoDisplay(GraphUser user) {

        ContentValues cv = new ContentValues();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        StringBuilder userInfo = new StringBuilder("");

        userInfo.append(String.format("Name: %s\n\n",
                user.getName()));
        cv.put("name", user.getName());

        userInfo.append(String.format("Birthday: %s\n\n",
                user.getBirthday()));
        cv.put("Birthday", user.getBirthday());

        userInfo.append(String.format("Locale: %s\n\n",
                user.getProperty("locale")));
//        cv.put("Locale", user.getLocation().getCountry());

        userFacebookId = user.getId();


        URL img_value = null;
        try {
            img_value = new URL("http://graph.facebook.com/" + userFacebookId + "/picture?type=large");
            Bitmap mIcon1 = BitmapFactory.decodeStream(img_value.openConnection().getInputStream());
            profilePictureView.setProfileId(user.getId());
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("eer", "error");
        }
        Log.d(userFacebookId, " msg");

        db.insert("mytable", null, cv);

        return userInfo.toString();
    }

    private void logIn(boolean in) {
        if (in) {
            ensureOpenSession();
        } else {
            Session.getActiveSession().closeAndClearTokenInformation();
            onLoggedOut();
        }
    }

    private void onLoggedIn() {
        login.setText("logout");
        pickFriendsButton.setVisibility(View.VISIBLE);
        mLogged = true;
    }

    private void onLoggedOut() {
        login.setText("login");
        mLogged = false;
        profilePictureView.setStandartProfile();
        pickFriendsButton.setVisibility(View.INVISIBLE);

    }


}