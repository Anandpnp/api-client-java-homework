package com.aixigo.sample_projects.api_client_java;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssetPriceParserTest {

  private final ObjectMapper om = new ObjectMapper();

  @Test
  void returnsDisplayQuoteAmountWhenPresent() throws Exception {
    String json = """
            {
              "holdings": [
                {
                  "assets": [
                    {
                      "id": "A1",
                      "quote": {
                        "displayQuote": { "amount": 123.45 }
                      }
                    }
                  ]
                }
              ]
            }
            """;

    double price = AssetPriceParser.findAssetPriceEurFromSnapshot(om, json, "A1");
    assertEquals(123.45, price, 0.000001);
  }

  @Test
  void returnsQuoteAmountWhenDisplayQuoteMissing() throws Exception {
    String json = """
            {
              "holdings": [
                {
                  "assets": [
                    {
                      "id": "A1",
                      "quote": {
                        "quote": { "amount": 99.0 }
                      }
                    }
                  ]
                }
              ]
            }
            """;

    double price = AssetPriceParser.findAssetPriceEurFromSnapshot(om, json, "A1");
    assertEquals(99.0, price, 0.000001);
  }

  @Test
  void returnsNaNWhenAssetIdNotFound() throws Exception {
    String json = """
            {
              "holdings": [
                {
                  "assets": [
                    { "id": "A1", "quote": { "displayQuote": { "amount": 10 } } }
                  ]
                }
              ]
            }
            """;

    double price = AssetPriceParser.findAssetPriceEurFromSnapshot(om, json, "DOES_NOT_EXIST");
    assertTrue(Double.isNaN(price));
  }

  // ---------- USD/EUR tests: NO CHANGE NEEDED ----------

  @Test
  void usdEur_returnsDisplayQuoteAmountWhenPresent() throws Exception {
    String json = """
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

    double rate = AssetPriceParser.findUsdEurPer1UsdFromAssetApi(om, json);
    assertEquals(0.92, rate, 1e-9);
  }

  @Test
  void usdEur_fallsBackToQuoteAmountWhenDisplayQuoteMissing() throws Exception {
    String json = """
      {
        "assets": [
          {
            "quote": {
              "quote": { "amount": 0.91 }
            }
          }
        ]
      }
      """;

    double rate = AssetPriceParser.findUsdEurPer1UsdFromAssetApi(om, json);
    assertEquals(0.91, rate, 1e-9);
  }

  @Test
  void usdEur_returnsNaNWhenAssetsMissingOrEmpty() throws Exception {
    String json1 = "{ \"assets\": [] }";
    assertTrue(Double.isNaN(AssetPriceParser.findUsdEurPer1UsdFromAssetApi(om, json1)));

    String json2 = "{ \"assets\": [ { \"quote\": {} } ] }";
    assertTrue(Double.isNaN(AssetPriceParser.findUsdEurPer1UsdFromAssetApi(om, json2)));
  }
}
