package com.mamikos.kostapi.common.response;

import com.mamikos.kostapi.common.filter.RequestIdFilter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

/** Builds the {@code meta} block every response carries. */
public final class MetaFactory {

    private MetaFactory() {}

    public static Map<String, Object> base() {
        Map<String, Object> meta = new LinkedHashMap<>();
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID_KEY);
        if (requestId != null) {
            meta.put("requestId", requestId);
        }
        meta.put("timestamp", Instant.now().toString());
        return meta;
    }
}
