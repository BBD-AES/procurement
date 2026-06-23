package com.bbd.procurement.vendor.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.vendor.application.port.in.*;
import com.bbd.procurement.vendor.application.port.in.command.ChangeVendorActivationCommand;
import com.bbd.procurement.vendor.application.port.in.command.RegisterVendorCommand;
import com.bbd.procurement.vendor.application.port.in.command.UpdateVendorCommand;
import com.bbd.procurement.vendor.application.port.out.LoadVendorPort;
import com.bbd.procurement.vendor.application.port.out.SaveVendorPort;
import com.bbd.procurement.vendor.application.port.out.VendorCodeGeneratorPort;
import com.bbd.procurement.vendor.domain.Vendor;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService implements
        RegisterVendorUseCase,
        UpdateVendorUseCase,
        ChangeVendorActivationUseCase,
        GetVendorQuery,
        ListVendorQuery {
    private final SaveVendorPort saveVendorPort;
    private final LoadVendorPort loadVendorPort;
    private final VendorCodeGeneratorPort vendorCodeGeneratorPort;

    @Override
    @Transactional
    public Vendor register(RegisterVendorCommand command) {
        // 멱등 사전 조회: 동일 requestId로 이미 등록한 공급사가 있으면 새로 만들지 않고 그대로 반환(replay).
        // (시간차 더블클릭/재시도를 여기서 흡수한다. requestId 미전송 레거시 요청은 건너뛰고 기존대로 생성.)
        if (StringUtils.hasText(command.requestId())) {
            Optional<Vendor> existing = loadVendorPort.findByRequestId(command.requestId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        String code = vendorCodeGeneratorPort.generate();
        Vendor vendor = Vendor.create(code, command.name(), command.contact(), command.terms(), command.requestId());
        try {
            return saveVendorPort.save(vendor);
        } catch (DataIntegrityViolationException e) {
            // 거의 동시에 들어온 요청들이 사전 조회를 모두 통과한 경우(TOCTOU):
            // uq_vendor_request UNIQUE 제약이 두 번째 INSERT를 거부 → 중복 등록 요청(409)으로 변환.
            // 그 외(uk_vendor_code 등)는 기존대로 공급사 코드 중복(409)으로 처리.
            if (isRequestIdConflict(e)) {
                throw new ApiException(ErrorCode.VENDOR_DUPLICATE_REQUEST);
            }
            throw new ApiException(ErrorCode.VENDOR_CODE_DUPLICATED);
        }
    }

    // 제약 위반 원인이 멱등키(uq_vendor_request)인지 식별한다.
    private boolean isRequestIdConflict(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.toLowerCase().contains("uq_vendor_request")) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public Vendor update(UpdateVendorCommand command) {
        Vendor vendor = findVendorOrThrow(command.code());
        vendor.updateInfo(command.name(), command.contact(), command.terms());
        return vendor;
    }

    @Override
    @Transactional
    public Vendor changeActivation(ChangeVendorActivationCommand command) {
        Vendor vendor = findVendorOrThrow(command.code());
        if (command.active()) {
            vendor.activate();
        } else {
            vendor.deactivate();
        }
        return vendor;
    }

    @Override
    public Vendor getByCode(String code) {
        return findVendorOrThrow(code);
    }

    @Override
    public List<Vendor> list() {
        return loadVendorPort.findAll();
    }

    private Vendor findVendorOrThrow(String code) {
        return loadVendorPort.findByCode(code)
                .orElseThrow(() -> new ApiException(ErrorCode.VENDOR_NOT_FOUND));
    }
}
