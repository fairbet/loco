package android.duke290.com.loco;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CloudStorageService extends IntentService {

    protected String mActionType; // either "upload" or "download"
    protected ResultReceiver mReceiver;
    protected InputStream mLocalStream;
    protected String mStoragePath;
    protected String mContentType;

    StorageReference mStorageRef;
    StorageReference mDestinationRef;
    String TAG = "CloudStorageService";

    protected InputStream mDownloadedStream;
    protected String mDownloadedContentType;

    byte[] dwnld_b_ar;
    int writing_complete = 0;
    String stg_filename = null;

    public CloudStorageService() {
        super("CloudStorageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "onHandleIntent called");

        mActionType = intent.getStringExtra("CLOUD_STORAGE_OPTION");
        mReceiver = intent.getParcelableExtra("CLOUD_STORAGE_RECEIVER");
        if (intent.getByteArrayExtra("CLOUD_STORAGE_LOCAL_BYTE_ARRAY") != null) {
            mLocalStream = new ByteArrayInputStream(
                    intent.getByteArrayExtra("CLOUD_STORAGE_LOCAL_BYTE_ARRAY"));
        }
        mStoragePath = intent.getStringExtra("CLOUD_STORAGE_STORAGE_PATH");
        mContentType = intent.getStringExtra("CLOUD_STORAGE_CONTENT_TYPE");

        FirebaseStorage storage = FirebaseStorage.getInstance();

        mStorageRef = storage.getReference();

        mDestinationRef = mStorageRef.child(mStoragePath);

        if (mActionType.equals("upload")) {
            uploadFile();
        } else if (mActionType.equals("download")) {
            downloadFile();
        } else {
            deliverResultToReceiver(Constants.FAILURE_RESULT,
                    "No command to upload or download");
        }


    }

    protected void uploadFile() {

        // add metadata (content type)
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(mContentType)
                .build();

        UploadTask uploadTask = mDestinationRef.putStream(mLocalStream, metadata);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.d(TAG, "Upload failed");
                deliverResultToReceiver(Constants.FAILURE_RESULT, "Upload failed.");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "Upload successful!");
                deliverResultToReceiver(Constants.SUCCESS_RESULT, "Upload successful!");
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                @SuppressWarnings("VisibleForTests")
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d(TAG, "Upload is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "Upload is paused");
            }
        });
    }

    protected void downloadFile() {
        mDestinationRef.getStream().addOnSuccessListener(new OnSuccessListener<StreamDownloadTask.TaskSnapshot>() {
            @Override @SuppressWarnings("VisibleForTests")
            public void onSuccess(StreamDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                Log.d(TAG, "Download of file successful!");
                if (mDownloadedStream != null) {
                    Log.d(TAG, "mDownloadedStream already being used");
                }
                mDownloadedStream = taskSnapshot.getStream();

                Log.d(TAG, "Downloading file metadata");
                downloadFileContentType(taskSnapshot.getStream());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.d(TAG, "Download failed");
                deliverResultToReceiver(Constants.FAILURE_RESULT, "Download failed.");
            }
        });
    }

    protected void downloadFileContentType(final InputStream downloadedStream) {
        mDestinationRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                mDownloadedContentType = storageMetadata.getContentType();
//                String downloadedContentType = storageMetadata.getContentType();

                Log.d(TAG, "Download file and metadata successful");
                deliverResultToReceiver(Constants.SUCCESS_RESULT,
                        "Download completely successful!");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Download metadata failed");

                deliverResultToReceiver(Constants.FAILURE_RESULT,
                        "Download of file successful but download of metadata failed");
            }
        });
    }

    private void deliverResultToReceiver(int resultCode,
                                         String process_message) {
        if (mDownloadedStream != null && mActionType.equals("download")) {
            stg_filename = "stg" + System.currentTimeMillis();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
//                    try {
//                        dwnld_b_ar = IOUtils.toByteArray(mDownloadedStream);
//                        Log.d(TAG, "size of dwnld_b_ar = " + dwnld_b_ar.length);
//                        byte_array_conversion_complete = 1;
//                    } catch (IOException e) {
//                        Log.d(TAG, "IOException when converting downloaded input stream to byte array");
//                    }

//                    try {
//                        mDownloadedStream.();
//                    } catch (IOException e) {
//                        Log.d(TAG, "resetting downloaded stream failed: " + e.getMessage());
//                    }

                    Context context = getApplicationContext();
                    File dir = context.getDir("downloaded_storage_items", Context.MODE_PRIVATE);

                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    File stg_file = new File(context.getDir("downloaded_storage_items", Context.MODE_PRIVATE),
                            stg_filename);

                    Log.d(TAG, "writing to : " + stg_file.getPath());

                    try {
                            FileOutputStream os = new FileOutputStream(stg_file);

                            byte[] buffer = new byte[1024];
                            int len = mDownloadedStream.read(buffer);
                            Log.d(TAG, "len = " + len);
                            while (len >= 0) {
                                os.write(buffer, 0, len);
                                Log.d(TAG, "writing");
                                len = mDownloadedStream.read(buffer);
                            }

                            os.close();
                            os.flush();
                            os.close();

                            Log.d(TAG, "outputstream written to");
                            writing_complete = 1;
                            Log.d(TAG, "Write to internal storage done");
                    } catch (IOException e) {
                        Log.d(TAG, "IOException while writing to outputstream: " + e.getMessage());
                    }

//                    try {
//                        mDownloadedStream.close();
//                        os.close();
//                    } catch (IOException e) {
//                        Log.d(TAG, "2: IOException when writing inputstream to outputstream");
//                    }
                }
            });

            while (writing_complete == 0) {
//                // for some reason, putting a statement here fixed a problem where the app
//                // would hang up and seem to get stuck even after completing byte array conversion
                Log.d(TAG, "writing to internal storage");
            }

        }

        writing_complete = 0;
        Bundle bundle = new Bundle();
        bundle.putString("CLOUD_PROCESS_MSG_KEY", process_message);
//        bundle.putByteArray("CLOUD_DOWNLOADED_BYTE_ARRAY_KEY", dwnld_b_ar);
        bundle.putString("CLOUD_DOWNLOADED_FILENAME", stg_filename);
        bundle.putString("CLOUD_DOWNLOADED_CONTENT_TYPE", mDownloadedContentType);
        mReceiver.send(resultCode, bundle);

        Log.d(TAG, "receiver sent");

    }

}
