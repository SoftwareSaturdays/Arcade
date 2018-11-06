package com.softwaresaturdays.app.arcade.networkHelpers;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.softwaresaturdays.app.arcade.MyApplication;
import com.softwaresaturdays.app.arcade.models.GifMessage;
import com.softwaresaturdays.app.arcade.models.Message;
import com.softwaresaturdays.app.arcade.models.TextMessage;
import com.softwaresaturdays.app.arcade.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class DatabaseHelper {

    public static final String KEY_USERS = "users";
    public static final String KEY_MESSAGES = "messages";
    public static final String KEY_GAMES = "games";
    public static final String KEY_SESSIONS = "sessions";
    private static final String TAG = "DATABASE_HELPER:";

    public static void uploadUserInfo(User user) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference colRef = db.collection(KEY_USERS);

        // Merge data to avoid overwriting fields such as registration fcm token
        colRef.document(user.getUid()).set(user, SetOptions.merge());
    }

    public static void uploadMessage(Message message) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference colRef = db.collection(KEY_MESSAGES);

        colRef.document(message.getTimestamp() + "").set(message);
    }

    public static void fetchMessages(int limit, final OnDatabaseFetchListener listener) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference colRef = db.collection(KEY_MESSAGES);


        colRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {

                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "Current data: " + queryDocumentSnapshots.size() + " messages");

                    ArrayList<Message> messages = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.get("type").equals(Message.TYPE_TEXT_MESSAGE)) {
                            try {
                                TextMessage text = documentSnapshot.toObject(TextMessage.class);
                                messages.add(0, text);
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                        } else if (documentSnapshot.get("type").equals(Message.TYPE_GIF_MESSAGE)) {
                            try {
                                GifMessage gif = documentSnapshot.toObject(GifMessage.class);
                                messages.add(0, gif);
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                        }
                    }

                    listener.onMessagesFetched(messages);

                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    public static void getUserInfo(String userId, final OnUserInfoFetchListener listener) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference docRef = db.collection("users").document(userId);

        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                try {
                    User user = documentSnapshot.toObject(User.class);
                    listener.onUserInfoFetched(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void getAllUsersInfo(final OnUserInfoFetchListener listener) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference colRef = db.collection(KEY_USERS);

        colRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                @javax.annotation.Nullable FirebaseFirestoreException e) {

                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "Current data: " + queryDocumentSnapshots.size() + " users");

                    ArrayList<User> users = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        try {
                            User user = documentSnapshot.toObject(User.class);
                            users.add(user);
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }

                    listener.onAllUsersInfoFetched(users);

                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    public static void deleteMessage(Message mSelectedMessage) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference colRef = db.collection(KEY_MESSAGES);
        colRef.document(mSelectedMessage.getTimestamp() + "").delete();
    }

    public static void updateGameHighScore(String game, int score) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference colRef = db.collection(KEY_GAMES);

//        HashMap<String, String> top1 = new HashMap<>();
//        top1.put("uid", MyApplication.currUser.getUid());
//        top1.put("score", score + "");
//
//        colRef.document(game).set(top1);
    }

    public static void initTurnBasedGame(final String gameTitle, final String hostCode, final Map<String, Object> data) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // delete any currently existing game of this type tied to this user
        db.collection(KEY_USERS).document(MyApplication.currUser.getUid()).collection(KEY_SESSIONS).document(gameTitle).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (!documentSnapshot.contains("code")) {
                    createTurnBasedGameSession(gameTitle, hostCode, data);
                    return;
                }
                final String currentGameCode = documentSnapshot.getString("code");

                db.collection(KEY_GAMES).document(gameTitle).collection(KEY_SESSIONS).document(currentGameCode).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        createTurnBasedGameSession(gameTitle, hostCode, data);
                    }
                });
            }
        });
    }

    private static void createTurnBasedGameSession(final String gameTitle, final String hostCode, final Map<String, Object> data) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // create game session
        db.collection(KEY_GAMES).document(gameTitle).collection(KEY_SESSIONS).document(hostCode).set(data);

        // tie new game to user's profile
        final Map<String, Object> session = new HashMap<>();
        session.put("code", hostCode);
        session.put("host", true);

        db.collection(KEY_USERS).document(MyApplication.currUser.getUid()).collection(KEY_SESSIONS).document(gameTitle).set(session, SetOptions.merge());
    }

    public interface OnDatabaseFetchListener {
        void onMessagesFetched(ArrayList<Message> messages);
    }

    public interface OnUserInfoFetchListener {
        void onUserInfoFetched(User user);

        void onAllUsersInfoFetched(ArrayList<User> allUsers);
    }
}
