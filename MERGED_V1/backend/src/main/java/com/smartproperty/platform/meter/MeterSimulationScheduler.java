package com.smartproperty.platform.meter;
import com.smartproperty.platform.meter.service.MeterSimulationService;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;
@Component public class MeterSimulationScheduler {private final MeterSimulationService service;public MeterSimulationScheduler(MeterSimulationService s){service=s;}@Scheduled(fixedDelay=1000) public void tick(){service.scheduledTick();}}
