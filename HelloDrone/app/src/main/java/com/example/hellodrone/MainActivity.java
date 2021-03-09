package com.example.hellodrone;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ApplicationErrorReport;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo;
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf;
import com.parrot.drone.groundsdk.facility.AutoConnection;

public class MainActivity extends AppCompatActivity {

    // GroundSDK Instance
    private GroundSdk groundSdk;

    // Current Drone Instance
    private Drone drone;

    // Reference to the current Drone State
    private Ref<DeviceState> droneStateRef = null;

    // Reference to the current Drone Battery Level
    private Ref<BatteryInfo> droneBatteryInfoRef = null;

    // Reference to a current Drone Piloting interface
    private Ref<ManualCopterPilotingItf> pilotingItfRef = null;

    // Drone State TextView
    private TextView droneStateTxt;

    // Drone Battery Level TextView
    private TextView droneBatteryTxt;

    // Take Off / Land Button
    private Button takeOffLandBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get user interface instances
        droneStateTxt = findViewById(R.id.droneStateTxt);
        droneBatteryTxt = findViewById(R.id.droneBatteryTxt);
        takeOffLandBtn = findViewById(R.id.takeOffLandBt);
        takeOffLandBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTakeOffLandCLick();
            }
        });

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
        monitorPilotingInterface();
    }

    /*  Stop the drone monitors  */
    private void stopDroneMonitors() {
        droneStateRef.close();
        droneStateRef = null;

        droneBatteryInfoRef.close();
        droneBatteryInfoRef = null;

        pilotingItfRef.close();
        pilotingItfRef = null;
    }

    private void resetDroneUi() {
        droneStateTxt.setText(DeviceState.ConnectionState.DISCONNECTED.toString());
        droneBatteryTxt.setText("-%");
        takeOffLandBtn.setEnabled(false);
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

    /*  Monitors current drone piloting interface  */
    private void monitorPilotingInterface() {
        pilotingItfRef = drone.getPilotingItf(ManualCopterPilotingItf.class, new Ref.Observer<ManualCopterPilotingItf>() {
            @Override
            public void onChanged(@Nullable ManualCopterPilotingItf manualCopterPilotingItf) {
                if (manualCopterPilotingItf == null)
                    takeOffLandBtn.setEnabled(false);
                else
                    managePilotingItfState(manualCopterPilotingItf);
            }
        });
    }

    /*  Called on Take Off / Land click  */
    private void onTakeOffLandCLick() {
        ManualCopterPilotingItf manualCopterPilotingItf = pilotingItfRef.get();
        assert manualCopterPilotingItf != null;

        if (manualCopterPilotingItf.canTakeOff())
            manualCopterPilotingItf.takeOff();
        else if (manualCopterPilotingItf.canLand())
            manualCopterPilotingItf.land();
    }

    /*  Manage piloting interface state
        @param itf :: the current piloting interface  */
    private void managePilotingItfState(ManualCopterPilotingItf itf) {
        switch (itf.getState()) {
            case IDLE:
                takeOffLandBtn.setEnabled(false);
                itf.activate();
                break;
            case ACTIVE:
                if (itf.canTakeOff()) {
                    takeOffLandBtn.setEnabled(true);
                    takeOffLandBtn.setText(getString(R.string.take_off));
                }
                else if (itf.canLand()) {
                    takeOffLandBtn.setEnabled(true);
                    takeOffLandBtn.setText(getString(R.string.land));
                }
                else
                    takeOffLandBtn.setEnabled(false);
                break;
            default:
                takeOffLandBtn.setEnabled(false);
                break;
        }
    }
}