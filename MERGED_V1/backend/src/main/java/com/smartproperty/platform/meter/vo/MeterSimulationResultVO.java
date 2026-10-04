package com.smartproperty.platform.meter.vo;
public record MeterSimulationResultVO(int requested,int inserted,int alreadyExists,int failed,String bucket){}
