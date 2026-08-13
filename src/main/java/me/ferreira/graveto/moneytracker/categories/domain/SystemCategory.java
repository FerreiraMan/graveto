package me.ferreira.graveto.moneytracker.categories.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum SystemCategory {

  SYSTEM_ROOT("00000000-0000-0000-0000-000000000000", true, false),
  INITIAL_BALANCE("00000000-0000-0000-0000-000000000001", true, false),
  TRANSFER_IN("00000000-0000-0000-0000-000000000002", true, false),
  TRANSFER_OUT("00000000-0000-0000-0000-000000000003", true, false),
  OTHER("00000000-0000-0000-0000-000000000004", false, true),
  OTHER_INCOME("00000000-0000-0000-0000-000000000005", false, true);

  private static final Set<String> FALLBACK_CATEGORY_SET = Arrays.stream(values())
      .filter(SystemCategory::isFallback)
      .map(Enum::name)
      .collect(Collectors.toSet());

  private final UUID sid;
  private final boolean isInternal;
  private final boolean isFallback;

  SystemCategory(final String sid, final boolean isInternal, final boolean isFallback) {
    this.sid = UUID.fromString(sid);
    this.isInternal = isInternal;
    this.isFallback = isFallback;
  }

  public static List<UUID> allInternalSids() {

    return Arrays.stream(values())
        .filter(SystemCategory::isInternal)
        .map(SystemCategory::getSid)
        .toList();
  }

  public static boolean isFallback(final String name) {
    return FALLBACK_CATEGORY_SET.contains(name);
  }

}
