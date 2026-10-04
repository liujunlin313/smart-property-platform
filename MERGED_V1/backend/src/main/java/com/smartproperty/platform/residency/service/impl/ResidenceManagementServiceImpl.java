package com.smartproperty.platform.residency.service.impl;
import com.smartproperty.platform.access.*;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.residency.dto.*;
import com.smartproperty.platform.residency.mapper.ResidenceManagementMapper;
import com.smartproperty.platform.residency.mapper.ResidenceManagementMapper.*;
import com.smartproperty.platform.residency.service.ResidenceManagementService;
import com.smartproperty.platform.residency.vo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
@Service
public class ResidenceManagementServiceImpl implements ResidenceManagementService {
  private static final Set<String> PARKING_INELIGIBILITY_REASONS=Set.of("OWNER_NOT_GRANTED","NO_AVAILABLE_QUOTA","LEASE_EXCLUDES_PARKING","OTHER");
  private final ResidenceManagementMapper mapper;
  private final AuthorizationService authorization;
  private final DataScopeService scope;

  public ResidenceManagementServiceImpl(ResidenceManagementMapper a,AuthorizationService b,DataScopeService c) {
   mapper=a;authorization=b;scope=c;
 }
 public PersonOptionVO createPerson(CreatePersonRequest r){
  authorization.requirePermission(PermissionCodes.RESIDENT_MANAGE);
  var c=new PersonCreateCommand("H"+DateTimeFormatter.ofPattern("yyMMddHHmmssSSS").format(LocalDateTime.now()),r.fullName().trim(),gender(r.gender()),blank(r.phone()));mapper.insertPerson(c);
  return new PersonOptionVO(c.getPersonId(),c.getFullName(),c.getPhone());
 }
 public List<PersonOptionVO> searchPeople(String k){
   authorization.requirePermission(PermissionCodes.RESIDENT_MANAGE);
   return mapper.searchPeople(blank(k),30);
  }
  public List<AvailablePropertyVO> availableProperties(Long p,MoveInRequest.MoveInType t){
   authorization.requirePermission(PermissionCodes.RESIDENT_MANAGE);
   if(p==null||t==null)throw bad("PERSON_AND_TYPE_REQUIRED","请选择人员和入住性质");
   var s=scope.currentCommunityScope();
   return mapper.selectAvailableProperties(p,t==MoveInRequest.MoveInType.OWNER_OCCUPY,!s.global(),s.communityIds());
  }
 @Transactional
 public MoveInResultVO moveIn(MoveInRequest r){
    authorization.requirePermission(PermissionCodes.RESIDENT_MANAGE);
    dates(r);
    boolean tenant=r.moveInType()==MoveInRequest.MoveInType.TENANT;
    boolean eligible=Boolean.TRUE.equals(r.parkingApplicationEligible());
    String reason=tenant&&!eligible?blank(r.parkingIneligibleReasonCode()):null;
    if(tenant&&!eligible&&reason==null)throw bad("PARKING_INELIGIBILITY_REASON_REQUIRED","请选择不允许申请车证的原因");
    if(tenant&&!eligible&&!PARKING_INELIGIBILITY_REASONS.contains(reason))throw bad("PARKING_INELIGIBILITY_REASON_INVALID","停车资格原因无效，请重新选择");
    if(!mapper.existsActivePerson(r.personId()))throw bad("PERSON_NOT_FOUND","人员不存在或已停用");
    scope.requireProperty(r.propertyUnitId());

    var p=mapper.selectPropertyForClaim(r.propertyUnitId());

    if(p==null||!"ACTIVE".equals(p.status())||!("VACANT".equals(p.occupancyStatus())||"AVAILABLE_FOR_RENT".equals(p.occupancyStatus())))throw conflict();
    if(r.moveInType()==MoveInRequest.MoveInType.OWNER_OCCUPY&&!mapper.existsActiveOwnership(r.personId(),r.propertyUnitId()))throw bad("OWNERSHIP_REQUIRED","产权人自住必须先存在有效产权关系");
    String target=r.moveInType()==MoveInRequest.MoveInType.OWNER_OCCUPY?"OWNER_OCCUPIED":"TENANT_OCCUPIED";

    if(mapper.claimProperty(p.propertyUnitId(),p.version(),target)!=1)throw conflict();

    var c=new ResidenceCommand(r.personId(),r.propertyUnitId(),r.startDate(),r.expectedEndDate());

    mapper.insertResidence(c);

    if(tenant){mapper.insertParkingEligibility(new ParkingEligibilityCommand(c.getResidenceId(),eligible,reason,blank(r.parkingEligibilityRemark())));}
    
    return new MoveInResultVO(c.getResidenceId(),r.personId(),r.propertyUnitId(),target,p.version()+1);
  }
 @Transactional
 public void moveOut(Long id,MoveOutRequest r){
   authorization.requirePermission(PermissionCodes.RESIDENT_MANAGE);
   var x=mapper.selectActiveResidence(id);
   if(x==null)throw new BusinessException(HttpStatus.NOT_FOUND,"ACTIVE_RESIDENCE_NOT_FOUND","未找到当前入住记录");
   scope.requireProperty(x.propertyUnitId());
   if(!"TENANT_OCCUPIED".equals(x.occupancyStatus()))throw bad("TENANT_MOVE_OUT_ONLY","当前仅支持租赁入住办理退租");
   if(r.moveOutDate().isBefore(x.startDate()))throw bad("MOVE_OUT_DATE_INVALID","实际退租日期不能早于入住日期");
   if(mapper.endResidence(id,r.moveOutDate())!=1)throw conflict();
   if(mapper.countActiveResidence(x.propertyUnitId())==0){
      String st=mapper.existsOpenRentalListing(x.propertyUnitId())?"AVAILABLE_FOR_RENT":"VACANT";
      if(mapper.releaseProperty(x.propertyUnitId(),x.propertyVersion(),st)!=1)throw conflict();
     }
  }
 private void dates(MoveInRequest r){
   if(r.expectedEndDate()!=null&&r.expectedEndDate().isBefore(r.startDate()))throw bad("EXPECTED_END_DATE_INVALID","预计结束日期不能早于入住日期");
   if(r.moveInType()==MoveInRequest.MoveInType.TENANT&&r.expectedEndDate()==null)throw bad("EXPECTED_END_DATE_REQUIRED","租赁入住需要填写预计租期结束日期");
  }
  private String gender(String v){
   return "MALE".equals(v)||"FEMALE".equals(v)?v:"UNKNOWN";
  }
  private String blank(String v){
   return v==null||v.isBlank()?null:v.trim();
  }private BusinessException bad(String c,String m){return new BusinessException(HttpStatus.BAD_REQUEST,c,m);}private BusinessException conflict(){return new BusinessException(HttpStatus.CONFLICT,"PROPERTY_STATE_CHANGED","该房屋状态已经变化，请刷新后重新选择");}}
