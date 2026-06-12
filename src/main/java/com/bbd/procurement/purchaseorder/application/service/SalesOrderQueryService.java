package com.bbd.procurement.purchaseorder.application.service;

import com.bbd.procurement.purchaseorder.application.port.in.GetSalesOrderQuery;
import com.bbd.procurement.purchaseorder.application.port.out.LoadSalesOrderPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.SalesOrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalesOrderQueryService implements GetSalesOrderQuery {

    private final LoadSalesOrderPort loadSalesOrderPort;

    @Override
    public SalesOrderResult getBySoNumber(String soNumber) {
        return loadSalesOrderPort.findBySoNumber(soNumber);
    }
}
