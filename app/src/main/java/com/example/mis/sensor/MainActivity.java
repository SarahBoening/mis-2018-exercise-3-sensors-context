package com.example.mis.sensor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import static java.lang.String.format;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private double[] freqCounts;

    private int windowSize = 64;
    private int sampleSize = 64;
    private int sample = 0;
    private SensorManager sensorManager;
    private Sensor sensor;

    List<Double> magValues = new ArrayList<>();
    LineGraphSeries<DataPoint> lineX, lineY, lineZ, lineMag;
    GraphView graphViewAcc, graphViewFFT;
    TextView textWindow, textSample, textSpeed;
    SeekBar seekbarSample, seekbarWindow;

    int SAMPLE_RATE = 20000;

    MediaPlayer musicPlayerBike;
    MediaPlayer musicPlayerJog;


    LocationManager locationManager;
    double locationSpeed;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private Boolean mLocationPermissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialization of all components
        initSeekBars();
        initSensors();
        initAccGraph();
        initFFTGraph();
        initMusicPlayer();
        getLocationPermission();
        initLocation();

    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        double mag = magnitude(event.values[0], event.values[1], event.values[2]);
        magValues.add(mag);
        sample++;
        updateGraph(sample, event.values[0], event.values[1], event.values[2], mag);

    }

    @Override
    protected void onResume() {

        super.onResume();
        sensorManager.registerListener(this, sensor, SAMPLE_RATE);
        musicPlayerJog = MediaPlayer.create(this, R.raw.coldfunk);
        musicPlayerBike = MediaPlayer.create(this, R.raw.music);
    }

    @Override
    protected void onPause() {

        super.onPause();
        if (musicPlayerJog.isPlaying())
            musicPlayerJog.stop();
        if (musicPlayerBike.isPlaying())
            musicPlayerBike.stop();
        musicPlayerBike.release();
        musicPlayerJog.release();
        sensorManager.unregisterListener(this);
    }

    //Location permission on runtime
    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            textSpeed.setText("Current speed: no permission granted");
                            makeErrorLog("Location permission denied");
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;
                }
            }
        }
    }

    private void initSensors(){

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null)
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        else
            makeErrorLog("SensorManager not available");
        if (sensor == null)
            makeErrorLog("Accelerometer not available");
        else
            sensorManager.registerListener(this, sensor, SAMPLE_RATE);
    }

    private void initMusicPlayer(){

        musicPlayerBike = MediaPlayer.create(this, R.raw.coldfunk);
        musicPlayerBike.setLooping(true);
        musicPlayerJog = MediaPlayer.create(this, R.raw.music);
        musicPlayerJog.setLooping(true);
    }

    private void initLocation(){
        
        textSpeed = findViewById(R.id.textViewSpeed);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationPermissionsGranted) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                makeErrorLog("No permission for GPS");
                this.onLocationChanged(null);
                textSpeed.setText("Current speed: no permission granted");
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        }
     }

    // http://www.android-graphview.org/
    private void initAccGraph() {

        graphViewAcc = findViewById(R.id.graphViewAcc);
        
        graphViewAcc.setBackgroundColor(Color.WHITE);

        lineX = new LineGraphSeries<>();
        lineY = new LineGraphSeries<>();
        lineZ = new LineGraphSeries<>();
        lineMag = new LineGraphSeries<>();

        lineX.setColor(Color.RED);
        lineY.setColor(Color.GREEN);
        lineZ.setColor(Color.BLUE);
         // Set to gray in order to see it better
        lineMag.setColor(Color.GRAY);

        graphViewAcc.addSeries(lineX);
        graphViewAcc.addSeries(lineY);
        graphViewAcc.addSeries(lineZ);
        graphViewAcc.addSeries(lineMag);

        graphViewAcc.getGridLabelRenderer().setHorizontalAxisTitle("Sample");
        graphViewAcc.getGridLabelRenderer().setVerticalAxisTitle("Acceleration");
        graphViewAcc.getGridLabelRenderer().setHorizontalLabelsVisible(true);
        
        // add legend
        lineX.setTitle("x-axis");
        lineY.setTitle("y-axis");
        lineZ.setTitle("z-axis");
        lineMag.setTitle("Magnitude");
        graphViewAcc.getLegendRenderer().setVisible(true);
        graphViewAcc.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
       
        // set axes
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
        graphViewFFT.getViewport().setMaxX(windowSize + 5);

    }

    private void updateFFTGraphAxis() {

        graphViewFFT.getViewport().setMaxX(windowSize + 5);
    }

    private void initSeekBars() {

        seekbarSample = findViewById(R.id.seekbarSample);
        seekbarSample.setMax(1000000);
        seekbarSample.setProgress(0);
        seekbarSample.incrementProgressBy(10000);
        textSample = findViewById(R.id.textViewSample);

        seekbarWindow = findViewById(R.id.seekbarWindow);
        seekbarWindow.setProgress(64);
        seekbarWindow.setMax(1024);
        textWindow = findViewById(R.id.textViewWindow);

        seekbarSample.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // https://stackoverflow.com/questions/7329166/changing-step-values-in-seekbar
                progress /= 10000;
                progress *= 10000;
                String text = "Sample rate: " + progress + " ms";
                textSample.setText(text);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int progress = seekBar.getProgress();
                progress /= 10000;
                progress *= 10000;
                seekBar.setProgress(progress );

                String text = "Sample size: " + progress  + " ms";
                textSample.setText(text);
                SAMPLE_RATE = progress;
                updateSampleRate(progress);
            }
        });

        seekbarWindow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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
                windowSize = progress;
                updateFFTGraphAxis();
            }
        });
    }

    private void updateSampleRate(int rate){

        sensorManager.unregisterListener(this);
        sensorManager.registerListener(this, sensor, rate);
    }
    
    private void getFFTData() {

        // Array needs to be of the size of windowSize for FFT, so we need to wait for it to fill
        // If windowSize is reached, start FFT
        // If it's bigger then windowSize, fill again
        Double[] FFTValues = magValues.toArray(new Double[magValues.size()]);
        if(FFTValues.length == windowSize)
            new FFTAsynctask(windowSize).execute(ArrayUtils.toPrimitive(FFTValues));

        if (FFTValues.length > windowSize)
            magValues.clear();
    }

    // Got rid of the thread since it caused crashes, new solution: http://jjoe64.github.io/GraphView/javadoc/com/jjoe64/graphview/GraphView.html#onDataChanged-boolean-boolean-
    private void updateGraph(final int sample, final double x, final double y, final double z, final double mag) {

                lineX.appendData(new DataPoint(sample, x), true, 1000);
                lineY.appendData(new DataPoint(sample, y), true, 1000);
                lineZ.appendData(new DataPoint(sample, z), true, 1000);
                lineMag.appendData(new DataPoint(sample, mag), true, 1000);
                graphViewAcc.onDataChanged(true, true);
                getFFTData();
            }


    @Override
    public void onLocationChanged(Location location) {
        if (mLocationPermissionsGranted) {
            if (location == null) {
                textSpeed.setText("Current speed: 0.0 km/h");
                locationSpeed = 0.00;
            } else {
                // m/s -> km/h
                locationSpeed = location.getSpeed() * 3.6;
                String speedString = format("%.1f", locationSpeed);
                textSpeed.setText("Current speed: " + speedString + " km/h");
            }
        } else
            textSpeed.setText("Current speed: no permission granted");

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

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
            double[] imagPart = new double[this.wsize]; // init empty


            /*
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
                FFT fft = new FFT(this.wsize);
                fft.fft(realPart, imagPart);

            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[this.wsize];

            //fill array with magnitude values of the distribution
            for (int i = 0; this.wsize > i; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }
            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {

            //hand over values to global variable after background task is finished
            freqCounts = values;
            updateFFTGraph();
            changeMusicBySpeed();
        }
    }

    private void updateFFTGraph(){

        // Draw line graph for FFT results
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

    private void changeMusicBySpeed() {

        double temp = 0.00;
        for (int i = 1; i < freqCounts.length; i++) {
            if (freqCounts[i] >= temp)
                temp = freqCounts[i];
        }

        // change moving behaviour to standing/sitting still
        // See README.txt for more information
        if (temp < 20.00) {
            if (locationSpeed < 1.00 || !mLocationPermissionsGranted) {
                // no music playing
                if (musicPlayerJog.isPlaying())
                    musicPlayerJog.pause();
                if (musicPlayerBike.isPlaying())
                    musicPlayerBike.pause();
            }
            // bus/car no real movement of phone
            else if (locationSpeed > 25.00) {
                if (musicPlayerJog.isPlaying())
                    musicPlayerJog.pause();
                if (musicPlayerBike.isPlaying())
                    musicPlayerBike.pause();
            }
        }
        
        //Change in the moving behaviour
        if (temp > 20.00) {
            // Walking/Jogging
            if (locationSpeed >= 1.00 && locationSpeed <= 13.00 || !mLocationPermissionsGranted) {
                if (musicPlayerBike.isPlaying())
                    musicPlayerBike.pause();
                if (!musicPlayerJog.isPlaying())
                    musicPlayerJog.start();
            }

            // Riding a bike
           else if (locationSpeed >= 14.00 && locationSpeed <= 25.00) {
                if (musicPlayerJog.isPlaying())
                    musicPlayerJog.pause();
                if (musicPlayerBike.isPlaying())
                    musicPlayerBike.pause();
            }
            // Double check if moving in bus/car since there might be a lot of moving of the device (e.g. bumpy roads)
           else if (locationSpeed > 25.00) {
                if (musicPlayerJog.isPlaying())
                    musicPlayerJog.pause();
                if (musicPlayerBike.isPlaying())
                    musicPlayerBike.pause();
            }
        }
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
