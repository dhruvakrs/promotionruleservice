package io.promoengine.helper;

import io.promoengine.dto.request.CalculateRequest;
import io.promoengine.dto.request.OrderItem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BeanValidationHelper {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> validate(Map<String, Object> map) {
        CalculateRequest req = toCalculateRequest(map);
        Set<ConstraintViolation<CalculateRequest>> violations = VALIDATOR.validate(req);
        return violations.stream()
                .map(v -> Map.of(
                        "propertyPath", v.getPropertyPath().toString(),
                        "message", v.getMessage()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static CalculateRequest toCalculateRequest(Map<String, Object> map) {
        CalculateRequest req = new CalculateRequest();
        req.setStoreId((String) map.get("storeId"));
        req.setCustomerId(map.get("customerId") != null ? map.get("customerId").toString() : null);

        Object date = map.get("transactionDate");
        if (date instanceof String) {
            req.setTransactionDate(LocalDate.parse((String) date));
        } else if (date instanceof LocalDate) {
            req.setTransactionDate((LocalDate) date);
        }

        Object itemsObj = map.get("items");
        if (itemsObj instanceof List<?>) {
            List<OrderItem> items = new ArrayList<>();
            for (Object itemObj : (List<?>) itemsObj) {
                if (itemObj instanceof Map<?, ?>) {
                    Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                    OrderItem item = new OrderItem();
                    item.setSku(itemMap.get("sku") != null ? itemMap.get("sku").toString() : null);
                    item.setQuantity(itemMap.get("quantity") != null ? ((Number) itemMap.get("quantity")).intValue() : 0);
                    items.add(item);
                }
            }
            req.setItems(items.isEmpty() ? null : items);
        }

        return req;
    }
}
