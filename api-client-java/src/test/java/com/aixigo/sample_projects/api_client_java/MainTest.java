package com.aixigo.sample_projects.api_client_java;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

  private final ObjectMapper om = new ObjectMapper();

  @Test
  void snapshotPriceParsing_returnsNumber_whenAssetExists() throws Exception {
    // This matches the REAL snapshot shape: holdings -> assets
    String snapshotJson = """
      {
        "holdings": [
          {
            "assets": [
              {
                "id": "514000",
                "quote": {
                  "displayQuote": { "amount": 12.34 }
                }
              }
            ]
          }
        ]
      }
      """;

    double p = AssetPriceParser.findAssetPriceEurFromSnapshot(om, snapshotJson, "514000");
    assertEquals(12.34, p, 1e-9);
  }

  @Test
  void snapshotPriceParsing_returnsNaN_whenAssetMissing() throws Exception {
    String snapshotJson = """
      {
        "holdings": [
          {
            "assets": [
              {
                "id": "A1",
                "quote": {
                  "displayQuote": { "amount": 1.0 }
                }
              }
            ]
          }
        ]
      }
      """;

    double p = AssetPriceParser.findAssetPriceEurFromSnapshot(om, snapshotJson, "DOES_NOT_EXIST");
    assertTrue(Double.isNaN(p));
  }

  @Test
  void usdRateParsing_returnsDisplayQuoteAmount() throws Exception {
    // This matches /asset/assets shape: assets -> quote
    String assetApiJson = """
      {
        "assets": [
          {
            "quote": {
              "displayQuote": { "amount": 0.92 }
            }
          }
        ]
      }
      """;

    double eurPerUsd = AssetPriceParser.findUsdEurPer1UsdFromAssetApi(om, assetApiJson);
    assertEquals(0.92, eurPerUsd, 1e-9);
  }
}
