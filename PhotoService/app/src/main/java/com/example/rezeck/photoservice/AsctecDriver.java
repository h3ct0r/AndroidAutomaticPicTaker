package com.example.rezeck.photoservice;

/**
 * Created by h3ct0r on 13/1/16.
 */

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import com.example.rezeck.photoservice.util.AsctecPackage;
import com.example.rezeck.photoservice.util.ByteConversor;
import com.example.rezeck.photoservice.util.crc16.CRC16JavaImp;
import com.example.rezeck.photoservice.util.crc16.ICRC16;

import static com.example.rezeck.photoservice.util.ByteConversor.extractShort;
import static com.example.rezeck.photoservice.util.ByteConversor.extractSignedInt;
import static com.example.rezeck.photoservice.util.ByteConversor.extractSignedShort;

/**
 * Driver to control a Hummingbird or Pelican Quadcopter.
 *
 */
public class AsctecDriver implements IRobotControl, IStateSensor, ICRC16 {

    private static final double GRAVITY_SI = 9.80665; // in SI m/s^2

    ICommunication communicator;
    ICRC16 crc16Helper;

    final String TAG = this.getClass().getName();

    /**
     * Commands for quadcopter.
     */
    // Start motors >*>m'+chr(1)
    private static final byte[] START_MOTORS = { 0x3e, 0x2a, 0x3e, 0x6d, 0x01 };
    // Stop motors '>*>m'+chr(0)
    private static final byte[] STOP_MOTORS = { 0x3e, 0x2a, 0x3e, 0x6d, 0x00 };

    /**
     * Header
     */
    private static final byte[] REQUEST_HEADER = { 0x3e, 0x2a, 0x3e, 0x70 };

    // >*>di
    private static final byte[] CONTROL_HEADER = { 0x3e, 0x2a, 0x3e, 0x64, 0x69 };

    /**
     * Status
     */
    private static final byte[] STATUS = { 0x01, 0x00 };

    private static final byte[] IMU_CALC_DATA = { 0x04, 0x00 };

    private static final byte[] IMU_RAW_DATA = { 0x02, 0x00 };
    // 0x80 0x00
    private static final byte[] GPS_DATA = { -128, 0x00 };

    private static final byte[] WAYPOINT_HEADER = { 0x3e, 0x2a, 0x3e, 0x64, 0x69 }; // >*>ws
    private static final byte[] WAYPOINT_GOTO_HEADER = { 0x3e, 0x2a, 0x3e, 0x64, 0x69 }; // >*>wg
    private static final byte[] WAYPOINT_LAUCH_HEADER = { 0x3e, 0x2a, 0x3e, 0x64, 0x69 }; // >*>wl
    private static final byte[] WAYPOINT_END_FLIGHT_HEADER = { 0x3e, 0x2a, 0x3e, 0x64, 0x69 }; // >*>we
    private static final byte[] WAYPOINT_COME_HOME_HEADER = { 0x3e, 0x2a, 0x3e, 0x64, 0x69 }; // >*>wh

    public enum WAYPOINT_PROP {
        WPPROP_ABSCOORDS(0x01), // if set waypoint is interpreted as absolute coordinates, else relative coords
        WPPROP_HEIGHTENABLED(0x02), // set new height at waypoint
        WPPROP_YAWENABLED(0x04), // set new yaw-angle at waypoint (not yet implemented)
        WPPROP_AUTOMATICGOTO(0x10), // if set, vehicle will not wait for a goto command, but goto this waypoint directly
        WPPROP_CAM_TRIGGER(0x20); // if set, photo camera is triggered when waypoint is reached and time to stay is 80% up

        private int value;

        private WAYPOINT_PROP(int c) {
            this.value = c;
        }

        public int getValue() {
            return value;
        }
    };

    public AsctecDriver() {
        communicator = new SerialCommunication();
        crc16Helper = new CRC16JavaImp();
    }

