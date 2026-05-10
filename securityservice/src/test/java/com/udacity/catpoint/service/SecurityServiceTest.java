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

    @Test
    @DisplayName("Req 1: sensorActivated_alarmArmedAndStatusNoAlarm_statusChangesToPending")
    void sensorActivated_alarmArmedAndStatusNoAlarm_statusChangesToPending() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(statusListener).notify(AlarmStatus.PENDING_ALARM);
    }

    @Test
    @DisplayName("Req 2: sensorActivated_alarmArmedAndStatusPending_statusChangesToAlarm")
    void sensorActivated_alarmArmedAndStatusPending_statusChangesToAlarm() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).notify(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @DisplayName("Req 3: sensorDeactivated_systemPendingAndAllSensorsInactive_statusChangesToNoAlarm")
    @ValueSource(strings = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorDeactivated_systemPendingAndAllSensorsInactive_statusChangesToNoAlarm(String armingStatusStr) {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.valueOf(armingStatusStr));
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener).notify(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("Req 4: sensorActivated_alarmActive_noStateChange")
    void sensorActivated_alarmActive_noStateChange() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(false);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any());
        verify(statusListener, never()).notify(any());
    }

    @Test
    @DisplayName("Req 4: sensorDeactivated_alarmActive_noStateChange")
    void sensorDeactivated_alarmActive_noStateChange() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
        verify(statusListener, never()).notify(any());
    }

    @Test
    @DisplayName("Req 5: sensorActivatedWhileActive_systemPending_statusChangesToAlarm")
    void sensorActivatedWhileActive_systemPending_statusChangesToAlarm() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).notify(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("Req 6: sensorDeactivatedWhileInactive_noAlarmStateChange")
    void sensorDeactivatedWhileInactive_noAlarmStateChange() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(false);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
        verify(statusListener, never()).notify(any());
    }

    @Test
    @DisplayName("Req 7: catDetected_systemArmedHome_statusChangesToAlarm")
    void catDetected_systemArmedHome_statusChangesToAlarm() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(image);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).notify(AlarmStatus.ALARM);
        verify(statusListener).catDetected(true);
    }

    @Test
    @DisplayName("Req 8: noCatDetected_statusChangesToNoAlarm")
    void noCatDetected_statusChangesToNoAlarm() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(new Sensor("Front Door", SensorType.DOOR));
        
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(image);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener).notify(AlarmStatus.NO_ALARM);
        verify(statusListener).catDetected(false);
    }

    @Test
    @DisplayName("Req 9: systemDisarmed_statusChangesToNoAlarm")
    void systemDisarmed_statusChangesToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener).notify(AlarmStatus.NO_ALARM);
        verify(securityRepository).setArmingStatus(ArmingStatus.DISARMED);
    }

    @ParameterizedTest
    @DisplayName("Req 10: systemArmed_allSensorsReset")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemArmed_allSensorsReset(ArmingStatus armingStatus) {
        Sensor sensor1 = new Sensor("Front Door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Garage Window", SensorType.WINDOW);
        sensor1.setActive(true);
        sensor2.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(armingStatus);

        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());
        verify(securityRepository, times(2)).updateSensor(any(Sensor.class));
    }

    @Test
    @DisplayName("Req 11: systemArmedHomeWithCat_statusIsAlarm")
    void systemArmedHomeWithCat_statusIsAlarm() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getSensors()).thenReturn(new HashSet<>());

        securityService.processImage(image);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("Sensor activation when disarmed has no effect")
    void sensorActivated_systemDisarmed_noAlarmChange() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any());
        verify(statusListener, never()).notify(any());
    }

    @Test
    @DisplayName("Add and remove status listeners")
    void addRemoveStatusListeners() {
        StatusListener listener1 = mock(StatusListener.class);
        StatusListener listener2 = mock(StatusListener.class);

        securityService.removeStatusListener(statusListener);
        securityService.addStatusListener(listener1);
        securityService.addStatusListener(listener2);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(listener1).notify(AlarmStatus.PENDING_ALARM);
        verify(listener2).notify(AlarmStatus.PENDING_ALARM);
        verify(statusListener, never()).notify(any());
    }

    @Test
    @DisplayName("Get sensors from repository")
    void getSensors() {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(new Sensor("Front Door", SensorType.DOOR));
        when(securityRepository.getSensors()).thenReturn(sensors);

        Set<Sensor> result = securityService.getSensors();

        assertEquals(sensors, result);
        verify(securityRepository).getSensors();
    }

    @Test
    @DisplayName("Add sensor to repository")
    void addSensor() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        securityService.addSensor(sensor);

        verify(securityRepository).addSensor(sensor);
    }

    @Test
    @DisplayName("Remove sensor from repository")
    void removeSensor() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        securityService.removeSensor(sensor);

        verify(securityRepository).removeSensor(sensor);
    }

    @Test
    @DisplayName("Get alarm status from repository")
    void getAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        AlarmStatus status = securityService.getAlarmStatus();

        assertEquals(AlarmStatus.ALARM, status);
        verify(securityRepository).getAlarmStatus();
    }

    @Test
    @DisplayName("Get arming status from repository")
    void getArmingStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        ArmingStatus status = securityService.getArmingStatus();

        assertEquals(ArmingStatus.ARMED_HOME, status);
        verify(securityRepository).getArmingStatus();
    }

    @Test
    @DisplayName("Image service method is called with correct parameters")
    void processImage_callsImageService() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(image);

        verify(imageService).imageContainsCat(image, 50.0f);
    }

    @Test
    @DisplayName("Multiple sensors with different states")
    void multipleSensorsWithDifferentStates() {
        Sensor door = new Sensor("Front Door", SensorType.DOOR);
        Sensor window = new Sensor("Living Room Window", SensorType.WINDOW);
        door.setActive(true);
        window.setActive(false);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(door);
        sensors.add(window);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        Set<Sensor> result = securityService.getSensors();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.getName().equals("Front Door") && s.getActive()));
        assertTrue(result.stream().anyMatch(s -> s.getName().equals("Living Room Window") && !s.getActive()));
    }
}
