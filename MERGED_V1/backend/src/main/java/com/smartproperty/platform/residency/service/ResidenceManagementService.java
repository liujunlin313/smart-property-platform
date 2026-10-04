package com.smartproperty.platform.residency.service;
import com.smartproperty.platform.residency.dto.*;import com.smartproperty.platform.residency.vo.*;import java.util.List;
public interface ResidenceManagementService {PersonOptionVO createPerson(CreatePersonRequest request);List<PersonOptionVO> searchPeople(String keyword);List<AvailablePropertyVO> availableProperties(Long personId,MoveInRequest.MoveInType type);MoveInResultVO moveIn(MoveInRequest request);void moveOut(Long residenceId,MoveOutRequest request);}
