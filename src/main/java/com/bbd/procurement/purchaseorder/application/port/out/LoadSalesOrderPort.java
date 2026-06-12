package com.bbd.procurement.purchaseorder.application.port.out;

import com.bbd.procurement.purchaseorder.application.port.out.result.SalesOrderResult;

public interface LoadSalesOrderPort {

    SalesOrderResult findBySoNumber(String soNumber);

}
