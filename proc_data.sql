--
-- PostgreSQL database dump
--

\restrict czPU88Anu2szDmaQXGFRaxTgZruWsIZUd6ZmvRBCDTIsgbISuXAuj5y4HNkKBPM

-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: purchase_order; Type: TABLE DATA; Schema: public; Owner: -
--

COPY bbd.purchase_order (id, po_number, vendor_code, warehouse_code, status, total_amount, expected_arrival, note, created_by, received_by, created_at, updated_at, received_at, so_number) FROM stdin;
1	PO-2026-000001	V000001	WH-HQ-001	DRAFT	1650000.00	2026-06-30	테스트 발주	E001	\N	2026-06-09 16:57:50.341244	2026-06-09 16:57:50.341244	\N	\N
3	PO-2026-000003	V000006	WH-HQ-001	DRAFT	10000.00	2026-07-01	확정 테스트	E001	\N	2026-06-10 11:45:05.959119	2026-06-10 11:45:05.959119	\N	\N
2	PO-2026-000002	V000003	W000003	CANCELED	3000.00	2026-06-13	예상 도착일이 3일 지연되었습니다.	E001	\N	2026-06-10 09:45:59.608812	2026-06-10 11:47:32.599289	\N	SO-2026-000003
4	PO-2026-000004	V000001	WH-HQ-001	DRAFT	1250000.00	2026-07-01	complete 테스트	E001	\N	2026-06-11 01:09:54.999678	2026-06-11 01:09:54.999678	\N	\N
5	PO-2026-000005	V000001	WH-HQ-001	DRAFT	1250000.00	2026-07-01	complete 테스트	E001	\N	2026-06-11 01:13:06.337921	2026-06-11 01:13:06.337921	\N	\N
6	PO-2026-000006	V000001	WH-HQ-001	DRAFT	1250000.00	2026-07-01	complete 테스트	E001	\N	2026-06-11 01:14:30.874347	2026-06-11 01:14:30.874347	\N	\N
7	PO-2026-000007	V000001	WH-HQ-001	RECEIVED	1250000.00	2026-07-01	complete 테스트	E001	E001	2026-06-11 01:24:02.099786	2026-06-11 01:24:11.934791	2026-06-11 01:24:11.924663	\N
8	PO-2026-000010	V000001	WH-HQ-001	DRAFT	0.00	2026-06-12	인증 제거 검증용	SYSTEM	\N	2026-06-12 17:11:17.689887	2026-06-12 17:11:17.689887	\N	\N
9	PO-2026-000011	V000001	WH-HQ-001	CANCELED	0.00	\N	헤더 사번 검증	EMP-001	\N	2026-06-12 17:11:33.812975	2026-06-12 17:11:43.175626	\N	\N
11	PO-2026-000013	V000001	WH-HQ-001	CANCELED	0.00	2026-06-12	스웨거 검증 2	EMP-001	\N	2026-06-12 17:16:04.711117	2026-06-12 17:17:23.598791	\N	\N
12	PO-2026-000014	V000002	WH-002	RECEIVED	0.00	2026-06-14	카프카 브로커 복구 설정 테스트	SYSTEM	EMP-TEST	2026-06-15 02:01:57.838217	2026-06-15 02:05:37.230437	2026-06-15 02:05:37.211361	S000002
10	PO-2026-000012	V000001	WH-HQ-001	RECEIVED	0.00	2026-06-12	인증 제거 검증용	SYSTEM	EMP-FIX46	2026-06-12 17:14:23.771108	2026-06-15 03:06:00.777082	2026-06-15 03:06:00.756535	\N
13	PO-2026-000015	V000001	WH-HQ-001	RECEIVED	0.00	\N	장애회귀 검증용	EMP-FIX46	EMP-FIX46	2026-06-15 03:06:44.551733	2026-06-15 03:08:53.969547	2026-06-15 03:08:53.946247	\N
\.


