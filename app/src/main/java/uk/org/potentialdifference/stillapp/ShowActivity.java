package uk.org.potentialdifference.stillapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import uk.org.potentialdifference.stillapp.imageservice.ImageDownloadDelegate;
import uk.org.potentialdifference.stillapp.imageservice.ImageDownloadTask;

public class ShowActivity extends AppCompatActivity implements StillWebsocketDelegate {

    private static String TAG = "ShowActivity";
    WebSocketClient mWebSocketClient;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        //stop the screen from going to sleep:
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);





        imageView = (ImageView) findViewById(R.id.imageView);

        setupWebSockets();
    }

    protected void setupWebSockets() {
        URI uri;

        try {
            uri = new URI(String.format("wss://%s:%s",getString(R.string.still_server_hostname), getString(R.string.still_server_http_port)));
        } catch (URISyntaxException e) {
            Log.d(TAG, "error creating URI");
            e.printStackTrace();
            return;
        }

        Log.d(TAG, "creating websocket client...");
        mWebSocketClient = new StillWebsocketClient(this, uri, new Draft_10(), this);
        Log.d(TAG, "created websocket client");
        mWebSocketClient.connect();

    }


    @Override
    public void showImage(Bitmap image) {
        imageView.setImageBitmap(image);
    }

    @Override
    public void hideImage(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ColorDrawable cd = new ColorDrawable(Color.parseColor("#FF000000"));
                imageView.setImageDrawable(cd);
            }
        });

        }

        @Override
    public void exitShowMode() {
        //exit the activity
        finish();
    }
}
