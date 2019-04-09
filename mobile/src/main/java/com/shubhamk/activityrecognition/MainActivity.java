package com.shubhamk.activityrecognition;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    protected Handler myHandler;
    int sentMessageNumber = 1;
    String fileName;
    File file = null;
    FirebaseStorage storage;
    StorageReference storageReference;
    ListView listView;
    ArrayList<String> file_name = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Snackbar snackbar = Snackbar.make(listView, "Device doesn't support Bluetooth", Snackbar.LENGTH_LONG);
            snackbar.show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        checkPermissions();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        listView = findViewById(R.id.listView);
        String path = Environment.getExternalStorageDirectory().toString()+"/Activity Recognition";
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            file_name.add(files[i].getName());
            Log.d("Files", "FileName:" + files[i].getName());
        }

        listView.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                file_name) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icons8_document_24, 0, 0, 0);
                view.setCompoundDrawablePadding(10);
                return view;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int q = position;
                final Intent intent = new Intent(MainActivity.this,ClassifierActivity.class);
                intent.putExtra("file_name_index", q );
                startActivity(intent);
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                alertBuilder.setTitle("Continue")
                        .setMessage("Are you sure you want to continue processing this file")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            }
        });
        myHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                messageText(stuff.getString("messageText"));
                return true;
            }
        });
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    public void messageText(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            //textView.append("\n" + newinfo);
        }
    }
    protected boolean checkPermissions() {
        int result;
        final List<String> listPermissionsNeeded = new ArrayList<>();
        final String[] PERMISSIONS = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        for (String p : PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat
                    .requestPermissions(this,
                            listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                            100);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String permissions[],
                                           @NonNull final int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //Local Receiver
    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Toast.makeText(getApplicationContext(),"Received message from watch at "+
                    String.valueOf(Calendar.getInstance().getTime().getTime()) ,Toast.LENGTH_SHORT).show();
            fileName = String.valueOf(Calendar.getInstance().getTime().getTime()) + ".csv";
            try {
                final File directory =
                        new File(Environment.getExternalStorageDirectory()
                                + "/Activity Recognition");
                if (!directory.exists()) {
                    if (!directory.mkdirs()) {
                        throw new FileNotFoundException("Could not create requested directory");
                    }
                }
                file = new File(directory, fileName);
                FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
                try {
                    fileWriter.append(message);
                } catch (Exception e) {
                    Log.v("Error", e.toString());
                } finally {
                    try {
                        fileWriter.flush();
                        fileWriter.close();
                        file_name.add(fileName);
                        listView.getAdapter().notify();
                    } catch (IOException e) {
                        System.out.println("Error while flushing/closing fileWriter !!!");
                        Log.v("Error", e.toString());
                    }
                }

            } catch (Exception e) {

            }

        }
    }

    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);
    }

    class NewThread extends Thread {
        String path;
        String message;
        NewThread(String p, String m) {
            path = p;
            message = m;
        }
        public void run() {
            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());
                    try {
                        Integer result = Tasks.await(sendMessageTask);
                        sendmessage("I just sent the wearable a message " + sentMessageNumber++);
                    }
                    catch (ExecutionException exception) {
                        Log.e("Error",exception.toString());
                    }
                    catch (InterruptedException exception) {
                        Log.e("InterruptedError",exception.toString());
                    }
                }
            }
            catch (ExecutionException exception) {
                Log.e("Error",exception.toString());
            }
            catch (InterruptedException exception) {
                Log.e("InterruptedError",exception.toString());
            }
        }
    }
}