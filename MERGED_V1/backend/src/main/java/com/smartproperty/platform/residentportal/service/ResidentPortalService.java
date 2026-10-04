package com.smartproperty.platform.residentportal.service;
import com.smartproperty.platform.residentportal.dto.*;import com.smartproperty.platform.residentportal.vo.ResidentPortalVO.*;import java.util.*;
public interface ResidentPortalService {SummaryVO summary();List<ResidenceVO> residences();List<PropertyVO> properties();List<BillVO> bills();List<NotificationVO> notifications();List<IncidentVO> myIncidents();IncidentVO submitIncident(ResidentIncidentCreateRequest request);List<IncidentVO> scopedIncidents();void updateIncidentStatus(Long id,IncidentStatusRequest request);}
