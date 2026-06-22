package com.bbd.procurement.global.util;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String toJson(ObjectMapper mapper, Object value, String context) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException e) {
            log.error("Failed to serialize {}", context, e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
