package com.example.hellodrone;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ApplicationErrorReport;
import android.os.Bundle;
import android.widget.TextView;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo;
import com.parrot.drone.groundsdk.facility.AutoConnection;

public class MainActivity extends AppCompatActivity {

    // GroundSDK Instance
    private GroundSdk groundSdk;

    // Current Drone Instance
    private Drone drone;

    // Reference to the current Drone State
    private Ref<DeviceState> droneStateRef;

    // Reference to the current Drone Battery Level
    private Ref<BatteryInfo> droneBatteryInfoRef;

    // Drone State TextView
    private TextView droneStateTxt;

    // Drone Battery Level TextView
    private TextView droneBatteryTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get user interface instances
        droneStateTxt = findViewById(R.id.droneStateTxt);
        droneBatteryTxt = findViewById(R.id.droneBatteryTxt);

        // Initialise user interface default values
        droneStateTxt.setText(DeviceState.ConnectionState.DISCONNECTED.toString());

        // Get the GroundSDK Session
        groundSdk = ManagedGroundSdk.obtainSession(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Monitor the auto connection facility
        groundSdk.getFacility(AutoConnection.class, new Ref.Observer<AutoConnection>() {
            @Override
            public void onChanged(@Nullable AutoConnection autoConnection) {
                if (autoConnection != null) {
                    // Start auto connection
                    if (autoConnection.getStatus() != AutoConnection.Status.STARTED) {
                        autoConnection.start();
                    }
                    // If the drone has changed
                    if (MainActivity.this.drone != null) {
                        if (!MainActivity.this.drone.getUid().equals(autoConnection.getDrone().getUid())) {
                            MainActivity.this.stopDroneMonitors();
                            resetDroneUi();
                        }
                    }
                    // Monitor the new drone
                    if (MainActivity.this.drone != null) {
                        MainActivity.this.drone = autoConnection.getDrone();
                        MainActivity.this.startDroneMonitors();
                    }
                }
            }
        });
    }

    /*  Start the drone monitors  */
    private void startDroneMonitors() {
        monitorDroneState();
        monitorDroneBatteryLevel();
    }

    /*  Stop the drone monitors  */
    private void stopDroneMonitors() {
        droneStateRef.close();
        droneStateRef = null;

        droneBatteryInfoRef.close();
        droneBatteryInfoRef = null;
    }

    private void resetDroneUi() {
        droneStateTxt.setText(DeviceState.ConnectionState.DISCONNECTED.toString());
        droneBatteryTxt.setText("-%");
    }

    /*  Monitor current drone state  */
    private void monitorDroneState() {
        droneStateRef = drone.getState(new Ref.Observer<DeviceState>() {
            @Override
            public void onChanged(@Nullable DeviceState deviceState) {
                assert deviceState != null;
                droneStateTxt.setText(deviceState.getConnectionState().toString());
            }
        });
    }

    /*  Monitors current drone battery level  */
    private void monitorDroneBatteryLevel() {
        droneBatteryInfoRef = drone.getInstrument(BatteryInfo.class, new Ref.Observer<BatteryInfo>() {
            @Override
            public void onChanged(@Nullable BatteryInfo batteryInfo) {
                assert batteryInfo != null;
                droneBatteryTxt.setText(batteryInfo.getBatteryLevel() + "%");
            }
        });
    }
}