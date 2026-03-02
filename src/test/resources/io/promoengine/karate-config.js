function fn() {
    var env = karate.env || 'local';
    var config = {
        baseUrl:  'http://localhost:8080/promotionengine/api',
        apiKey:   'test-key',
        tenantId: 'default'
    };
    if (env === 'docker') {
        config.baseUrl = 'http://localhost:8080/promotionengine/api';
    }
    if (env === 'staging') {
        config.baseUrl = karate.properties['staging.url'];
        config.apiKey  = karate.properties['staging.key'];
    }
    karate.configure('connectTimeout', 10000);
    karate.configure('readTimeout', 30000);
    return config;
}
