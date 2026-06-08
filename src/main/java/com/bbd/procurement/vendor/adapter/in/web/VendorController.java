package com.bbd.procurement.vendor.adapter.in.web;

import com.bbd.procurement.global.auth.HasRole;
import com.bbd.procurement.global.auth.Role;
import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.vendor.adapter.in.web.request.ChangeVendorActivationRequest;
import com.bbd.procurement.vendor.adapter.in.web.request.RegisterVendorRequest;
import com.bbd.procurement.vendor.adapter.in.web.request.UpdateVendorRequest;
import com.bbd.procurement.vendor.adapter.in.web.response.VendorResponse;
import com.bbd.procurement.vendor.adapter.in.web.response.VendorSummaryResponse;
import com.bbd.procurement.vendor.application.port.in.*;
import com.bbd.procurement.vendor.domain.Vendor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final RegisterVendorUseCase registerVendorUseCase;
    private final UpdateVendorUseCase updateVendorUseCase;
    private final ChangeVendorActivationUseCase changeVendorActivationUseCase;
    private final GetVendorQuery getVendorQuery;
    private final ListVendorQuery listVendorQuery;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @HasRole({Role.HQ_MANAGER})
    public ApiResponse<VendorResponse> register(
            @Valid @RequestBody RegisterVendorRequest request
    ) {
        Vendor vendor = registerVendorUseCase.register(request.toCommand());
        return ApiResponse.success("공급사가 등록되었습니다.", VendorResponse.from(vendor));
    }

    @PatchMapping("/{code}")
    @HasRole({Role.HQ_MANAGER})
    public ApiResponse<VendorResponse> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateVendorRequest request
            ) {
        Vendor vendor = updateVendorUseCase.update(request.toCommand(code));
        return ApiResponse.success(VendorResponse.from(vendor));
    }

    @PatchMapping("/{code}/active")
    @HasRole({Role.HQ_MANAGER})
    public ApiResponse<VendorResponse> changeActivation(
            @PathVariable String code,
            @Valid @RequestBody ChangeVendorActivationRequest request
            ) {
        Vendor vendor = changeVendorActivationUseCase.changeActivation(request.toCommand(code));
        return ApiResponse.success(VendorResponse.from(vendor));
    }

    @GetMapping("/{code}")
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<VendorResponse> get(@PathVariable String code) {
        Vendor vendor = getVendorQuery.getByCode(code);
        return ApiResponse.success(VendorResponse.from(vendor));
    }

    @GetMapping
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<List<VendorSummaryResponse>> list() {
        List<VendorSummaryResponse> result = listVendorQuery.list().stream()
                .map(VendorSummaryResponse::from)
                .toList();
        return ApiResponse.success(result);
    }
}
