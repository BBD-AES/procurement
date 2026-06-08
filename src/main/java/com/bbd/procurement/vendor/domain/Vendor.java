package com.bbd.procurement.vendor.domain;

import com.bbd.procurement.global.entity.BaseTimeEntity;
import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vendor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 10, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Vendor(String code, String name, String contact, String terms) {
        this.code = code;
        this.name = name;
        this.contact = contact;
        this.terms = terms;
        this.active = true;
    }

    // 신규 공급사 생성, code는 VendorCodeGeneratorPort 구현체가 채번한 값 주입
    public static Vendor create(String code, String name, String contact, String terms) {
        validateCode(code);
        validateName(name);
        return new Vendor(code, name, contact, terms);
    }

    // 공급사 정보 수정, code는 불변
    public void updateInfo(String name, String contact, String terms) {
        validateName(name);
        this.name = name;
        this.contact = contact;
        this.terms = terms;
    }

    public void activate() {
        if (this.active) {
            return;
        }
        this.active = true;
    }

    public void deactivate() {
        if (!this.active) {
            return;
        }
        this.active = false;
    }

    private static void validateCode(String code) {
        if (!StringUtils.hasText(code) || !code.matches("^V\\d{6}$")) {
            throw new ApiException(ErrorCode.VENDOR_CODE_INVALID);
        }
    }

    private static void validateName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ApiException(ErrorCode.VENDOR_NAME_REQUIRED);
        }
    }
}
