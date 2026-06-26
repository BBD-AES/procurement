package com.bbd.procurement.purchaseorder.application.port.in;

import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;

import java.util.List;

public interface ListPurchaseOrderQuery {

    List<PurchaseOrder> list();

    /** 특정 SO 연계 PO 목록(최신순). 요청 알림 상세에서 역조회용. */
    List<PurchaseOrder> listBySoNumber(String soNumber);

}
