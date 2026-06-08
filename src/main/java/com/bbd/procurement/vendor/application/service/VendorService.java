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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        String code = vendorCodeGeneratorPort.generate();

        if (loadVendorPort.existsByCode(code)) {
            throw new ApiException(ErrorCode.VENDOR_CODE_DUPLICATED);
        }

        Vendor vendor = Vendor.create(code, command.name(), command.contact(), command.terms());
        return saveVendorPort.save(vendor);
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
