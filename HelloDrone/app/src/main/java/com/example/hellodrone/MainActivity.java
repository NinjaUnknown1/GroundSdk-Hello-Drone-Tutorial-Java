package com.example.hellodrone;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf;
import com.parrot.drone.groundsdk.facility.AutoConnection;
import com.parrot.drone.groundsdk.stream.GsdkStreamView;

public class MainActivity extends AppCompatActivity {

    // GroundSDK Instance
    private GroundSdk groundSdk;

    // Current Drone Instance
    private Drone drone;

    // Current Drone Livestream
    private CameraLive livestream = null;

    // Current Remote Control Instance
    private RemoteControl rc = null;

    // Reference to the current Drone State
    private Ref<DeviceState> droneStateRef = null;

    // Reference to the current Drone Battery Level
    private Ref<BatteryInfo> droneBatteryInfoRef = null;

    // Reference to a current Drone Piloting interface
    private Ref<ManualCopterPilotingItf> pilotingItfRef = null;

    // Reference to the current Drone Stream Server Peripheral
    private Ref<StreamServer> streamServerRef = null;

    // Reference to the current Drone Livestream
    private Ref<CameraLive> liveStreamRef = null;

    // Reference to the current remote control state
    private Ref<DeviceState> rcStateRef = null;

    // Reference to the current remote control battery info instrument
    private Ref<BatteryInfo> rcBatteryInfoRef = null;

    // Drone State TextView
    private TextView droneStateTxt;

    // Drone Battery Level TextView
    private TextView droneBatteryTxt;

    // RC State TextView
    private TextView rcStateTxt;

    // RC Battery Level TextView
    private TextView rcBatteryTxt;

    // Take Off / Land Button
    private Button takeOffLandBtn;

    // Video Stream View
    private GsdkStreamView streamView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get user interface instances
        droneStateTxt = findViewById(R.id.droneStateTxt);
        droneBatteryTxt = findViewById(R.id.droneBatteryTxt);
        streamView = findViewById(R.id.streamView);
        rcStateTxt = findViewById(R.id.rcStateTxt);
        rcBatteryTxt = findViewById(R.id.rcBatteryTxt);
        takeOffLandBtn = findViewById(R.id.takeOffLandBt);
        takeOffLandBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTakeOffLandCLick();
            }
        });

        // Initialise user interface default values
        droneStateTxt.setText(DeviceState.ConnectionState.DISCONNECTED.toString());
        rcStateTxt.setText(DeviceState.ConnectionState.DISCONNECTED.toString());

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
                    MainActivity.this.drone = autoConnection.getDrone();
                    if (MainActivity.this.drone != null) {
                        MainActivity.this.startDroneMonitors();
                    }

                    // If the remote control has changed
                    if (MainActivity.this.rc != null) {
                        if (!MainActivity.this.rc.getUid().equals(autoConnection.getRemoteControl().getUid())) {
                            // Stop monitoring the old remote
                            stopRcMonitors();

                            // Reset Remote User Interface
                            resetRcUi();
                        }
                    }

                    // Monitor the new remote
                    MainActivity.this.rc = autoConnection.getRemoteControl();
                    if (MainActivity.this.rc != null) {
                        startRcMonitors();
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
        startVideoStream();
    }

    /*  Stop the drone monitors  */
    private void stopDroneMonitors() {
        droneStateRef.close();
        droneStateRef = null;

        droneBatteryInfoRef.close();
        droneBatteryInfoRef = null;

        pilotingItfRef.close();
        pilotingItfRef = null;

        liveStreamRef.close();
        liveStreamRef = null;

        streamServerRef.close();
        streamServerRef = null;

        livestream = null;
    }

    /*  Start the remote control monitors  */
    private void startRcMonitors() {
        monitorRcState();
        monitorRcBatteryLevel();
    }

    /*  Stop the remote control monitors  */
    private void stopRcMonitors() {
        rcStateRef.close();
        rcStateRef = null;

        rcBatteryInfoRef.close();
        rcBatteryInfoRef = null;
    }

    /*  Reset Drone UI  */
    private void resetDroneUi() {
        droneStateTxt.setText(DeviceState.ConnectionState.DISCONNECTED.toString());
        droneBatteryTxt.setText("-%");
        takeOffLandBtn.setEnabled(false);
        streamView.setStream(null);
    }

    /*  Reset RC UI  */
    private void resetRcUi() {
        rcStateTxt.setText(DeviceState.ConnectionState.DISCONNECTED.toString());
        rcBatteryTxt.setText("-%");
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

    /*  Monitor current Remote Control State  */
    private void monitorRcState() {
        rcStateRef = rc.getState(new Ref.Observer<DeviceState>() {
            @Override
            public void onChanged(@Nullable DeviceState deviceState) {
                assert deviceState != null;
                rcStateTxt.setText(deviceState.getConnectionState().toString());
            }
        });
    }

    /*  Monitor current Remote Control Battery Level  */
    private void monitorRcBatteryLevel() {
        rcBatteryInfoRef = rc.getInstrument(BatteryInfo.class, new Ref.Observer<BatteryInfo>() {
            @Override
            public void onChanged(@Nullable BatteryInfo batteryInfo) {
                assert batteryInfo != null;
                rcBatteryTxt.setText(batteryInfo.getBatteryLevel() + "%");
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

    /*  Starts the Video Stream  */
    private void startVideoStream() {
        // Monitor the stream service
        streamServerRef = drone.getPeripheral(StreamServer.class, new Ref.Observer<StreamServer>() {
            @Override
            public void onChanged(@Nullable StreamServer streamServer) {
                if (streamServer != null) {
                    // Enable Streaming
                    if (!streamServer.streamingEnabled()) {
                        streamServer.enableStreaming(true);
                    }

                    // Monitor the livestream
                    if (liveStreamRef == null)
                    {
                        liveStreamRef = streamServer.live(new Ref.Observer<CameraLive>() {
                            @Override
                            public void onChanged(@Nullable CameraLive cameraLive) {
                                if (cameraLive != null) {
                                    if (livestream == null)
                                    {
                                        // It is a new livestream
                                        // Set the live stream as the stream to be rendered
                                        // by the stream view
                                        streamView.setStream(cameraLive);
                                    }

                                    // Play the livestream
                                    if (cameraLive.playState() != CameraLive.PlayState.PLAYING) {
                                        cameraLive.play();
                                    }
                                }
                                else {
                                    streamView.setStream(null);
                                }
                                livestream = cameraLive;
                            }
                        });
                    }
                }
                else {
                    // Stop monitoring the livestream
                    liveStreamRef.close();
                    liveStreamRef = null;
                    // Stop rendering the stream
                    streamView.setStream(null);
                }
            }
        });
    }
}