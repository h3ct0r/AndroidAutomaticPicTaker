package com.example.rezeck.photoservice;

/**
 * Created by h3ct0r on 13/1/16.
 */
import java.util.Map;

/**
 * Basic Communication for a low-level communication.
 *
 *
 */
public interface ICommunication {

    void connect(Map<String, Object> params) throws Exception;

    void disconnect() throws Exception;

    /**
     * Send data
     * @param data bytes to send.
     */
    void send(byte[] data);

    /**
     * Just send a byte.
     * @param data byte to send.
     */
    void send(byte data);


    /**
     * Receive data.
     * @param buffer this buffer is filled by the method.
     * @return number of received
     */
    int receive(byte[] buffer);
}
