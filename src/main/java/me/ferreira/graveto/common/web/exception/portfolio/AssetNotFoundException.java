package me.ferreira.graveto.common.web.exception.portfolio;

import java.util.UUID;

public class AssetNotFoundException extends RuntimeException {
  public AssetNotFoundException(final UUID assetSid) {
    super("Asset with SID [" + assetSid + "] was not found or you do not have permission to view it.");
  }
}
