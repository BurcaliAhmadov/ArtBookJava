package com.burcaliahmadov.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.burcaliahmadov.artbook.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.sql.Statement;
import java.util.zip.InflaterOutputStream;

public class ArtActivity extends AppCompatActivity {
    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        resultLauncher();
        database=this.openOrCreateDatabase("ARTS",MODE_PRIVATE,null);
        Intent intent=getIntent();
        String info =intent.getStringExtra("info");
        if(info.matches("new")){
            binding.artText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.imageView.setImageResource(R.drawable.image);
            binding.saveButton.setVisibility(view.VISIBLE);

        }
        else{
            int artId=intent.getIntExtra("ArtID",0);
            binding.saveButton.setVisibility(view.INVISIBLE);
            try{
                Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id=?",new String[]{String.valueOf(artId)});
                int artIx=cursor.getColumnIndex("artname");
                int artistIx=cursor.getColumnIndex("artistname");
                int yearIx=cursor.getColumnIndex("year");
                int imageIx=cursor.getColumnIndex("image");
                while(cursor.moveToNext()){
                    binding.artText.setText(cursor.getString(artIx));
                    binding.artistText.setText(cursor.getString(artistIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes =cursor.getBlob(imageIx);
                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);

                }
                cursor.close();



            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public void selectImage(View view){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(ArtActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view ,"Permission nedded for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            }
            else{
                //request Permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

        }else{
            //Gallery
            Intent intentToGallery =new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }




    }
    public void save(View view ){
        String artName =binding.artText.getText().toString();
        String artisNAme=binding.artistText.getText().toString();
        String year= binding.yearText.getText().toString();

        Bitmap smallImage=makeSmallerImage(selectedImage,300);
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray =outputStream.toByteArray();
        //insert data
        try{

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)");
            String sqlString="INSERT INTO arts (artname,artistname,year,image) VALUES(?,?,?,?)";
            SQLiteStatement statement=database.compileStatement(sqlString);
            statement.bindString(1,artName);
            statement.bindString(2,artisNAme);
            statement.bindString(3,year);
            statement.bindBlob(4,byteArray);
            statement.execute();
        }catch (Exception e){
            e.printStackTrace();
        }
        Intent intent=new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);


    }
    public Bitmap makeSmallerImage (Bitmap image,int maximumSize){
        int height =image.getHeight();
        int width=image.getWidth();
        float bitmapRatio=(float)width/(float)height;
        if(bitmapRatio>1){
            //landscape
            width=maximumSize;
            height=(int)(width/bitmapRatio);
        }else{
            //portrait
            height=maximumSize;
            width=(int)(height*bitmapRatio);
        }

        return image.createScaledBitmap(image,width,height,true);
    }
    private void resultLauncher(){
        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()==RESULT_OK){
                    Intent intentFromResult=result.getData();
                    if(intentFromResult!=null){
                        Uri setImage=intentFromResult.getData();
                        try{
                            ImageDecoder.Source source=ImageDecoder.createSource(ArtActivity.this.getContentResolver(),setImage);
                            selectedImage= ImageDecoder.decodeBitmap(source);
                            binding.imageView.setImageBitmap(selectedImage);

                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }


                    }
                }
            }
        });

        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //permission granted
                    Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }else{//permission dined
                    Toast.makeText(ArtActivity.this,"Permission ",Toast.LENGTH_LONG).show();
                }
            }
        });
    }




}