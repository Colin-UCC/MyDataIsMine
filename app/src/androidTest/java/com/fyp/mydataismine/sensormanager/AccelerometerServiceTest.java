package com.fyp.mydataismine.sensormanager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class AccelerometerServiceTest {

    @Mock
    private SensorManager mockSensorManager;
    @Mock
    private Sensor mockAccelerometer;
    @Mock
    private SensorEvent mockSensorEvent;

    private AccelerometerService service;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = ApplicationProvider.getApplicationContext();
        service = new AccelerometerService() {
            @Override
            public void onCreate() {
                sensorManager = mockSensorManager;
                accelerometerSensor = mockAccelerometer;
                databaseHelper = new DatabaseHelper(context);
            }
        };
        service.onCreate();
    }

    @Test
    public void testInitialiseSensors() {
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockAccelerometer);
        service.initialiseSensors();
        verify(mockSensorManager).registerListener(service, mockAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Test
    public void testOnSensorChanged() {
        when(mockSensorEvent.sensor.getType()).thenReturn(Sensor.TYPE_ACCELEROMETER);
        when(mockSensorEvent.values).thenReturn(new float[]{0.5f, 1.0f, -0.5f});

        service.onSensorChanged(mockSensorEvent);
        verify(service.filterX, times(1)).addValue(0.5f);
        verify(service.filterY, times(1)).addValue(1.0f);
        verify(service.filterZ, times(1)).addValue(-0.5f);
    }
}
