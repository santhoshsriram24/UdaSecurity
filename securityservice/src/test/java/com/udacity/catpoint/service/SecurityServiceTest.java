package com.udacity.catpoint.service;

import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.data.SensorType;
import com.udacity.catpoint.application.StatusListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityService.
 * Tests all 11 application requirements for the security monitoring system.
 */
@DisplayName("Security Service Tests")
public class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        securityService = new SecurityService(securityRepository, imageService);
        securityService.addStatusListener(statusListener);
    }

    // ==================== REQUIREMENT 1 ====================
    // If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    @DisplayName("Req 1: sensorActivated_alarmArmedAndStatusNoAlarm_statusChangesToPending")
    void sensorActivated_alarmArmedAndStatusNoAlarm_statusChangesToPending() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        // Act
        securityService.changeSensorActivationStatus(sensor, true);

        // Assert
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(statusListener).notify(AlarmStatus.PENDING_ALARM);
    }

    // ==================== REQUIREMENT 2 ====================
    // If alarm is armed and a sensor becomes activated and the system is already pending alarm, set off the alarm.
    @Test
    @DisplayName("Req 2: sensorActivated_alarmArmedAndStatusPending_statusChangesToAlarm")
    void sensorActivated_alarmArmedAndStatusPending_statusChangesToAlarm() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Act
        securityService.changeSensorActivationStatus(sensor, true);

        // Assert
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).notify(AlarmStatus.ALARM);
    }

    // ==================== REQUIREMENT 3 ====================
    // If pending alarm and all sensors are inactive, return to no alarm state.
    @ParameterizedTest
    @DisplayName("Req 3: sensorDeactivated_systemPendingAndAllSensorsInactive_statusChangesToNoAlarm")
    @ValueSource(strings = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorDeactivated_systemPendingAndAllSensorsInactive_statusChangesToNoAlarm(String armingStatusStr) {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.valueOf(armingStatusStr));
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);

        // Act
        securityService.changeSensorActivationStatus(sensor, false);

        // Assert
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener).notify(AlarmStatus.NO_ALARM);
    }

    // ==================== REQUIREMENT 4 ====================
    // If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    @DisplayName("Req 4: sensorActivated_alarmActive_noStateChange")
    void sensorActivated_alarmActive_noStateChange() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(false);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);

        // Act
        securityService.changeSensorActivationStatus(sensor, true);

        // Assert - setAlarmStatus should not be called when alarm is already ALARM
        verify(securityRepository, never()).setAlarmStatus(any());
        verify(statusListener, never()).notify(any());
    }

    @Test
    @DisplayName("Req 4: sensorDeactivated_alarmActive_noStateChange")
    void sensorDeactivated_alarmActive_noStateChange() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);

        // Act
        securityService.changeSensorActivationStatus(sensor, false);

        // Assert - setAlarmStatus should not be called when alarm is already ALARM
        verify(securityRepository, never()).setAlarmStatus(any());
        verify(statusListener, never()).notify(any());
    }

    // ==================== REQUIREMENT 5 ====================
    // If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    @DisplayName("Req 5: sensorActivatedWhileActive_systemPending_statusChangesToAlarm")
    void sensorActivatedWhileActive_systemPending_statusChangesToAlarm() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true); // Already active
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Act
        securityService.changeSensorActivationStatus(sensor, true); // Try to activate again

        // Assert
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).notify(AlarmStatus.ALARM);
    }

    // ==================== REQUIREMENT 6 ====================
    // If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    @DisplayName("Req 6: sensorDeactivatedWhileInactive_noAlarmStateChange")
    void sensorDeactivatedWhileInactive_noAlarmStateChange() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(false); // Already inactive
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Act
        securityService.changeSensorActivationStatus(sensor, false); // Try to deactivate again

        // Assert
        verify(securityRepository, never()).setAlarmStatus(any());
        verify(statusListener, never()).notify(any());
    }

    // ==================== REQUIREMENT 7 ====================
    // If the camera image contains a cat while the system is armed-home, put the system into alarm status.
    @Test
    @DisplayName("Req 7: catDetected_systemArmedHome_statusChangesToAlarm")
    void catDetected_systemArmedHome_statusChangesToAlarm() {
        // Arrange
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Act
        securityService.processImage(image);

        // Assert
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).notify(AlarmStatus.ALARM);
        verify(statusListener).catDetected(true);
    }

    // ==================== REQUIREMENT 8 ====================
    // If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    @DisplayName("Req 8: noCatDetected_statusChangesToNoAlarm")
    void noCatDetected_statusChangesToNoAlarm() {
        // Arrange
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(new Sensor("Front Door", SensorType.DOOR)); // inactive sensor
        
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getSensors()).thenReturn(sensors);

        // Act
        securityService.processImage(image);

        // Assert
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener).notify(AlarmStatus.NO_ALARM);
        verify(statusListener).catDetected(false);
    }

    // ==================== REQUIREMENT 9 ====================
    // If the system is disarmed, set the status to no alarm.
    @Test
    @DisplayName("Req 9: systemDisarmed_statusChangesToNoAlarm")
    void systemDisarmed_statusChangesToNoAlarm() {
        // Arrange
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Act
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // Assert
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener).notify(AlarmStatus.NO_ALARM);
        verify(securityRepository).setArmingStatus(ArmingStatus.DISARMED);
    }

    // ==================== REQUIREMENT 10 ====================
    // If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @DisplayName("Req 10: systemArmed_allSensorsReset")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemArmed_allSensorsReset(ArmingStatus armingStatus) {
        // Arrange
        Sensor sensor1 = new Sensor("Front Door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Garage Window", SensorType.WINDOW);
        sensor1.setActive(true);
        sensor2.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);
        when(securityRepository.getSensors()).thenReturn(sensors);

        // Act
        securityService.setArmingStatus(armingStatus);

        // Assert
        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());
        verify(securityRepository, times(2)).updateSensor(any(Sensor.class));
    }

    // ==================== REQUIREMENT 11 ====================
    // If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    @DisplayName("Req 11: systemArmedHomeWithCat_statusIsAlarm")
    void systemArmedHomeWithCat_statusIsAlarm() {
        // Arrange
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(new HashSet<>());

        // Act
        securityService.processImage(image);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // Assert
        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    // ==================== ADDITIONAL TESTS FOR COVERAGE ====================

    @Test
    @DisplayName("Sensor activation when disarmed has no effect")
    void sensorActivated_systemDisarmed_noAlarmChange() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        // Act
        securityService.changeSensorActivationStatus(sensor, true);

        // Assert
        verify(securityRepository, never()).setAlarmStatus(any());
        verify(statusListener, never()).notify(any());
    }

    @Test
    @DisplayName("Add and remove status listeners")
    void addRemoveStatusListeners() {
        // Arrange
        StatusListener listener1 = mock(StatusListener.class);
        StatusListener listener2 = mock(StatusListener.class);

        // Act
        securityService.removeStatusListener(statusListener);
        securityService.addStatusListener(listener1);
        securityService.addStatusListener(listener2);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        // Assert
        verify(listener1).notify(AlarmStatus.PENDING_ALARM);
        verify(listener2).notify(AlarmStatus.PENDING_ALARM);
        verify(statusListener, never()).notify(any());
    }

    @Test
    @DisplayName("Get sensors from repository")
    void getSensors() {
        // Arrange
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(new Sensor("Front Door", SensorType.DOOR));
        when(securityRepository.getSensors()).thenReturn(sensors);

        // Act
        Set<Sensor> result = securityService.getSensors();

        // Assert
        assertEquals(sensors, result);
        verify(securityRepository).getSensors();
    }

    @Test
    @DisplayName("Add sensor to repository")
    void addSensor() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        // Act
        securityService.addSensor(sensor);

        // Assert
        verify(securityRepository).addSensor(sensor);
    }

    @Test
    @DisplayName("Remove sensor from repository")
    void removeSensor() {
        // Arrange
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        // Act
        securityService.removeSensor(sensor);

        // Assert
        verify(securityRepository).removeSensor(sensor);
    }

    @Test
    @DisplayName("Get alarm status from repository")
    void getAlarmStatus() {
        // Arrange
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Act
        AlarmStatus status = securityService.getAlarmStatus();

        // Assert
        assertEquals(AlarmStatus.ALARM, status);
        verify(securityRepository).getAlarmStatus();
    }

    @Test
    @DisplayName("Get arming status from repository")
    void getArmingStatus() {
        // Arrange
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Act
        ArmingStatus status = securityService.getArmingStatus();

        // Assert
        assertEquals(ArmingStatus.ARMED_HOME, status);
        verify(securityRepository).getArmingStatus();
    }

    @Test
    @DisplayName("Image service method is called with correct parameters")
    void processImage_callsImageService() {
        // Arrange
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Act
        securityService.processImage(image);

        // Assert
        verify(imageService).imageContainsCat(image, 50.0f);
    }

    @Test
    @DisplayName("Multiple sensors with different states")
    void multipleSensorsWithDifferentStates() {
        // Arrange
        Sensor door = new Sensor("Front Door", SensorType.DOOR);
        Sensor window = new Sensor("Living Room Window", SensorType.WINDOW);
        door.setActive(true);
        window.setActive(false);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(door);
        sensors.add(window);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // Act
        Set<Sensor> result = securityService.getSensors();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.getName().equals("Front Door") && s.getActive()));
        assertTrue(result.stream().anyMatch(s -> s.getName().equals("Living Room Window") && !s.getActive()));
    }
}
