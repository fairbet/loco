package android.duke290.com.loco.photos;

import android.content.Intent;
import android.duke290.com.loco.Creation;
import android.duke290.com.loco.R;
import android.duke290.com.loco.SharedLists;
import android.duke290.com.loco.User;
import android.duke290.com.loco.database.DatabaseFetch;
import android.duke290.com.loco.database.DatabaseFetchCallback;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class PhotosActivity extends AppCompatActivity implements DatabaseFetchCallback {
    public ImageAdapter mImage_adp;
    private GridView gridview;
    private String fetchtype;
    private String FETCHTYPE = "fetchtype";
    private String INDIVIDUAL = "individual";
    private String SHARED = "shared";
    private FirebaseAuth mAuth;
    public User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        //Setting the toolbar for the activity
        Toolbar myToolbar = (Toolbar) findViewById(R.id.photos_toolbar);
        myToolbar.setTitle("Photos");
        setSupportActionBar(myToolbar);
        gridview = (GridView) findViewById(R.id.grid_view);
        Intent intent = getIntent();
        fetchtype = intent.getStringExtra(FETCHTYPE);
        if(fetchtype.equals(SHARED)){
            ArrayList<Creation> imagecreations = new ArrayList<>();
            imagecreations = SharedLists.getInstance().getImageCreations();
            setImages(imagecreations);
        }
        if(fetchtype.equals(INDIVIDUAL)){
            DatabaseFetch databasefetch = new DatabaseFetch(this);
            databasefetch.fetchByUser();
        }

    }

    private void setImages(ArrayList<Creation> imagecreations){
        mImage_adp = new ImageAdapter(this, imagecreations);
        gridview.setAdapter(mImage_adp);
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // Show full-size image
                Creation storageplace = (Creation) mImage_adp.getItem(position); // replace with full-size image indatabase
                String path = storageplace.extra_storage_path;
                Intent fullsize = new Intent(PhotosActivity.this, PhotoFullSizeActivity.class);
                fullsize.putExtra("path", path);
                startActivity(fullsize);
            }
        });
    }

    @Override
    public void onDatabaseResultReceived(ArrayList<Creation> creations) {
        ArrayList<Creation> image_creation_list = new ArrayList<Creation>();
        for (Creation c : creations) {
            if (c.type.equals("image")) {
                image_creation_list.add(c);
            }
        }
        setImages(image_creation_list);
    }

    @Override
    public void onUserReceived(User user) {

    }
}