    /**
     * Return the values of the calculated IMU values.
     *
     * @return values of: gyro_x, gyro_y, gyro_z, mag_x , mag_y, mag_z, acc_x,
     *         acc_y, acc_z, pressure.
     */
    @Override
    public double[] senseIMU() {
        // Send request
        communicator.send(REQUEST_HEADER);
        communicator.send(IMU_RAW_DATA);

        // get IMU_RAW_DATA
        AsctecPackage p = readPackage(0x01);

        if (p == null) {
            Log.e(AsctecDriver.class.getName(), "Package lost");
            return null;
        }

        // gyro_x gyro_y gyro_z mag_x mag_y mag_z acc_x acc_y acc_z
        // temp_gyro temp_ADC

        // pressure
        double pressure = ByteConversor.extractSignedInt(p.getDataBytes(), 0);

        // gyros
        double gyroX = extractShort(p.getData(), 4) / 1000.0;
        double gyroY = -extractShort(p.getData(), 6) / 1000.0;
        double gyroZ = (360000.0 - extractShort(p.getData(), 8)) / 1000.0;

        // magnetometers
        double magX = extractShort(p.getData(), 10);
        double magY = extractShort(p.getData(), 12);
        double magZ = extractShort(p.getData(), 14);

        // Accelerometers
        double accX = extractShort(p.getData(), 16) / 1000.0 * GRAVITY_SI;
        double accY = extractShort(p.getData(), 18) / 1000.0 * GRAVITY_SI;
        double accZ = extractShort(p.getData(), 20) / 1000.0 * GRAVITY_SI;

        return new double[] { gyroX, gyroY, gyroZ, magX, magY, magZ, accX,
                accY, accZ, pressure };
    }

    @Override
    public double[] senseState() {
        // Send request
        communicator.send(REQUEST_HEADER);
        communicator.send(IMU_CALC_DATA);

        // get IMU_CALC_DATA
        AsctecPackage p = readPackage(0x03);

        if (p == null) {
            Log.e(AsctecDriver.class.getName(), "Package lost");
            return null;
        }

        // angles derived by integration of gyro_outputs, drift compensated by
        // data
        // fusion; -90000..+90000 pitch(nick) and roll, 0..360000 yaw; 1000 = 1
        // degree
        int angle_nick = extractSignedInt(p.getDataBytes(), 0);
        int angle_roll = extractSignedInt(p.getDataBytes(), 4);
        int angle_yaw = extractSignedInt(p.getDataBytes(), 8);

        // angular velocities, raw values [16 bit] but bias free
        int angvel_nick = extractSignedInt(p.getDataBytes(), 12);
        int angvel_roll = extractSignedInt(p.getDataBytes(), 16);
        int angvel_yaw = extractSignedInt(p.getDataBytes(), 20);

        // acc-sensor outputs, calibrated: -10000..+10000 = -1g..+1g
        int acc_x_calib = extractSignedShort(p.getDataBytes(), 22);
        int acc_y_calib = extractSignedShort(p.getDataBytes(), 24);
        int acc_z_calib = extractSignedShort(p.getDataBytes(), 28);

        // horizontal / vertical accelerations: -10000..+10000 = -1g..+1g
        int acc_x = extractSignedShort(p.getDataBytes(), 30);
        int acc_y = extractSignedShort(p.getDataBytes(), 32);
        int acc_z = extractSignedShort(p.getDataBytes(), 34);

        // reference angles derived by accelerations only: -90000..+90000; 1000
        // = 1 degree
        int acc_angle_nick = extractSignedInt(p.getDataBytes(), 36);
        int acc_angle_roll = extractSignedInt(p.getDataBytes(), 38);

        // total acceleration measured (10000 = 1g)
        int acc_absolute_value = extractSignedInt(p.getDataBytes(), 42);

        // magnetic field sensors output, offset free and scaled;
        // units not determined, as only the direction of the field vector is
        // taken into account
        int Hx = extractSignedInt(p.getDataBytes(), 46);
        int Hy = extractSignedInt(p.getDataBytes(), 50);
        int Hz = extractSignedInt(p.getDataBytes(), 54);

        // compass reading: angle reference for angle_yaw: 0..360000; 1000 = 1
        // degree
        int mag_heading = extractSignedInt(p.getDataBytes(), 58);

        // pseudo speed measurements: integrated accelerations, pulled towards
        // zero;
        // units unknown; used for short-term position stabilization
        int speed_x = extractSignedInt(p.getDataBytes(), 62);
        int speed_y = extractSignedInt(p.getDataBytes(), 64);
        int speed_z = extractSignedInt(p.getDataBytes(), 68);

        // height in mm (after data fusion)
        int height = extractSignedInt(p.getDataBytes(), 72);

        // diff. height in mm/s (after data fusion)
        int dheight = extractSignedInt(p.getDataBytes(), 76);

        // diff. height measured by the pressure sensor [mm/s]
        int dheight_reference = extractSignedInt(p.getDataBytes(), 80);

        // height measured by the pressure sensor [mm]
        int height_reference = extractSignedInt(p.getDataBytes(), 84);

        StringBuffer sb = new StringBuffer();
        for (byte b : p.getDataBytes()) {
            sb.append("0x" + b + ",");
        }
        Log.d(TAG, "Raw data lenght = " + p.getDataBytes().length
                + " Raw data " + sb.toString());

        return new double[] { angle_nick, angle_roll, angle_yaw };
    }

