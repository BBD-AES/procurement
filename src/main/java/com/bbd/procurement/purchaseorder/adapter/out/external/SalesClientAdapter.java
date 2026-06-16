package com.bbd.procurement.purchaseorder.adapter.out.external;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.out.LoadSalesOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.SalesOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesClientAdapter implements LoadSalesOrderPort {

    private final SalesHttpService salesHttpService;

    @Override
    public SalesOrderResult findBySoNumber(String soNumber) {
        log.info("⭐️ findBySoNumber 시작하기!!");
        SalesOrderResponse response = getSalesOrder(soNumber);
        log.info("⭐️ findBySoNumber 끝내기!!");
        return toResult(response);
    }

    private SalesOrderResponse getSalesOrder(String soNumber) {
        try {
            return salesHttpService.getSalesOrder(soNumber);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(ErrorCode.SO_NOT_FOUND);
        } catch (Exception e) {
            log.info(e.getMessage());
            throw new ApiException(ErrorCode.SALES_SERVICE_ERROR);
        }
    }

    private SalesOrderResult toResult(SalesOrderResponse so) {
        return new SalesOrderResult(
                so.soNumber(),
                so.fromWarehouseCode(),
                so.fromWarehouseName(),
                so.toWarehouseCode(),
                so.toWarehouseName(),
                so.status(),
                so.priority(),
                so.requestedBy(),
                so.approvedBy(),
                so.receivedBy(),
                so.canceledBy(),
                so.requestedAt(),
                so.approvedAt(),
                so.canceledAt(),
                so.receivedAt(),
                so.totalAmount(),
                so.note(),
                so.lines().stream()
                        .map(line -> new SalesOrderResult.Line(
                                line.lineNo(),
                                line.sku(),
                                line.nameSnapshot(),
                                line.unitPriceSnapshot(),
                                line.quantity()
                        ))
                        .toList()
        );
    }

}
