package com.roboticsftc.andi.arduinobluetooth;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by andy on 4/23/17.
 *
 * Control configuration
 */

public class Config implements Serializable{

    private String name = "";
    private HashMap<Integer, Key> keySet;

    public Config(String name) {
        this.name = name;
        this.keySet = new HashMap<>();
    }


    public String getName() {
        return name;
    }

    public void setCommand(int id, String command, String serviceUUID, String charUUID) {

        Key k;
        if (keySet.containsKey(id)) {
            k = keySet.get(id);

            //We don't want to replace values with a blank
            if (!command.equals("")) k.command = command;
            if (!serviceUUID.equals("")) k.serviceUUID = serviceUUID;
            if (!charUUID.equals("")) k.charUUID = charUUID;
        } else {
            k = new Key(command, serviceUUID, charUUID);
        }

        keySet.put(id, k);
    }

    //This should not be called outside this class
    private Key getKey(int id) {
        try {
            return keySet.get(id);
        } catch (NullPointerException npe) {
            return null;
        }

    }
    public String getCommand(int id) {
        Key k = getKey(id);
        if (k != null) return k.command;
        return "";
    }
    public String getServiceUUID(int id) {
        Key k = getKey(id);
        if (k != null) return k.serviceUUID;
        return "";
    }
    public String getCharUUID(int id) {
        Key k = getKey(id);
        if (k != null) return k.charUUID;
        return "";
    }

    //Valid means all 3 fields of command, service, and characteristic UUID haave values
    public boolean isValid(int id) {
        return !getCommand(id).equals("") && !getServiceUUID(id).equals("") && !getCharUUID(id).equals("");
    }
}

class Key implements Serializable { //Strings are already Serializable

    String command;
    String serviceUUID;
    String charUUID;

    Key(String command, String serviceUUID, String charUUID) {
        this.command = command;
        this.serviceUUID = serviceUUID;
        this.charUUID = charUUID;
    }
}