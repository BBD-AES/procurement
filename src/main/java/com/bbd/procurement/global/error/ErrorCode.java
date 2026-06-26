package com.bbd.procurement.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 오류가 발생했습니다."),

    // Vendor
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "V001", "공급사를 찾을 수 없습니다."),
    VENDOR_CODE_DUPLICATED(HttpStatus.CONFLICT, "V002", "이미 존재하는 공급사 코드입니다."),
    VENDOR_INACTIVE(HttpStatus.BAD_REQUEST, "V003", "비활성화된 공급사입니다."),
    VENDOR_CODE_INVALID(HttpStatus.BAD_REQUEST, "V004", "공급사 코드 형식이 올바르지 않습니다."),
    VENDOR_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "V005", "공급사명은 필수입니다."),
    VENDOR_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "V006", "이미 접수된 공급사 등록 요청입니다."),

    // PurchaseOrder
    PO_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "구매 주문을 찾을 수 없습니다."),
    PO_INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "P002", "허용되지 않는 PO 상태 전이입니다."),
    PO_NOT_EDITABLE(HttpStatus.CONFLICT, "P003", "DRAFT 상태의 PO만 수정할 수 있습니다."),
    PO_ALREADY_RECEIVED(HttpStatus.CONFLICT, "P004", "이미 입고 처리된 PO입니다."),
    PO_LINE_REQUIRED(HttpStatus.BAD_REQUEST, "P005", "PO에 최소 1개 이상의 라인이 필요합니다."),
    PO_LINE_INVALID(HttpStatus.BAD_REQUEST, "P006", "PO 라인 항목이 올바르지 않습니다."),
    PO_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "P007", "이미 접수된 주문입니다."),
    PO_FIELD_INVALID(HttpStatus.BAD_REQUEST, "P008", "PO 필드 값이 올바르지 않습니다."),

    // 요청 알림 클레임(처리중) — 구매/생산요청 공통
    REQUEST_NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "요청 알림을 찾을 수 없습니다."),
    REQUEST_ALREADY_CLAIMED(HttpStatus.CONFLICT, "R002", "이미 다른 담당자가 처리 중인 요청입니다."),
    REQUEST_CLAIM_FORBIDDEN(HttpStatus.FORBIDDEN, "R003", "본인이 처리 중인 요청만 해제할 수 있습니다."),

    // Item
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "해당 SKU의 부품을 찾을 수 없습니다."),
    ITEM_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "I002", "Item 서비스 호출에 실패했습니다."),

    // Sales
    SO_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "해당 번호의 SO를 찾을 수 없습니다."),
    SALES_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "Sales 서비스 호출에 실패했습니다."),

    // workOrder
    WORK_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "작업지시를 찾을 수 없습니다."),
    WORK_ORDER_INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "W002", "허용되지 않는 작업지시 상태 전이입니다."),
    WORK_ORDER_LINE_REQUIRED(HttpStatus.BAD_REQUEST, "W003", "작업지시에 최소 1개 이상의 라인이 필요합니다."),
    WORK_ORDER_LINE_INVALID(HttpStatus.BAD_REQUEST, "W004", "작업지시 라인 항목이 올바르지 않습니다."),
    WORK_ORDER_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "W005", "이미 접수된 작업지시입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
