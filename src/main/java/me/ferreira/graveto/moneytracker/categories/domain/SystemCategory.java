package me.ferreira.graveto.moneytracker.categories.domain;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public enum SystemCategory {

  SYSTEM_ROOT("00000000-0000-0000-0000-000000000000"),
  INITIAL_BALANCE("00000000-0000-0000-0000-000000000001"),
  TRANSFER_IN("00000000-0000-0000-0000-000000000002"),
  TRANSFER_OUT("00000000-0000-0000-0000-000000000003");

  private final UUID sid;

  SystemCategory(String sid) {
    this.sid = UUID.fromString(sid);
  }

  public static List<UUID> allSids() {

    return Arrays.stream(values())
        .map(SystemCategory::getSid)
        .toList();
  }

}
