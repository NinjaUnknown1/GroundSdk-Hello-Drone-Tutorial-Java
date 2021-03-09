package com.example.hellodrone;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.facility.AutoConnection;

public class MainActivity extends AppCompatActivity {

    // GroundSDK Instance
    private GroundSdk groundSdk;

    // Current Drone Instance
    private Drone drone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    }

    /*  Stop the drone monitors  */
    private void stopDroneMonitors() {

    }
}