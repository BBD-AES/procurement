package com.bbd.procurement.vendor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class VendorTest {
    private static final String VALID_CODE = "V000001";
    private static final String VALID_NAME = "현대오토에버";
    private static final String CONTACT = "02-1234-5678";
    private static final String TERMS = "월말 결제, 30일";

    @Nested
    @DisplayName("Vendor.create")
    class Create {

        @Test
        @DisplayName("정상 입력이면 active = true 상태로 생성됨")
        void success() {
            Vendor vendor = Vendor.create(VALID_CODE, VALID_NAME, CONTACT, TERMS);

            assertThat(vendor.getCode()).isEqualTo(VALID_CODE);
            assertThat(vendor.getName()).isEqualTo(VALID_NAME);
            assertThat(vendor.getContact()).isEqualTo(CONTACT);
            assertThat(vendor.getTerms()).isEqualTo(TERMS);
            assertThat(vendor.isActive()).isTrue();
        }

        @Test
        @DisplayName("contact / terms는 null이어도 생성됨")
        void allowNullableFields() {
            Vendor vendor = Vendor.create(VALID_CODE, VALID_NAME, null, null);

            assertThat(vendor.getContact()).isNull();
            assertThat(vendor.getTerms()).isNull();
        }

    }
}
