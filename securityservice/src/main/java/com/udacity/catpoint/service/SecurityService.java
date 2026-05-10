package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private boolean catDetected;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            // Reset all sensors to inactive when system is armed (Requirement 10)
            securityRepository.getSensors().forEach(sensor -> {
                if(sensor.getActive()) {
                    sensor.setActive(false);
                    securityRepository.updateSensor(sensor);
                }
            });

            // Requirement 11: Arming home while cat is detected should trigger alarm.
            if(armingStatus == ArmingStatus.ARMED_HOME && catDetected) {
                setAlarmStatus(AlarmStatus.ALARM);
            }
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        this.catDetected = cat;

        // Requirement 7: Cat detected while armed-home triggers alarm
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } 
        // Requirement 8: No cat - set to no alarm only if sensors are not active
        else if(!cat) {
            boolean anySensorActive = securityRepository.getSensors().stream().anyMatch(Sensor::getActive);
            if(!anySensorActive) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        // Requirement 4: If alarm is active, sensor state change doesn't affect it
        if(securityRepository.getAlarmStatus() == AlarmStatus.ALARM) {
            return;
        }
        
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        
        // Requirement 1: Armed and sensor activated -> PENDING_ALARM
        // Requirement 2: Armed, pending, and sensor activated -> ALARM
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated(Sensor sensor) {
        // Requirement 4: If alarm is active, sensor state change doesn't affect it
        if(securityRepository.getAlarmStatus() == AlarmStatus.ALARM) {
            return;
        }
        
        // Requirement 3: Pending alarm and all sensors inactive -> NO_ALARM
        // Requirement 6: Sensor deactivated while already inactive -> no change
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> {
                // Check if all sensors OTHER than the one being deactivated are inactive
                boolean otherSensorActive = securityRepository.getSensors().stream()
                    .filter(s -> !s.getName().equals(sensor.getName())) // Exclude current sensor
                    .anyMatch(Sensor::getActive);
                if(!otherSensorActive) {
                    setAlarmStatus(AlarmStatus.NO_ALARM);
                }
            }
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if(!sensor.getActive() && active) {
            handleSensorActivated();
        } else if (sensor.getActive() && active && securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
            // Requirement 5: Re-activating an already active sensor while pending sets alarm.
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (sensor.getActive() && !active) {
            handleSensorDeactivated(sensor);
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