--
-- Data for Name: purchase_order_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY bbd.purchase_order_history (id, po_number, change_type, before_payload, after_payload, changed_by, changed_at) FROM stdin;
1	PO-2026-000010	CREATED	\N	{"poNumber":"PO-2026-000010","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0,"expectedArrival":"2026-06-12","note":"인증 제거 검증용","createdBy":"SYSTEM","receivedBy":null,"receivedAt":null,"lines":[]}	SYSTEM	2026-06-12 17:11:17.723998
2	PO-2026-000011	CREATED	\N	{"poNumber":"PO-2026-000011","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0,"expectedArrival":null,"note":"헤더 사번 검증","createdBy":"EMP-001","receivedBy":null,"receivedAt":null,"lines":[]}	EMP-001	2026-06-12 17:11:33.817434
3	PO-2026-000011	CANCELED	{"poNumber":"PO-2026-000011","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0.00,"expectedArrival":null,"note":"헤더 사번 검증","createdBy":"EMP-001","receivedBy":null,"receivedAt":null,"lines":[]}	{"poNumber":"PO-2026-000011","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"CANCELED","totalAmount":0.00,"expectedArrival":null,"note":"헤더 사번 검증","createdBy":"EMP-001","receivedBy":null,"receivedAt":null,"lines":[]}	EMP-002	2026-06-12 17:11:43.172158
4	PO-2026-000012	CREATED	\N	{"poNumber":"PO-2026-000012","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0,"expectedArrival":"2026-06-12","note":"인증 제거 검증용","createdBy":"SYSTEM","receivedBy":null,"receivedAt":null,"lines":[]}	SYSTEM	2026-06-12 17:14:23.779014
5	PO-2026-000013	CREATED	\N	{"poNumber":"PO-2026-000013","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0,"expectedArrival":"2026-06-12","note":"스웨거 검증 2","createdBy":"EMP-001","receivedBy":null,"receivedAt":null,"lines":[]}	EMP-001	2026-06-12 17:16:04.717524
6	PO-2026-000013	CANCELED	{"poNumber":"PO-2026-000013","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0.00,"expectedArrival":"2026-06-12","note":"스웨거 검증 2","createdBy":"EMP-001","receivedBy":null,"receivedAt":null,"lines":[]}	{"poNumber":"PO-2026-000013","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"CANCELED","totalAmount":0.00,"expectedArrival":"2026-06-12","note":"스웨거 검증 2","createdBy":"EMP-001","receivedBy":null,"receivedAt":null,"lines":[]}	EMP-002	2026-06-12 17:17:23.58424
7	PO-2026-000014	CREATED	\N	{"poNumber":"PO-2026-000014","vendorCode":"V000002","warehouseCode":"WH-002","soNumber":"S000002","status":"DRAFT","totalAmount":0,"expectedArrival":"2026-06-14","note":"카프카 브로커 복구 설정 테스트","createdBy":"SYSTEM","receivedBy":null,"receivedAt":null,"lines":[]}	SYSTEM	2026-06-15 02:01:57.870036
8	PO-2026-000014	COMPLETED	{"poNumber":"PO-2026-000014","vendorCode":"V000002","warehouseCode":"WH-002","soNumber":"S000002","status":"DRAFT","totalAmount":0.00,"expectedArrival":"2026-06-14","note":"카프카 브로커 복구 설정 테스트","createdBy":"SYSTEM","receivedBy":null,"receivedAt":null,"lines":[{"lineOrder":1,"sku":"SKU-TEST-001","partName":"카프카검증부품","unitPrice":1000.00,"quantity":10,"subtotal":10000.00}]}	{"poNumber":"PO-2026-000014","vendorCode":"V000002","warehouseCode":"WH-002","soNumber":"S000002","status":"RECEIVED","totalAmount":0.00,"expectedArrival":"2026-06-14","note":"카프카 브로커 복구 설정 테스트","createdBy":"SYSTEM","receivedBy":"EMP-TEST","receivedAt":"2026-06-15T02:05:37.211361","lines":[{"lineOrder":1,"sku":"SKU-TEST-001","partName":"카프카검증부품","unitPrice":1000.00,"quantity":10,"subtotal":10000.00}]}	EMP-TEST	2026-06-15 02:05:37.226984
9	PO-2026-000012	COMPLETED	{"poNumber":"PO-2026-000012","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0.00,"expectedArrival":"2026-06-12","note":"인증 제거 검증용","createdBy":"SYSTEM","receivedBy":null,"receivedAt":null,"lines":[{"lineOrder":1,"sku":"SKU-FIX46-A","partName":"정상발행검증","unitPrice":2000.00,"quantity":5,"subtotal":10000.00}]}	{"poNumber":"PO-2026-000012","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"RECEIVED","totalAmount":0.00,"expectedArrival":"2026-06-12","note":"인증 제거 검증용","createdBy":"SYSTEM","receivedBy":"EMP-FIX46","receivedAt":"2026-06-15T03:06:00.756535","lines":[{"lineOrder":1,"sku":"SKU-FIX46-A","partName":"정상발행검증","unitPrice":2000.00,"quantity":5,"subtotal":10000.00}]}	EMP-FIX46	2026-06-15 03:06:00.770024
10	PO-2026-000015	CREATED	\N	{"poNumber":"PO-2026-000015","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0,"expectedArrival":null,"note":"장애회귀 검증용","createdBy":"EMP-FIX46","receivedBy":null,"receivedAt":null,"lines":[]}	EMP-FIX46	2026-06-15 03:06:44.555804
11	PO-2026-000015	COMPLETED	{"poNumber":"PO-2026-000015","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"DRAFT","totalAmount":0.00,"expectedArrival":null,"note":"장애회귀 검증용","createdBy":"EMP-FIX46","receivedBy":null,"receivedAt":null,"lines":[{"lineOrder":1,"sku":"SKU-FIX46-B","partName":"장애회귀부품","unitPrice":3000.00,"quantity":2,"subtotal":6000.00}]}	{"poNumber":"PO-2026-000015","vendorCode":"V000001","warehouseCode":"WH-HQ-001","soNumber":null,"status":"RECEIVED","totalAmount":0.00,"expectedArrival":null,"note":"장애회귀 검증용","createdBy":"EMP-FIX46","receivedBy":"EMP-FIX46","receivedAt":"2026-06-15T03:08:53.946247","lines":[{"lineOrder":1,"sku":"SKU-FIX46-B","partName":"장애회귀부품","unitPrice":3000.00,"quantity":2,"subtotal":6000.00}]}	EMP-FIX46	2026-06-15 03:08:53.962012
\.


--
-- Data for Name: purchase_order_line; Type: TABLE DATA; Schema: public; Owner: -
--

COPY bbd.purchase_order_line (id, purchase_order_id, line_order, sku, part_name, unit_price, quantity, subtotal) FROM stdin;
1	1	1	SKU-1001	엔진오일	12500.00	100	1250000.00
2	1	2	SKU-1002	필터	8000.00	50	400000.00
3	2	1	D000001	못	3000.00	1	3000.00
4	3	1	SKU-3001	테스트	1000.00	10	10000.00
5	4	1	SKU-1001	테스트	12500.00	100	1250000.00
6	5	1	SKU-1001	테스트	12500.00	100	1250000.00
7	6	1	SKU-1001	테스트	12500.00	100	1250000.00
8	7	1	SKU-1001	테스트	12500.00	100	1250000.00
9	12	1	SKU-TEST-001	카프카검증부품	1000.00	10	10000.00
10	10	1	SKU-FIX46-A	정상발행검증	2000.00	5	10000.00
11	13	1	SKU-FIX46-B	장애회귀부품	3000.00	2	6000.00
\.


--
-- Data for Name: vendor; Type: TABLE DATA; Schema: public; Owner: -
--

COPY bbd.vendor (id, code, name, contact, terms, active, created_at, updated_at) FROM stdin;
1	V000001	현대오토에버 웹/앱	010-000-000	월말 결제	t	2026-06-07 14:27:27.5722	2026-06-07 14:28:37.844564
2	V000002	curl 테스트	02-1111	월말	t	2026-06-08 22:40:34.438027	2026-06-08 22:40:34.438027
3	V000003	(주)두산건설	010-222-2222	현금결제	t	2026-06-10 09:43:40.895654	2026-06-10 09:44:49.365901
\.


--
-- Name: purchase_order_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('bbd.purchase_order_history_id_seq', 11, true);


--
-- Name: purchase_order_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('bbd.purchase_order_id_seq', 13, true);


--
-- Name: purchase_order_line_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('bbd.purchase_order_line_id_seq', 11, true);


--
-- Name: vendor_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('bbd.vendor_id_seq', 3, true);


--
-- PostgreSQL database dump complete
--

\unrestrict czPU88Anu2szDmaQXGFRaxTgZruWsIZUd6ZmvRBCDTIsgbISuXAuj5y4HNkKBPM

