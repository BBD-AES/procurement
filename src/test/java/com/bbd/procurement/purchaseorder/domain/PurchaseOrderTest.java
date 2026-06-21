package com.bbd.procurement.purchaseorder.domain;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PurchaseOrder 도메인 단위테스트 (이슈 #64).
 *
 * 상태전이/금액계산/검증 분기를 DB·스프링 컨텍스트 없이 순수 객체로 검증한다.
 * 외부 의존이 전혀 없는 도메인이므로 목 없이 그대로 생성해 테스트한다.
 */
class PurchaseOrderTest {

    private static final String VALID_PO = "PO-2026-000001";

    /** 라인 1개를 가진 정상 DRAFT PO 생성 헬퍼. */
    private PurchaseOrder draftWithOneLine() {
        PurchaseOrderLine line = PurchaseOrderLine.create(1, "SKU-1", "부품A", new BigDecimal("100"), 2);
        return PurchaseOrder.create(
                VALID_PO, "V001", "WH-HQ-001", "SO-1", null, "note", List.of(line), 1L, null);
    }

    @Nested
    @DisplayName("markReceived(입고완료 전이)")
    class MarkReceived {

        @Test
        @DisplayName("DRAFT + 라인 보유 + receivedBy 정상 → RECEIVED로 전이된다")
        void 정상_입고완료() {
            PurchaseOrder po = draftWithOneLine();

            po.markReceived(99L);

            assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
            assertThat(po.getReceivedBy()).isEqualTo(99L);
            assertThat(po.getReceivedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 RECEIVED 상태면 PO_ALREADY_RECEIVED 예외")
        void 이미_입고완료면_예외() {
            PurchaseOrder po = draftWithOneLine();
            po.markReceived(99L);

            assertThatThrownBy(() -> po.markReceived(99L))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PO_ALREADY_RECEIVED);
        }

        @Test
        @DisplayName("DRAFT가 아닌 상태(CANCELED)에서는 PO_INVALID_STATE_TRANSITION 예외")
        void 비DRAFT_전이_차단() {
            PurchaseOrder po = draftWithOneLine();
            po.cancel(); // DRAFT -> CANCELED

            assertThatThrownBy(() -> po.markReceived(99L))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("라인이 하나도 없으면 PO_LINE_REQUIRED 예외")
        void 라인없으면_예외() {
            PurchaseOrder po = PurchaseOrder.create(
                    VALID_PO, "V001", "WH-HQ-001", null, null, "note", List.of(), 1L, null);

            assertThatThrownBy(() -> po.markReceived(99L))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PO_LINE_REQUIRED);
        }

        @Test
        @DisplayName("receivedBy가 null이면 PO_INVALID_STATE_TRANSITION 예외")
        void receivedBy_누락이면_예외() {
            PurchaseOrder po = draftWithOneLine();

            assertThatThrownBy(() -> po.markReceived(null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("cancel(취소 전이)")
    class Cancel {

        @Test
        @DisplayName("DRAFT → CANCELED로 전이된다")
        void DRAFT_취소() {
            PurchaseOrder po = draftWithOneLine();

            po.cancel();

            assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        }

        @Test
        @DisplayName("이미 CANCELED면 멱등하게 그대로 둔다(예외 없음)")
        void CANCELED_멱등() {
            PurchaseOrder po = draftWithOneLine();
            po.cancel();

            po.cancel(); // 두 번째 호출도 예외 없이 통과

            assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
        }

        @Test
        @DisplayName("RECEIVED 상태는 취소할 수 없다 → PO_INVALID_STATE_TRANSITION")
        void RECEIVED_취소불가() {
            PurchaseOrder po = draftWithOneLine();
            po.markReceived(99L);

            assertThatThrownBy(po::cancel)
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("recalculateTotal(총액 계산)")
    class RecalculateTotal {

        @Test
        @DisplayName("생성 시 라인 subtotal 합으로 totalAmount가 계산된다")
        void 라인합산_정확성() {
            PurchaseOrderLine l1 = PurchaseOrderLine.create(1, "SKU-1", "부품A", new BigDecimal("100"), 2); // 200
            PurchaseOrderLine l2 = PurchaseOrderLine.create(2, "SKU-2", "부품B", new BigDecimal("50"), 3);  // 150

            PurchaseOrder po = PurchaseOrder.create(
                    VALID_PO, "V001", "WH-HQ-001", null, null, "note", List.of(l1, l2), 1L, null);

            assertThat(po.getTotalAmount()).isEqualByComparingTo("350");
        }

        @Test
        @DisplayName("라인이 없으면 총액은 0이다")
        void 라인없으면_0() {
            PurchaseOrder po = PurchaseOrder.create(
                    VALID_PO, "V001", "WH-HQ-001", null, null, "note", List.of(), 1L, null);

            assertThat(po.getTotalAmount()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("replaceLines 후 총액이 새 라인 기준으로 재계산된다")
        void 라인교체_재계산() {
            PurchaseOrder po = draftWithOneLine(); // 100 * 2 = 200
            PurchaseOrderLine newLine = PurchaseOrderLine.create(1, "SKU-9", "부품Z", new BigDecimal("10"), 5); // 50

            po.replaceLines(List.of(newLine));

            assertThat(po.getTotalAmount()).isEqualByComparingTo("50");
        }
    }

    @Nested
    @DisplayName("create/validateRequired(생성 검증)")
    class Validation {

        @Test
        @DisplayName("poNumber 정규식(^PO-\\d{4}-\\d{6}$)을 어기면 예외")
        void poNumber_형식위반() {
            assertThatThrownBy(() -> PurchaseOrder.create(
                    "PO-26-1", "V001", "WH-HQ-001", null, null, "note", List.of(), 1L, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("vendorCode가 비어 있으면 예외")
        void vendorCode_누락() {
            assertThatThrownBy(() -> PurchaseOrder.create(
                    VALID_PO, "", "WH-HQ-001", null, null, "note", List.of(), 1L, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("createdBy가 null이면 예외")
        void createdBy_누락() {
            assertThatThrownBy(() -> PurchaseOrder.create(
                    VALID_PO, "V001", "WH-HQ-001", null, null, "note", List.of(), null, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("정상 입력이면 DRAFT 상태로 생성된다")
        void 정상생성() {
            PurchaseOrder po = PurchaseOrder.create(
                    VALID_PO, "V001", "WH-HQ-001", null, null, "note", List.of(), 1L, null);

            assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(po.getPoNumber()).isEqualTo(VALID_PO);
            assertThat(po.isEditable()).isTrue();
        }
    }
}
