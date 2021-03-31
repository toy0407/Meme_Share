package com.toy0407.practicememeshare;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MotionEventCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ImageView memeImageView;
    private ProgressBar bar;
    private OutputStream outputStream;
    private Drawable meme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        memeImageView = findViewById(R.id.memeImageView);
        bar = findViewById(R.id.memeProgressBar);
        requestMeme();


    }

    /* Next functions*/
    public void nextMeme(View view) {
        memeImageView.animate().alpha(0).start();
        requestMeme();
    }
    void requestMeme(){
        ProgressBar bar=findViewById(R.id.memeProgressBar);
        bar.setVisibility(View.VISIBLE);
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://meme-api.herokuapp.com/gimme";

        // Request a string response from the provided URL.
        JsonObjectRequest jsonObjectRequest =new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String url=response.getString("url");
                    Glide.with(MainActivity.this).load(url).into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            setImage(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, error -> Log.i("Failed","Failed"));

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }
    private void setImage(Drawable imageDrawable){
        memeImageView.setImageDrawable(imageDrawable);
        bar.setVisibility(View.GONE);
        memeImageView.animate().alpha(1).start();
        meme=imageDrawable;
    }


    /*Share functions*/
    public void shareMeme(View view) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) meme;
        Bitmap bitmap = bitmapDrawable.getBitmap();
        Uri uri=getImageUri(this,bitmap);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM,uri);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
    }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }


    /*Save functions*/
    public void saveMeme(View view) throws IOException {
        if (Build.VERSION.SDK_INT<=29) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                saveMemeToGallery();
            else
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        else {
            if (Environment.isExternalStorageManager()) saveMemeToGallery();
            else {
                Toast.makeText(getApplicationContext(),"Enable all files access permission to save memes",Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        }
    }
    private void saveMemeToGallery() throws IOException{
        BitmapDrawable bitmapDrawable = (BitmapDrawable) meme;
        Bitmap bitmap = bitmapDrawable.getBitmap();
        File filepath = Environment.getExternalStorageDirectory();
        File dir = new File(filepath.getAbsolutePath() + "/MemeShare/");
        dir.mkdir();
        File file = new File(dir, System.currentTimeMillis() + ".jpg");
        outputStream = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
        outputStream.flush();
        outputStream.close();
    }


    //Request Permission for storage
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==1){
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                try {
                    saveMemeToGallery();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                Toast.makeText(getApplicationContext(),"Permission Denied. Unable to save",Toast.LENGTH_SHORT).show();
            }
        }
    }


    /*Image Scaling*/

}