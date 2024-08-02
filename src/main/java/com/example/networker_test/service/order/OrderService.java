package com.example.networker_test.service.order;

import com.example.networker_test.domain.order.CartItems;
import com.example.networker_test.domain.order.OrderInfo;
import com.example.networker_test.domain.order.PaymentInfo;
import com.example.networker_test.domain.order.ShippingInfo;
import com.example.networker_test.dto.order.cartItem.CartItemsDTO;
import com.example.networker_test.dto.order.orderinfo.OrderInfoDTO;
import com.example.networker_test.dto.order.orderinfo.UserOrderInfoDTO;
import com.example.networker_test.dto.order.paymentinfo.PaymentInfoDTO;

import com.example.networker_test.dto.order.request.OrderRequest;
import com.example.networker_test.dto.order.shippinginfo.ShippingInfoDTO;
import com.example.networker_test.repository.order.CartItemsRepository;
import com.example.networker_test.repository.order.OrderInfoRepository;
import com.example.networker_test.repository.order.PaymentInfoRepository;
import com.example.networker_test.repository.order.ShippingInfoRepository;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = Logger.getLogger(OrderService.class.getName());

    private final OrderInfoRepository orderInfoRepository;
    private final ShippingInfoRepository shippingInfoRepository;
    private final PaymentInfoRepository paymentInfoRepository;
    private final CartItemsRepository cartItemsRepository;

    public OrderService(OrderInfoRepository orderInfoRepository, ShippingInfoRepository shippingInfoRepository, PaymentInfoRepository paymentInfoRepository, CartItemsRepository cartItemsRepository) {
        this.orderInfoRepository = orderInfoRepository;
        this.shippingInfoRepository = shippingInfoRepository;
        this.paymentInfoRepository = paymentInfoRepository;
        this.cartItemsRepository = cartItemsRepository;
    }


    public List<UserOrderInfoDTO> getUserOrdersByUserId(String userid) {
        List<OrderInfo> orders = orderInfoRepository.findByUserid(userid);
        return orders.stream().map(this::convertToUserOrderDTO).collect(Collectors.toList());
    }

    private UserOrderInfoDTO convertToUserOrderDTO(OrderInfo orderInfo) {
        UserOrderInfoDTO dto = new UserOrderInfoDTO();
        dto.setOrderId(orderInfo.getOrderId());
        dto.setCreatedAt(orderInfo.getCreatedAt());

        // cartItems 가져오기
        List<CartItems> cartItemsList = cartItemsRepository.findByOrderInfo(orderInfo);
        if (!cartItemsList.isEmpty()) {
            CartItems firstItem = cartItemsList.get(0);
            dto.setImage(firstItem.getProductImage());
            dto.setProductName(firstItem.getProductName());
        }

        dto.setCount(cartItemsList.size());

        // PaymentInfo에서 총 결제 금액 가져오기
        PaymentInfo paymentInfo = paymentInfoRepository.findByOrderInfo(orderInfo);
        if (paymentInfo != null) {
            // BigDecimal to int 변환 (주의: 소수점 이하의 값이 있을 경우 데이터 손실 가능)
            dto.setTotalAmount(paymentInfo.getTotalAmount().intValue());
        } else {
            dto.setTotalAmount(0); // paymentInfo가 없을 경우 기본값 설정
        }

        return dto;
    }

    public ResponseEntity<JSONObject> confirmPayment(@RequestBody String jsonBody) throws Exception {

        JSONParser parser = new JSONParser();
        String orderId;
        String amount;
        String paymentKey;
        try {
            JSONObject requestData = (JSONObject) parser.parse(jsonBody);
            paymentKey = (String) requestData.get("paymentKey");
            orderId = (String) requestData.get("orderId");
            amount = (String) requestData.get("amount");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        JSONObject obj = new JSONObject();
        obj.put("orderId", orderId);
        obj.put("amount", amount);
        obj.put("paymentKey", paymentKey);

        String widgetSecretKey = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6";
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedBytes = encoder.encode((widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        String authorizations = "Basic " + new String(encodedBytes);

        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorizations);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(obj.toString().getBytes("UTF-8"));

        int code = connection.getResponseCode();
        boolean isSuccess = code == 200;

        InputStream responseStream = isSuccess ? connection.getInputStream() : connection.getErrorStream();

        Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8);
        JSONObject jsonObject = (JSONObject) parser.parse(reader);
        responseStream.close();

        return ResponseEntity.status(code).body(jsonObject);
    }

    @Transactional
    public ResponseEntity<?> processOrder( OrderRequest.OrderData orderData) {
        OrderInfoDTO orderInfoDTO = orderData.getOrderInfo();

        // UUID 설정
        if (orderInfoDTO.getOrderId() == null || orderInfoDTO.getOrderId().isEmpty()) {
            orderInfoDTO.setOrderId(UUID.randomUUID().toString());
        }
        OrderInfo orderInfo = saveOrderInfo(orderInfoDTO);
        saveShippingInfo(orderInfo, orderData.getShippingInfo());
        savePaymentInfo(orderInfo, orderData.getPaymentInfo());
        saveCartItem(orderInfo, orderData.getCartItems());

        return ResponseEntity.ok().body(Map.of("success", true));
    }

        @Transactional
    public OrderInfo saveOrderInfo(OrderInfoDTO orderInfoDTO) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(orderInfoDTO.getOrderId());
        orderInfo.setOrderName(orderInfoDTO.getOrderName());
        orderInfo.setEmail(orderInfoDTO.getEmail());
        orderInfo.setMobile(orderInfoDTO.getMobile());
        orderInfo.setUserid(orderInfoDTO.getUserId());
        return orderInfoRepository.save(orderInfo);
    }

    @Transactional
    public void saveCartItem(OrderInfo orderInfo, List<CartItemsDTO> cartItemDTOs) {
        List<CartItems> cartItemList = new ArrayList<>();
        logger.info("Starting to save cart items");

        for (CartItemsDTO cartItemDTO : cartItemDTOs) {

            logger.info("DTO Values - Product ID: " + cartItemDTO.getProductId() +
                    ", Product Name: " + cartItemDTO.getProductName() +
                    ", Product Price: " + cartItemDTO.getProductPrice() +
                    ", Product Count: " + cartItemDTO.getProductCount());

            CartItems cartItems = new CartItems();
            cartItems.setProductId(cartItemDTO.getProductId());
            cartItems.setProductName(cartItemDTO.getProductName());
            cartItems.setProductPrice(cartItemDTO.getProductPrice());
            cartItems.setProductImage(cartItemDTO.getProductImage());
            cartItems.setProductCount(cartItemDTO.getProductCount());
            cartItems.setOrderInfo(orderInfo);

            logger.info("Saving cart item with Product ID: " + cartItemDTO.getProductId());
            logger.info("Product Name: " + cartItemDTO.getProductName());
            logger.info("Product Price: " + cartItemDTO.getProductPrice());
            logger.info("Product Count: " + cartItemDTO.getProductCount());

            CartItems savedCartItems = cartItemsRepository.save(cartItems);
            cartItemList.add(savedCartItems);
            logger.info("Saved cart item: " + savedCartItems);
        }

        logger.info("Finished saving cart items");
    }

    @Transactional
    public PaymentInfo savePaymentInfo(OrderInfo orderInfo, PaymentInfoDTO paymentInfoDTO) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setTotalAmount(paymentInfoDTO.getTotalAmount());
        paymentInfo.setShippingFee(paymentInfoDTO.getShippingFee());
        paymentInfo.setFinalAmount(paymentInfoDTO.getFinalAmount());
        paymentInfo.setOrderInfo(orderInfo);

        return paymentInfoRepository.save(paymentInfo);
    }

    @Transactional
    public ShippingInfo saveShippingInfo(OrderInfo orderInfo, ShippingInfoDTO shippingInfoDTO) {
        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setReceiver(shippingInfoDTO.getReceiver());
        shippingInfo.setPostcode(shippingInfoDTO.getPostcode());
        shippingInfo.setAddress(shippingInfoDTO.getAddress());
        shippingInfo.setAddressDetail(shippingInfoDTO.getAddressDetail());
        shippingInfo.setMobile(shippingInfoDTO.getMobile());
        shippingInfo.setOrderInfo(orderInfo);

        return shippingInfoRepository.save(shippingInfo);
    }
    public ResponseEntity<?> getPaymentInfo(String orderId) {
        Optional<OrderInfo> orderInfoOptional = orderInfoRepository.findById(orderId);
        if (orderInfoOptional.isPresent()) {
            OrderInfo orderInfo = orderInfoOptional.get();
            PaymentInfo paymentInfo = paymentInfoRepository.findByOrderInfo(orderInfo);

            if (paymentInfo != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("userId", orderInfo.getUserid());
                response.put("finalAmount", paymentInfo.getFinalAmount());
                response.put("userEmail", orderInfo.getEmail());
                response.put("orderName", orderInfo.getOrderName());
                response.put("mobile", orderInfo.getMobile());
                response.put("count", cartItemsRepository.countByOrderInfo(orderInfo)); // 상품 갯수 추가

                return ResponseEntity.ok().body(response);
            } else {
                return ResponseEntity.status(404).body("Payment info not found");
            }
        } else {
            return ResponseEntity.status(404).body("Order not found");
        }
    }
    }
