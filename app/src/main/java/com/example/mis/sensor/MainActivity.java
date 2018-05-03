package com.example.mis.sensor;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //example variables
    private double[] rndAccExamplevalues;
    private double[] freqCounts;

    private SensorManager sensorManager;
    private Sensor sensor;
    double ax, ay, az, aMag;
    float time;
    LineGraphSeries<DataPoint> lineX, lineY, lineZ, lineMag;

    GraphView graphViewAcc, graphViewFFT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(sensor == null){
            makeErrorLog("Accelerometer not available");
        } else{
            graphViewAcc = findViewById(R.id.graphViewAcc);
            graphViewFFT = findViewById(R.id.graphViewFFT);

            graphViewAcc.setBackgroundColor(Color.GRAY);

            Viewport vp = graphViewAcc.getViewport();
            vp.setScalable(true);
            vp.setScalableY(true);
            vp.setScrollableY(true);
            vp.setScrollable(true);


            lineX = new LineGraphSeries<>();
            lineY = new LineGraphSeries<>();
            lineZ = new LineGraphSeries<>();
            lineMag = new LineGraphSeries<>();

            lineX.setColor(Color.RED);
            lineY.setColor(Color.GREEN);
            lineZ.setColor(Color.BLUE);
            lineMag.setColor(Color.WHITE);

            graphViewAcc.addSeries(lineX);
            graphViewAcc.addSeries(lineY);
            graphViewAcc.addSeries(lineZ);
            graphViewAcc.addSeries(lineMag);
/*
        //initiate and fill example array with random values
        rndAccExamplevalues = new double[64];
        randomFill(rndAccExamplevalues);
        new FFTAsynctask(64).execute(rndAccExamplevalues); */
    }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
          updateGraph(( event.timestamp/100000000), event.values[0], event.values[1], event.values[2], magnitude(event.values[0], event.values[1], event.values[2]));


    }
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */

    private void updateGraph(final long timestamp, final float x, final float y, final float z, final float mag){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lineX.appendData(new DataPoint(timestamp, x), true, 40);
                lineY.appendData(new DataPoint(timestamp, y), true, 40);
                lineZ.appendData(new DataPoint(timestamp, z), true, 40);
                lineMag.appendData(new DataPoint(timestamp, mag), true, 40);}
        });

    }
    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }

            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;
        }
    }

    /**
     * little helper function to fill example with random double values
     */
    public void randomFill(double[] array){
        Random rand = new Random();
        for(int i = 0; array.length > i; i++){
            array[i] = rand.nextDouble();
        }
    }

    public float magnitude(float x, float y, float z){
        return (float) Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z,2));
    }

    // Simple function to display and log errors/exceptions
    public void makeErrorLog(String error) {
        Log.e("MainActivity", error);
        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
    }

}
