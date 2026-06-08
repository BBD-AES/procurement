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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Vendor", description = "공급사(Vendor) 관리 API")
@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final RegisterVendorUseCase registerVendorUseCase;
    private final UpdateVendorUseCase updateVendorUseCase;
    private final ChangeVendorActivationUseCase changeVendorActivationUseCase;
    private final GetVendorQuery getVendorQuery;
    private final ListVendorQuery listVendorQuery;

    @Operation(
            summary = "공급사 등록",
            description = "신규 공급사를 등록 | 권한: HQ_MANAGER"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @HasRole({Role.HQ_MANAGER})
    public ApiResponse<VendorResponse> register(
            @Valid @RequestBody RegisterVendorRequest request
    ) {
        Vendor vendor = registerVendorUseCase.register(request.toCommand());
        return ApiResponse.success("공급사가 등록되었습니다.", VendorResponse.from(vendor));
    }

    @Operation(
            summary = "공급사 수정",
            description = "기존 공급사 정보를 수정 | 권한 : HQ_MANAGER"
    )
    @PatchMapping("/{code}")
    @HasRole({Role.HQ_MANAGER})
    public ApiResponse<VendorResponse> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateVendorRequest request
            ) {
        Vendor vendor = updateVendorUseCase.update(request.toCommand(code));
        return ApiResponse.success(VendorResponse.from(vendor));
    }

    @Operation(
            summary = "공급사 활성/비활성 전환",
            description = "공급사의 활성 상태를 변경 | 권한 : HQ_MANAGER"
    )
    @PatchMapping("/{code}/active")
    @HasRole({Role.HQ_MANAGER})
    public ApiResponse<VendorResponse> changeActivation(
            @PathVariable String code,
            @Valid @RequestBody ChangeVendorActivationRequest request
            ) {
        Vendor vendor = changeVendorActivationUseCase.changeActivation(request.toCommand(code));
        return ApiResponse.success(VendorResponse.from(vendor));
    }

    @Operation(
            summary = "공급사 단건 조회",
            description = "공급사 상세 정보를 조회 | 권한 : HQ_MANAGER, HQ_STAFF"
    )
    @GetMapping("/{code}")
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<VendorResponse> get(@PathVariable String code) {
        Vendor vendor = getVendorQuery.getByCode(code);
        return ApiResponse.success(VendorResponse.from(vendor));
    }

    @Operation(
            summary = "공급사 목록 조회",
            description = "전체 공급사 요약 목록을 조회 | 권한 : HQ_MANAGER, HQ_STAFF"
    )
    @GetMapping
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<List<VendorSummaryResponse>> list() {
        List<VendorSummaryResponse> result = listVendorQuery.list().stream()
                .map(VendorSummaryResponse::from)
                .toList();
        return ApiResponse.success(result);
    }
}
