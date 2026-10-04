package com.smartproperty.platform.meter.service;
import com.smartproperty.platform.meter.vo.MeterSimulationResultVO;
public interface MeterSimulationService {MeterSimulationResultVO simulateNow();void scheduledTick();}