    /**
     * Get the values from the GPS sensor.
     *
     * @return latitude, longitude, height, speedX, speedY, heading,
     *         horizontalAccur, verticalAccur, speedAccur, numSatelites, status
     */
    @Override
    public double[] senseGPS() {
        // Send request
        communicator.send(REQUEST_HEADER);
        communicator.send(GPS_DATA);

        // Read the response.
        AsctecPackage p = readPackage(0x23);

        if (p == null) {
            Log.e(AsctecDriver.class.getName(), "Package lost");
            return null;
        }

        // speed x speed y heading hor accur vert accur speed
        // acc num sat
        // latitude longitud and height
        double latitude = extractSignedInt(p.getDataBytes(), 0) / 10000000.0;
        double longitude = extractSignedInt(p.getDataBytes(), 4) / 10000000.0;
        double height = extractSignedInt(p.getDataBytes(), 8) / 1000.0;
        // Speed
        double speedX = extractSignedInt(p.getDataBytes(), 12) / 1000.0;
        double speedY = extractSignedInt(p.getDataBytes(), 16) / 1000.0;
        double heading = extractSignedInt(p.getDataBytes(), 20) / 1000.0;

        // horizontal accur vertical accur speed acc num sat status
        double horizontalAccur = extractSignedInt(p.getDataBytes(), 24) / 1000.0;
        double verticalAccur = extractSignedInt(p.getDataBytes(), 28) / 1000.0;
        double speedAccur = extractSignedInt(p.getDataBytes(), 32) / 1000.0;

        // Number of satelites
        double numSatelites = extractSignedInt(p.getDataBytes(), 36);
        // Status
        double status = extractSignedInt(p.getDataBytes(), 40);

        return new double[] { latitude, longitude, height, speedX, speedY,
                heading, horizontalAccur, verticalAccur, speedAccur,
                numSatelites, status };
    }

    /**
     * Based on the book: Autonomous Flying Robots by Kenzo Nonami. Equation
     * (8.19)
     */
    @Override
    public void velocityControl(double x, double y, double z, double angX,
                                double angY, double angZ) {

        // For testing, initially do not move the angular velocity.
        double yaw = 0;
        // Initially are random values
        // FIXME adjust with the mass.
        double m = 1;
        // FIXME adjust.
        double g = 1;
        double thrust = m * Math.sqrt(x * x + y * y + (z + g) * (z + g));
        // 100 because is a percentace and sqrt(6), because it needs to be

        double roll = Math.asin(m * (x * Math.sin(yaw) - y * Math.cos(yaw))
                / thrust);
        roll = Math.toDegrees(roll);

        double pitch = Math.atan((x * Math.cos(yaw) + y * Math.sin(yaw))
                / (z + g));
        pitch = Math.toDegrees(pitch);

        // normalized.
        thrust *= 100 / Math.sqrt(2+(1+g)*(1+g));

        Log.d("velocityControl", "t:\t" + thrust + "\npitch=\t" + pitch + "\nroll=\t"
                + roll + "\nyaw=\t" + yaw);

        // Send the values.
        pryControl(thrust, pitch, roll, yaw);
    }

