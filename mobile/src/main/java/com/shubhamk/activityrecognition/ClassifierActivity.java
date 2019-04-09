package com.shubhamk.activityrecognition;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ClassifierActivity extends AppCompatActivity {
    int file_name_index;
    private static List<Float> a_x = new ArrayList<Float>();
    private static List<Float> a_y = new ArrayList<Float>();
    private static List<Float> a_z = new ArrayList<Float>();
    private static List<Float> input_signal = new ArrayList<Float>();
    float[] output = new float[4];
    File csvFile;
    String line = "";
    String cvsSplitBy = ",";
    private ModelInterface modelInterface;
    private ProgressBar progressBar;
    ListView listView;
    ArrayList<String> ans = new ArrayList<>();
    String label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier);
        progressBar = findViewById(R.id.progress);
        listView = findViewById(R.id.listView_ans);
        Intent intent = getIntent();
        file_name_index = intent.getIntExtra("file_name_index", 0);
        String path = Environment.getExternalStorageDirectory().toString() + "/Activity Recognition";
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        csvFile = files[file_name_index];
        modelInterface = new ModelInterface(getApplicationContext());
        a_x.clear();
        a_y.clear();
        a_z.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            int t = 0;
            while ((line = br.readLine()) != null) {
                if (t != 0) {
                    String[] csv_line = line.split(cvsSplitBy);
                    label = csv_line[5];
                    a_x.add(Float.valueOf(csv_line[1]));
                    a_y.add(Float.valueOf(csv_line[2]));
                    a_z.add(Float.valueOf(csv_line[3]));
                }
                t++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        progressBar.setVisibility(View.VISIBLE);
        normalize();
        int total_batch = a_x.size() / 90;
        for (int i = 0; i < total_batch; i++) {
            input_signal.clear();
            for (int j = 0; j < 90; j++) {
                input_signal.add(round(a_x.get(i + j), 2));
            }
            for (int j = 0; j < 90; j++) {
                input_signal.add(round(a_y.get(i + j), 2));
            }
            for (int j = 0; j < 90; j++) {
                input_signal.add(round(a_z.get(i + j), 2));
            }

            float[] results = modelInterface.getActivityProb(toFloatArray(input_signal));
            if (i == 0) {
                for (int j = 0; j < 4; j++) {
                    output[j] = results[j];
                }
            } else {
                for (int j = 0; j < 4; j++) {
                    output[j] += results[j];
                }
                Log.v("nnn", "as");
            }

        }

        ans.clear();
        for (int j = 0; j < 4; j++) {
            output[j] = output[j] / total_batch;
        }

        ans.add("Jogging: " + String.valueOf(output[0]));
        ans.add("Sitting: " + String.valueOf(output[1]));
        ans.add("Standing: " + String.valueOf(output[2]));
        ans.add("Walking: " + String.valueOf(output[3]));

        progressBar.setVisibility(View.INVISIBLE);

        listView.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                ans) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setCompoundDrawablePadding(10);
                return view;
            }
        });

    }

    private void normalize()
    {
        float sum_x = 0.0f, standardDeviation_x = 0.0f;
        float sum_y = 0.0f, standardDeviation_y = 0.0f;
        float sum_z = 0.0f, standardDeviation_z = 0.0f;
        for (int i = 0;i <a_x.size();i++){
            sum_x += a_x.get(i);
            sum_y += a_y.get(i);
            sum_z += a_z.get(i);
        }
        float x_m = sum_x / a_x.size();
        float y_m = sum_y / a_y.size() ;
        float z_m = sum_z / a_z.size();
        for(int i = 0; i < a_x.size();i++) {
            standardDeviation_x += Math.pow(a_x.get(i) - x_m, 2);
            standardDeviation_y += Math.pow(a_y.get(i) - y_m, 2);
            standardDeviation_z += Math.pow(a_z.get(i) - z_m, 2);
        }
        float x_s = (float) Math.sqrt(standardDeviation_x/a_x.size());
        float y_s = (float) Math.sqrt(standardDeviation_y/a_y.size());
        float z_s = (float) Math.sqrt(standardDeviation_z/a_z.size());


        for(int i = 0; i < a_x.size(); i++)
        {
            a_x.set(i,((a_x.get(i) - x_m)/x_s));
            a_y.set(i,((a_y.get(i) - y_m)/y_s));
            a_z.set(i,((a_z.get(i) - z_m)/z_s));
        }
    }

    private float[] toFloatArray(List<Float> list)
    {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
}
