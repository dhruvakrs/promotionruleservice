function fn() {
    var Optional = Java.type('java.util.Optional');
    var CustomerData = Java.type('io.promoengine.enrichment.CustomerData');
    var HashMap = Java.type('java.util.HashMap');

    var customers = new HashMap();

    var repo = {
        withCustomer: function(customerId, groupCode, typeCode) {
            var data = CustomerData.builder().customerId(customerId).customerGroupCode(groupCode).customerTypeCode(typeCode).build();
            customers.put(customerId, data);
            return repo;
        },
        findCustomer: function(customerId) {
            var customer = customers.get(customerId);
            if (customer == null) {
                return Optional.empty();
            }
            return Optional.of(customer);
        }
    };
    return repo;
}