    @Override
    public void pryControl(double thrust, double pitch, double roll, double yaw) {
        // struct CTRL_INPUT
        int controlMessage[] = new int[6];

        // pitch -2047..+2047 (0=neutral)
        int pitch1 = (int) Math.round((2047.0 / 51.2) * pitch);
        // roll -2047..+2047 (0=neutral)
        int roll1 = (int) Math.round((2047.0 / 51.2) * roll);
        // yaw -2047..+2047 (0=neutral)
        int yaw1 = (int) Math.round((2047.0 / 51.2) * yaw);

        // thrust 0..4095 => 0..100%
        // Normal Vector times 4095/100
        int thrust1 = (int) Math.round(thrust * 40.95);

        controlMessage[0] = pitch1;
        controlMessage[1] = roll1;
        controlMessage[2] = yaw1;
        controlMessage[3] = thrust1;
        // control bits
        controlMessage[4] = 15;// Short.parseShort("1000", 2);

        // Computing checksum
        int cs = 0;
        for (int arrayVal : controlMessage) {
            cs += (arrayVal);
        }
        // Assign Checksum
        controlMessage[5] = cs + 0xAAAA;

        // Convert control message to little endian
        byte data[] = new byte[12];
        int dataOffSet = 0;
        for (int a : controlMessage) {
            ByteBuffer bb = ByteBuffer.allocate(4).order(
                    ByteOrder.nativeOrder());
            bb.putInt(a);
            byte[] bytes = bb.array();

            // Put significant numeric information of the bytebuffer to the data
            // array
            for (int i = 0; i <= 1; i++) {
                // Log.d(TAG,
                // "Byte message [" + String.format("0x%02X ", bytes[i])
                // + "]");
                data[dataOffSet] = bytes[i];
                dataOffSet++;
            }
        }

        // Append control header to the data message (CONTROL_HEADER + data)
        // byte[] result = Arrays.copyOf(CONTROL_HEADER, CONTROL_HEADER.length
        // + data.length);
        // System.arraycopy(data, 0, result, CONTROL_HEADER.length,
        // data.length);

        // Testing purposes only
        StringBuffer sb = new StringBuffer();
        for (byte b : data) {
            sb.append(String.format("0x%02X ", b));
        }
        Log.d(TAG, "Byte control message [" + sb.toString() + "]");

        // send data
        communicator.send(CONTROL_HEADER);
        communicator.send(data);
    }

