package uk.org.potentialdifference.stillapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class ShowActivity extends AppCompatActivity {

    private static String TAG = "ShowActivity";
    WebSocketClient mWebSocketClient;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        imageView = (ImageView) findViewById(R.id.imageView);

        setupWebSockets();
    }

    protected void setupWebSockets() {
        URI uri;
        try {
            uri = new URI("wss://192.168.0.6:8081");
        } catch (URISyntaxException e) {
            Log.d(TAG, "error creating URI");
            e.printStackTrace();
            return;
        }

        Log.d(TAG, "creating websocket client...");
        mWebSocketClient = new WebSocketClient(uri, new Draft_10()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i(TAG, "Websocket onOpen called");
            }

            @Override
            public void onMessage(String s) {
                Log.d(TAG, "websocket message " + s);
                try {
                    Log.d(TAG, "message received: " + s);
                    JSONObject jsonObj = new JSONObject(s);
                    String message = jsonObj.getString("message");
                    Log.d(TAG, "got message type: " + message);
                    if (message.equals("displayImage")) {
                        Log.d(TAG, "setting new display image");
                        String path = jsonObj.getString("path");
                        final String uri = "http://192.168.0.6:8081/" + path;
                        final Uri imageUri = Uri.parse(uri);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Bitmap bmp = BitmapFactory.decodeStream(new java.net.URL(uri).openStream());
                                    imageView.setImageBitmap(bmp);
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage());
                                }
                            }
                        });
                        Log.d(TAG, "setting new display image to " + uri);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "json exception: " + e.toString());
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        Log.d(TAG, "created websocket client");
        mWebSocketClient.connect();

    }

}
