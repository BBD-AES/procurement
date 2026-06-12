package com.bbd.procurement.purchaseorder.adapter.out.external;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.purchaseorder.application.port.out.LoadSalesOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.SalesOrderResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class SalesRestClient implements LoadSalesOrderPort {

    private final RestClient restClient;

    public SalesRestClient(@Value("${sales.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public SalesOrderResult findBySoNumber(String soNumber) {
        SalesOrderResponse response = getSalesOrder(soNumber);
        return toResult(response);
    }

    private SalesOrderResponse getSalesOrder(String soNumber) {
        try {
            return restClient.get()
                    .uri("/api/v1/sales-orders/{soNumber}", soNumber)
                    .retrieve()
                    .body(SalesOrderResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(ErrorCode.SO_NOT_FOUND);
        } catch (Exception e) {
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
