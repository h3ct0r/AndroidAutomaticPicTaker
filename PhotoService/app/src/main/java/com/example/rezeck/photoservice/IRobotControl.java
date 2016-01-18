package com.example.rezeck.photoservice;

/**
 * Created by h3ct0r on 13/1/16.
 */
public interface IRobotControl {

    /**
     * Set a linear and angular speed for the robot.
     *
     * @param x linear velocity for axe X
     * @param y linear velocity for axe Y
     * @param z linear velocity for axe Z
     * @param angX angular velocity for axe X
     * @param angY angular velocity for axe Y
     * @param angZ angular velocity for axe Z
     */
    void velocityControl(double x, double y, double z, double angX, double angY, double angZ);


    /**
     * Pitch, Roll, Yaw control for the robot.
     *
     * @param thrust
     * @param pitch
     * @param roll
     * @param yaw
     */
    void pryControl(double thrust, double pitch, double roll,
                    double yaw);

//	public void setSpeed(double x, double y, double z, double pitch,
//			double roll, double yaw) {
    /**
     * Turn on/off the robot.
     * @param state true for tuning on and false to turning off.
     * @return successful action.
     */
    boolean turnOnOff(boolean state);
}