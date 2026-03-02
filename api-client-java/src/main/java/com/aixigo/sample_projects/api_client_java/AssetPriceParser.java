package com.aixigo.sample_projects.api_client_java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for extracting asset prices from analytics snapshot responses.
 */
public final class AssetPriceParser {

  private AssetPriceParser() {}

  public static double findAssetPriceEurFromSnapshot(
    ObjectMapper om,
    String snapshotJson,
    String assetId
  ) throws Exception {

    JsonNode root = om.readTree(snapshotJson);
    JsonNode holdings = root.path("holdings");

    if (!holdings.isArray()) return Double.NaN;

    for (JsonNode holding : holdings) {
      JsonNode assets = holding.path("assets");
      if (!assets.isArray()) continue;

      for (JsonNode asset : assets) {
        if (assetId.equals(asset.path("id").asText())) {

          JsonNode dq = asset.path("quote")
            .path("displayQuote")
            .path("amount");
          if (dq.isNumber()) return dq.asDouble();

          JsonNode q = asset.path("quote")
            .path("quote")
            .path("amount");
          if (q.isNumber()) return q.asDouble();

          return Double.NaN;
        }
      }
    }
    return Double.NaN;
  }

  public static double findUsdEurPer1UsdFromAssetApi(
    ObjectMapper om,
    String usdAssetJson
  ) throws Exception {

    JsonNode root = om.readTree(usdAssetJson);
    JsonNode arr = root.has("assets") ? root.get("assets") : root;

    if (!arr.isArray() || arr.isEmpty()) return Double.NaN;

    JsonNode usd = arr.get(0);

    JsonNode dq = usd.path("quote").path("displayQuote").path("amount");
    if (dq.isNumber()) return dq.asDouble();

    JsonNode q = usd.path("quote").path("quote").path("amount");
    if (q.isNumber()) return q.asDouble();

    return Double.NaN;
  }
}
