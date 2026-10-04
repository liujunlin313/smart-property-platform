package com.smartproperty.platform.common.exception;

import com.smartproperty.platform.common.api.ApiResponse;
import com.smartproperty.platform.common.web.RequestTrace;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return failure(exception.status(), exception.code(), exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return failure(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "提交的数据不完整或格式不正确", fields);
    }

    @ExceptionHandler({ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> handleInvalidArgument(Exception exception) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "请求参数不正确", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableJson(HttpMessageNotReadableException exception) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_JSON", "请求 JSON 格式不正确", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException exception) {
        return failure(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "请求的资源不存在", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return failure(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "该接口不支持当前请求方法", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleUnsupportedMedia(HttpMediaTypeNotSupportedException exception) {
        return failure(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "不支持当前请求内容类型", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleIntegrity(DataIntegrityViolationException exception) {
        String databaseMessage = exception.getMostSpecificCause().getMessage();
        String code = "DATA_CONFLICT";
        String friendlyMessage = "数据与现有记录冲突，请检查编号或关联关系";
        if (databaseMessage != null && databaseMessage.contains("uk_building_community_no")) {
            code = "BUILDING_NUMBER_DUPLICATE";
            friendlyMessage = "同一小区内楼栋号不能重复";
        } else if (databaseMessage != null && databaseMessage.contains("uk_building_unit_building_no")) {
            code = "BUILDING_UNIT_NUMBER_DUPLICATE";
            friendlyMessage = "同一楼栋内单元号不能重复";
        } else if (databaseMessage != null && databaseMessage.contains("uk_property_unit_parent_no")) {
            code = "PROPERTY_NUMBER_DUPLICATE";
            friendlyMessage = "同一单元内房号不能重复";
        } else if (databaseMessage != null && databaseMessage.contains("uk_community_code")) {
            code = "COMMUNITY_CODE_DUPLICATE";
            friendlyMessage = "小区业务编码已经存在";
        }
        log.warn("trace={} database constraint rejected the request: {}",
                RequestTrace.currentTraceId(), databaseMessage);
        return failure(HttpStatus.CONFLICT, code, friendlyMessage, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException exception) {
        return failure(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "当前账号没有执行此操作的权限", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return failure(HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE", "上传文件超过允许大小", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        String traceId = RequestTrace.currentTraceId();
        log.error("trace={} unhandled request failure", traceId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("INTERNAL_ERROR", "系统暂时无法完成操作，请稍后重试",
                        null, HttpStatus.INTERNAL_SERVER_ERROR.value(), traceId));
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatus status,
                                                       String code,
                                                       String message,
                                                       Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(ApiResponse.failure(
                code, message, fieldErrors, status.value(), RequestTrace.currentTraceId()));
    }
}
