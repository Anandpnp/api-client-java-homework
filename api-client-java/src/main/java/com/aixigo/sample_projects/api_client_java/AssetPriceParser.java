package com.aixigo.sample_projects.api_client_java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AssetPriceParser {

  private AssetPriceParser() {}

  /**
   * Extracts asset price in EUR from snapshot JSON.
   * Expected formats supported:
   * 1) quote.displayQuote.amount
   * 2) quote.quote.amount
   */
  public static double findAssetPriceEurFromSnapshot(ObjectMapper om, String snapshotJson, String assetId) throws Exception {
    JsonNode root = om.readTree(snapshotJson);

    // Adjust this depending on your real JSON:
    // some snapshots might have root["assets"], or root itself is the array.
    JsonNode assetsNode = root.has("assets") ? root.get("assets") : root;

    if (assetsNode == null || !assetsNode.isArray()) {
      return Double.NaN;
    }

    for (JsonNode asset : assetsNode) {
      String id = asset.path("id").asText(null);
      if (assetId.equals(id)) {
        JsonNode dq = asset.path("quote").path("displayQuote").path("amount");
        if (dq.isNumber()) return dq.asDouble();

        JsonNode q = asset.path("quote").path("quote").path("amount");
        if (q.isNumber()) return q.asDouble();

        return Double.NaN;
      }
    }

    return Double.NaN;
  }

  public static double findUsdEurPer1UsdFromAssetApi(ObjectMapper om, String usdAssetJson) throws Exception {
    JsonNode root = om.readTree(usdAssetJson);
    JsonNode arr = root.has("assets") ? root.get("assets") : root;
    if (!arr.isArray() || arr.size() == 0) return Double.NaN;

    JsonNode usd = arr.get(0);

    JsonNode dq = usd.path("quote").path("displayQuote").path("amount");
    if (dq.isNumber()) return dq.asDouble();

    JsonNode q = usd.path("quote").path("quote").path("amount");
    if (q.isNumber()) return q.asDouble();

    return Double.NaN;
  }
}
