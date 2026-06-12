package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.application.port.out.result.SalesOrderResult;

public interface GetSalesOrderQuery {

    SalesOrderResult getBySoNumber(String soNumber);

}
