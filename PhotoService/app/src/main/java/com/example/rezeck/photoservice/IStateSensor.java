package com.example.rezeck.photoservice;

/**
 * Created by h3ct0r on 13/1/16.
 */
public interface IStateSensor {

    /**
     * Reads the IMU values. 3 accelerometers and 3 gyroscopes.
     *
     * @return an array of 6 values (3 for accelerometers and 3 for gyroscopes).
     */
    double[] senseIMU();


    /**
     * Sense the computed state from IMU data.
     *
     * @return
     */
    double[] senseState();


    /**
     * Read the GPS values. Latitude, Longitude and altitude.
     *
     * @return an array with the 3 values.
     */
    double[] senseGPS();


    double getVoltage();
}
