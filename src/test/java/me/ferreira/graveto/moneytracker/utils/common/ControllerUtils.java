package me.ferreira.graveto.moneytracker.utils.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.util.List;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

public class ControllerUtils {

  public static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <E> E convertIntoObject(final MvcTestResult payload, final Class<E> targetClass) throws Exception {

    final ObjectMapper objectMapper = new ObjectMapper();
    final String json = payload.getResponse().getContentAsString();
    return objectMapper.readValue(json, targetClass);
  }

  public static <E> List<E> convertIntoObjectList(final MvcTestResult payload, final Class<E> targetClass)
      throws Exception {

    final ObjectMapper objectMapper = new ObjectMapper();
    final String json = payload.getResponse().getContentAsString();
    final CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, targetClass);
    return objectMapper.readValue(json, listType);
  }

}
