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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import android.media.MediaPlayer;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.lang3.ArrayUtils;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private double[] freqCounts;

    private int wsize = 64;
    private int sampleSize = 64;
    private int sample = 0;
    private SensorManager sensorManager;
    private Sensor sensor;
    double time;
    List<Double> magValues = new ArrayList<>();
    LineGraphSeries<DataPoint> lineX, lineY, lineZ, lineMag;
    GraphView graphViewAcc, graphViewFFT;
    TextView textWindow, textSample, textSpeed;
    SeekBar seekbarSample, seekbarWindow;

    MediaPlayer musicPlayerBike;
    MediaPlayer musicPlayerJog;

    double maxFreq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSeekBars();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if(sensor == null){
            makeErrorLog("Accelerometer not available");
        } else{
            initAccGraph();
            initFFTGraph();
            textSpeed = findViewById(R.id.textViewSpeed);
            textSpeed.setText("Current speed: 0 km/h");
            musicPlayerBike = MediaPlayer.create(this, R.raw.coldfunk);
            musicPlayerBike.setLooping(true);
            musicPlayerJog = MediaPlayer.create(this, R.raw.music);
            musicPlayerJog.setLooping(true);
            //musicPlayer.start();
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        double mag = magnitude(event.values[0], event.values[1], event.values[2]);
        magValues.add(mag);
        time = (event.timestamp / 100000000.0);
        sample++;
        updateGraph(sample, event.values[0], event.values[1], event.values[2], mag);


    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        musicPlayerJog = MediaPlayer.create(this, R.raw.coldfunk);
        musicPlayerBike = MediaPlayer.create(this, R.raw.music);
    }

    @Override
    protected void onPause() {
        super.onPause();
        musicPlayerJog.stop();
        musicPlayerBike.stop();
        musicPlayerBike.release();
        musicPlayerJog.release();
        sensorManager.unregisterListener(this);
    }


    // http://www.android-graphview.org/
    private void initAccGraph() {

        graphViewAcc = findViewById(R.id.graphViewAcc);

        // Set to gray in order to see the white line
        graphViewAcc.setBackgroundColor(Color.WHITE);

        lineX = new LineGraphSeries<>();
        lineY = new LineGraphSeries<>();
        lineZ = new LineGraphSeries<>();
        lineMag = new LineGraphSeries<>();

        lineX.setColor(Color.RED);
        lineY.setColor(Color.GREEN);
        lineZ.setColor(Color.BLUE);
        lineMag.setColor(Color.GRAY);

        graphViewAcc.addSeries(lineX);
        graphViewAcc.addSeries(lineY);
        graphViewAcc.addSeries(lineZ);
        graphViewAcc.addSeries(lineMag);

        graphViewAcc.getGridLabelRenderer().setHorizontalAxisTitle("Sample");
        graphViewAcc.getGridLabelRenderer().setVerticalAxisTitle("Acceleration");
        graphViewAcc.getGridLabelRenderer().setHorizontalLabelsVisible(true);

        lineX.setTitle("x-axis");
        lineY.setTitle("y-axis");
        lineZ.setTitle("z-axis");
        lineMag.setTitle("Magnitude");
        graphViewAcc.getLegendRenderer().setVisible(true);
        graphViewAcc.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        graphViewAcc.getViewport().setScalable(true);
        graphViewAcc.getViewport().setScalableY(true);
        graphViewAcc.getViewport().setScrollableY(true);
        graphViewAcc.getViewport().setScrollable(true);
        graphViewAcc.getViewport().setXAxisBoundsManual(true);
        graphViewAcc.getViewport().setMinX(0);
        graphViewAcc.getViewport().setMaxX(sampleSize);


    }

    private void initFFTGraph() {

        graphViewFFT = findViewById(R.id.graphViewFFT);

        graphViewFFT.setBackgroundColor(Color.WHITE);

        graphViewFFT.getViewport().setScalable(true);
        graphViewFFT.getViewport().setScalableY(true);
        graphViewFFT.getViewport().setScrollableY(true);
        graphViewFFT.getViewport().setScrollable(true);

        graphViewFFT.getGridLabelRenderer().setHorizontalAxisTitle("Sample");
        graphViewFFT.getGridLabelRenderer().setVerticalAxisTitle("Magnitude");
        graphViewFFT.getGridLabelRenderer().setHorizontalLabelsVisible(true);

        graphViewFFT.getViewport().setXAxisBoundsManual(true);
        graphViewFFT.getViewport().setMinX(0);
        graphViewFFT.getViewport().setMaxX(wsize + 5);

    }

    private void updateAccGraphAxis() {
        graphViewAcc.getViewport().setMaxX(sampleSize + 5);

    }

    private void updateFFTGraphAxis() {
        graphViewFFT.getViewport().setMaxX(wsize + 5);
    }

    private void initSeekBars() {
        seekbarSample = findViewById(R.id.seekbarSample);
        seekbarSample.setProgress(64);
        seekbarSample.setMax(1024);
        textSample = findViewById(R.id.textViewSample);
        textSample.setText("Sample size: 64");

        seekbarWindow = findViewById(R.id.seekbarWindow);
        seekbarWindow.setProgress(64);
        seekbarWindow.setMax(1024);
        textWindow = findViewById(R.id.textViewWindow);
        textWindow.setText("Window size: 64");

        seekbarSample.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                String text = "Sample size: " + progress;
                textSample.setText(text);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int progress = seekBar.getProgress();

                if (progress <= 64)
                    progress = 64;
                else if (progress <= 128)
                    progress = 128;
                else if (progress <= 256)
                    progress = 256;
                else if (progress <= 512)
                    progress = 512;
                else if (progress <= 1024)
                    progress = 1024;
                else
                    progress = 1024;

                seekBar.setProgress(progress);

                String text = "Sample size: " + progress;
                textSample.setText(text);
                sampleSize = progress;
                updateAccGraphAxis();


            }
        });

        seekbarWindow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String text = "Window size: " + progress;
                textWindow.setText(text);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();

                if (progress <= 64)
                    progress = 64;
                else if (progress <= 128)
                    progress = 128;
                else if (progress <= 256)
                    progress = 256;
                else if (progress <= 512)
                    progress = 512;
                else if (progress <= 1024)
                    progress = 1024;
                else
                    progress = 1024;

                seekBar.setProgress(progress);
                String text = "Window size: " + progress;
                textWindow.setText(text);
                wsize = progress;
                updateFFTGraphAxis();
            }
        });
    }

    private void getFFTData() {

        Double[] FFTvalues = magValues.toArray(new Double[magValues.size()]);
        if (FFTvalues.length >= sampleSize)
            new FFTAsynctask(wsize).execute(ArrayUtils.toPrimitive(FFTvalues));
        if (FFTvalues.length > sampleSize)
            magValues.clear();
    }

    // https://stackoverflow.com/questions/33215733/graphview-shows-wrong-graph-in-real-time-mode-when-scrolltoend-is-true
    private void updateGraph(final int sample, final double x, final double y, final double z, final double mag) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //magValues.add(mag);
                getFFTData();
                lineX.appendData(new DataPoint(sample, x), true, 1000);
                lineY.appendData(new DataPoint(sample, y), true, 1000);
                lineZ.appendData(new DataPoint(sample, z), true, 1000);
                lineMag.appendData(new DataPoint(sample, mag), true, 1000);
            }
        });

    }
    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */
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
            for (int i = 0; wsize > i; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }
            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;
            updateFFTGraph();
            playMusicBySpeed();
        }
    }

    private void updateFFTGraph(){

        DataPoint[] dataPoints = new DataPoint[freqCounts.length - 1];
        for (int i = 0; i < freqCounts.length - 1; i++)
            dataPoints[i] = new DataPoint(i, freqCounts[i + 1]);
        LineGraphSeries<DataPoint> fft = new LineGraphSeries<>(dataPoints);
        fft.setColor(Color.YELLOW);
        graphViewFFT.removeAllSeries();
        graphViewFFT.addSeries(fft);
        fft.setTitle("Magnitude");
        graphViewFFT.getLegendRenderer().setVisible(true);
        graphViewFFT.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
    }

    private void playMusicBySpeed(){
        double temp = 0.00;
        for (double freq : freqCounts){
            if(freq >= temp)
                temp = freq;
        }
        //TODO thresholds
    }

    private void playMusic(){

    }
    private float magnitude(float x, float y, float z) {
        return (float) Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z,2));
    }

    // Simple function to display and log errors/exceptions
    public void makeErrorLog(String error) {
        Log.e("MainActivity", error);
        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
    }

}
