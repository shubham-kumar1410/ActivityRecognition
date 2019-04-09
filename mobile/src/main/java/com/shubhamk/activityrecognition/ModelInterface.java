package com.shubhamk.activityrecognition;

import android.content.Context;
import android.content.res.AssetManager;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class ModelInterface {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    private static ModelInterface activityInferenceInstance;
    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE = "file:///android_asset/opti_output_graph.pb";
    private static final String INPUT_NODE = "input";
    private static final String[] OUTPUT_NODES = {"nn_output"};
    private static final String OUTPUT_NODE = "nn_output";
    private static final long[] INPUT_SIZE = {270};
    private static final int OUTPUT_SIZE = 4;
    private static AssetManager assetManager;

    public static ModelInterface getInstance(final Context context)
    {
        if (activityInferenceInstance == null)
        {
            activityInferenceInstance = new ModelInterface(context);
        }
        return activityInferenceInstance;
    }

    public ModelInterface(final Context context) {
        this.assetManager = context.getAssets();
        inferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_FILE);
    }

    public float[] getActivityProb(float[] input_signal)
    {
        float[] result = new float[OUTPUT_SIZE];
        inferenceInterface.feed(INPUT_NODE,input_signal,INPUT_SIZE);
        inferenceInterface.run(OUTPUT_NODES);
        inferenceInterface.fetch(OUTPUT_NODE,result);
        return result;
    }
}
