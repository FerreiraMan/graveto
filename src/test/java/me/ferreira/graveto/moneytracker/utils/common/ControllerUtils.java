package me.ferreira.graveto.moneytracker.utils.common;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ControllerUtils {

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