    /**
     * Send a waypoint command to the robot.
     *
     * @param yaw Desired heading at waypoint
     * @param height Height over 0 reference in mm
     * @param time Time to stay at a waypoint in 1/100 th s
     * @param point_x Waypoint coordinate in mm, longitude in abs coords
     * @param point_y Waypoint coordinate in mm, longitude in abs coords
     * @param max_speed Value in percent %, default value is 100
     * @param pos_acc Position accuracy to consider a waypoint reached in mm (default: 2500 (= 2.5 m))
     * @param waypointProperties Properties of this waypoint
     */
    public void waypointSimpleControl(int yaw, int height, int time, int point_x, int point_y,
                                      int max_speed, int pos_acc, WAYPOINT_PROP waypointProperties){
        int controlMessage[] = new int[10];
        int wp_number = 1;

        controlMessage[0] = wp_number; //Always set to 1

        //Check the definition of properties
        controlMessage[1] = waypointProperties.getValue();

        // Value in percent % (default value is 100)
        controlMessage[2] = max_speed;

        // Time to stay at a waypoint (XYZ) in 1/100th s
        controlMessage[3] = time;

        //position accuracy to consider a waypoint reached in mm (default: 2500 (= 2.5 m))
        controlMessage[4] = pos_acc;

        // Chskum
        controlMessage[5] = 0xAAAA;

        //waypoint coordinates in mm
        // longitude in abs coords
        controlMessage[6] = point_x;
        controlMessage[7] = point_y;

        //Desired heading at waypoint
        controlMessage[8] = yaw;

        //height over 0 reference in mm
        controlMessage[9] = height;

        // Computing checksum
        //chksum = 0xAAAA + wp.yaw + wp.height + wp.time + wp.X + wp.Y + wp.max_speed + wp.pos_acc + wp.properties + wp.wp_number;
        int chksum = controlMessage[0];
        for (int arrayVal : controlMessage) {
            chksum += (arrayVal);
        }
        controlMessage[0] = chksum;

        // Convert control message to little endian
        byte data[] = new byte[20];
        int dataOffSet = 0;
        for (int a : controlMessage) {
            ByteBuffer bb = ByteBuffer.allocate(4).order(
                    ByteOrder.nativeOrder());
            bb.putInt(a);
            byte[] bytes = bb.array();

            // Put significant numeric information of the bytebuffer to the data
            // array
            for (int i = 0; i <= 1; i++) {
                data[dataOffSet] = bytes[i];
                dataOffSet++;
            }
        }

        // Testing purposes only
        StringBuffer sb = new StringBuffer();
        for (byte b : data) sb.append(String.format("0x%02X ", b));
        Log.d(TAG, "Byte waypoint message [" + sb.toString() + "]");

        // send data
        communicator.send(WAYPOINT_HEADER);
        communicator.send(data);
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] received = new byte[20];
        int mSize = communicator.receive(received);
        // Testing purposes only
        sb = new StringBuffer();
        for (byte b : received) sb.append(String.format("0x%02X ", b));
        Log.d(TAG, "Byte waypoint received message [" + sb.toString() + "]");
    }

    public boolean turnOn() {
        communicator.send(START_MOTORS);
        return true;
    }

    public boolean turnOff() {
        communicator.send(STOP_MOTORS);
        return true;
    }

    /**
     * Turn ON/OFF the motors.
     *
     * @param state
     *            true for turning on.
     */
    @Override
    public boolean turnOnOff(boolean state) {
        if (state)
            communicator.send(START_MOTORS);
        else
            communicator.send(STOP_MOTORS);
        return true;
    }

    public double getVoltage() {

        // Send request
        communicator.send(REQUEST_HEADER);
        communicator.send(STATUS);

        AsctecPackage p = readPackage(0x02);
        if (p == null) {
            Log.e(AsctecDriver.class.getName(), "Package lost");
            return -1;
        }

        int volt = extractSignedShort(p.getDataBytes(), 0);
        return volt / 1000.0;
    }

    /**
     * Read a package from the serial port.
     *
     * @param packageDescriptor
     *            expected packaged descriptor.
     * @return read package.
     */
    private AsctecPackage readPackage(int packageDescriptor) {
        // Wait for a while, because the micro needs time to answer.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AsctecPackage newPackage = null;

        byte[] received = new byte[1000];

        // Receive the message.
        int mSize = communicator.receive(received);

        Log.d(TAG, "mSize ->" + mSize);

        // Validate received data
        for (int i = 0; i < mSize - 5; i++) {

            // Validate header 0x3A2A3E
            if (received[i] != 0x3E || received[i + 1] != 0x2A
                    || received[i + 2] != 0x3E) {
                continue;
            }
            // Get data length.
            int length = received[i + 3] + 256 * received[i + 4];
            // Validate size
            if (received.length < length + 11) {
                return null;
            }
            // TODO Validate end of the package.

            // Validate package descriptor
            if (received[i + 5] != packageDescriptor) {
                // This is not the requested package.
                i += length + 11; // 6 of the header and 5 of the end.
                Log.w(AsctecDriver.class.getName(),
                        "Wrong package type for status request");
                continue;
            }

            int[] data = new int[length];
            byte dataBytes[] = new byte[length];

            for (int j = 0; j < data.length; j++) {
                data[j] = received[6 + j] & 0xFF;
                dataBytes[j] = received[6 + j];
            }

            // fill data
            newPackage = new AsctecPackage();
            newPackage.setData(data);
            newPackage.setDataBytes(dataBytes);
            newPackage.setPackageDescriptor(received[5]);
            return newPackage;
        }

        return newPackage;
    }

    /**
     * Connect to the serial port.
     *
     * @param params
     * @throws Exception
     *             connection error.
     */
    public void connect(HashMap<String, Object> params) throws Exception {
        communicator.connect(params);
    }

    /**
     * Disconnect.
     *
     * @throws Exception
     *             connection error.
     */
    public void disconnect() throws Exception {
        communicator.disconnect();
    }

    @Override
    public int crc16(int[] data) {
        return crc16Helper.crc16(data);
    }

}

