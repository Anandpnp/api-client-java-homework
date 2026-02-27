package com.aixigo.sample_projects.api_client_java;

import com.aixigo.sample_projects.api_client_java.profiling.ApiResponseDecoder;
import com.aixigo.sample_projects.api_client_java.profiling.api.RiskProfileApi;
import com.aixigo.sample_projects.api_client_java.profiling.model.Answer;
import com.aixigo.sample_projects.api_client_java.profiling.model.RiskProfileAnswers;
import com.aixigo.sample_projects.api_client_java.profiling.model.RiskProfileQuestionnaire;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestInterceptor;
import feign.RequestLine;
import feign.RequestTemplate;
import feign.Response;
import feign.Util;
import feign.codec.StringDecoder;
import feign.jackson.JacksonEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Main {

  // ========= API101 (Client Profiling) =========
  private static final String DEFAULT_BASE_URL = "https://demo.portal.aixigo.cloud/client-profiling";
  private static final String ACCEPT_LANGUAGE = "en";
  private static final String TAG_NATURAL_PERSON = "natural_person";
  private static final String JUSTIFICATION_TEXT = "API101";
  private static final String QUESTION_ID =
    "risk_capability_questions_for_natural_person_number_of_persons_in_household";
  private static final String ANSWER_ID = "NUMBER_OF_PERSONS_ONE";

  // ========= API102 (Analytics) =========
  private static final String ANALYTICS_BASE_URL = "https://demo.portal.aixigo.cloud/analytics";
  private static final String WHEN_START = "2024-01-01";
  private static final String WHEN_END   = "2025-01-01";

  // ISINs / currency assets (inputs)
  private static final String ISIN_DB     = "DE0005140008";
  private static final String ISIN_CITI   = "US1729674242";
  private static final String ISIN_AMUNDI = "FR0004125920";
  private static final String ASSET_EUR   = "CURRENCY:EUR.EUR";
  private static final String ASSET_USD   = "CURRENCY:USD.USD";

  // Contract #1 quantities (homework definition)
  private static final double QTY_DB     = 50.0;
  private static final double QTY_CITI   = 20.0;
  private static final double QTY_AMUNDI = 30.0;
  private static final double QTY_EUR    = 5000.0;

  // ===== Raw APIs =====
  interface RawAssetApi {
    @RequestLine("GET /asset/assets?asset={asset}&when={when}")
    @Headers("Accept: application/json")
    String getAssets(@Param("asset") String asset, @Param("when") String when);
  }

  interface RawPortfolioApi {
    @RequestLine("POST /portfolio/temporary-contract")
    @Headers({
      "Accept: application/json",
      "Content-Type: application/json"
    })
    Response createTemporaryContract(Object body);

    // Return Response so we can print body on 400
    @RequestLine("GET /portfolio/assets-snapshot?when={when}&aggregation={aggregation}&contract={contract}&currency={currency}")
    @Headers("Accept: application/json")
    Response getAssetsSnapshot(
      @Param("when") String when,
      @Param("aggregation") String aggregation,
      @Param("contract") String contract,
      @Param("currency") String currency
    );
  }

  // API101 helper: raw GET for Last-Modified
  interface RiskProfileRawGetApi {
    @RequestLine("GET /risk-profiles/{id}?detailed=false")
    @Headers({
      "Accept: application/json",
      "Accept-Language: {acceptLanguage}"
    })
    Response getRiskProfileRaw(@Param("id") String id, @Param("acceptLanguage") String acceptLanguage);
  }

  public static void main(String[] args) throws Exception {
    final String profilingBaseUrl = (args.length > 0) ? args[0] : DEFAULT_BASE_URL;
    final String analyticsBaseUrl = (args.length > 1) ? args[1] : ANALYTICS_BASE_URL;

    final ObjectMapper objectMapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerModule(new JavaTimeModule());

    final RequestInterceptor auth = buildAuthInterceptor();

    // ========= API102 clients =========
    final RawAssetApi rawAssetApi = Feign.builder()
      .decoder(new StringDecoder())
      .requestInterceptor(auth)
      .target(RawAssetApi.class, analyticsBaseUrl);

    final RawPortfolioApi rawPortfolioApi = Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decoder(new StringDecoder())
      .requestInterceptor(auth)
      .target(RawPortfolioApi.class, analyticsBaseUrl);

    // ========= API102 Step 1: fetch asset IDs =========
    final String dbId     = extractFirstAssetId(objectMapper, rawAssetApi.getAssets(ISIN_DB, WHEN_START));
    final String citiId   = extractFirstAssetId(objectMapper, rawAssetApi.getAssets(ISIN_CITI, WHEN_START));
    final String amundiId = extractFirstAssetId(objectMapper, rawAssetApi.getAssets(ISIN_AMUNDI, WHEN_START));
    final String eurId    = extractFirstAssetId(objectMapper, rawAssetApi.getAssets(ASSET_EUR, WHEN_START));
    final String usdId    = extractFirstAssetId(objectMapper, rawAssetApi.getAssets(ASSET_USD, WHEN_START));

    System.out.println("dbId=" + dbId);
    System.out.println("citiId=" + citiId);
    System.out.println("amundiId=" + amundiId);
    System.out.println("eurId=" + eurId);
    System.out.println("usdId=" + usdId);

    // ========= API102 Step 2: create temporary contract #1 =========
    // FIX: contractId IS REQUIRED in your environment
    final String contractId1Requested = "contract_" + java.util.UUID.randomUUID();

    final ObjectNode contract1 = objectMapper.createObjectNode();
    contract1.put("contractId", contractId1Requested);
    contract1.put("when", WHEN_START);
    contract1.put("useCorporateActions", true);

    final ArrayNode investments1 = contract1.putArray("investments");
    investments1.add(investment(objectMapper, "POS_DB",       dbId,     QTY_DB,     "EUR"));
    investments1.add(investment(objectMapper, "POS_CITI",     citiId,   QTY_CITI,   "USD"));
    investments1.add(investment(objectMapper, "POS_AMUNDI",   amundiId, QTY_AMUNDI, "EUR"));
    investments1.add(investment(objectMapper, "POS_EUR_CASH", eurId,    QTY_EUR,    "EUR"));

    System.out.println("TEMP_CONTRACT_1_REQUEST=");
    System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contract1));

    final Response resp1 = rawPortfolioApi.createTemporaryContract(contract1);
    final String respBody1 = readBody(resp1);

    System.out.println("TEMP_CONTRACT_1_STATUS=" + resp1.status());
    System.out.println("TEMP_CONTRACT_1_HEADERS=" + resp1.headers());
    System.out.println("TEMP_CONTRACT_1_RESPONSE=");
    System.out.println(respBody1);

    if (resp1.status() < 200 || resp1.status() >= 300) {
      throw new IllegalStateException("TEMP_CONTRACT_1 failed. See response above.");
    }

    // Some environments echo the same id back; if not, fall back to requested
    String contractId1 = objectMapper.readTree(respBody1).path("contractId").asText(null);
    if (contractId1 == null || contractId1.isBlank()) contractId1 = contractId1Requested;

    System.out.println("contractId_1=" + contractId1);

    // ========= API102 Step 3: assets-snapshot (ASSET) for start/end =========
    final String snap1StartAsset = getSnapshotOrThrow(rawPortfolioApi, objectMapper, WHEN_START, "ASSET", contractId1, "EUR", "CONTRACT1_START");
    final String snap1EndAsset   = getSnapshotOrThrow(rawPortfolioApi, objectMapper, WHEN_END,   "ASSET", contractId1, "EUR", "CONTRACT1_END");

    // ---- Compute contract #1 value at start/end in EUR from snapshot prices ----
    final double pDbStart     = findAssetPriceEurFromSnapshot(objectMapper, snap1StartAsset, dbId);
    final double pCitiStart   = findAssetPriceEurFromSnapshot(objectMapper, snap1StartAsset, citiId);
    final double pAmundiStart = findAssetPriceEurFromSnapshot(objectMapper, snap1StartAsset, amundiId);
    final double pEurStart    = findAssetPriceEurFromSnapshot(objectMapper, snap1StartAsset, eurId);

    ensureNotNaN(pDbStart, "DB price @ start");
    ensureNotNaN(pCitiStart, "CITI price @ start");
    ensureNotNaN(pAmundiStart, "AMUNDI price @ start");
    ensureNotNaN(pEurStart, "EUR price @ start");

    final double v1Start = QTY_DB * pDbStart + QTY_CITI * pCitiStart + QTY_AMUNDI * pAmundiStart + QTY_EUR * pEurStart;

    final double pDbEnd     = findAssetPriceEurFromSnapshot(objectMapper, snap1EndAsset, dbId);
    final double pCitiEnd   = findAssetPriceEurFromSnapshot(objectMapper, snap1EndAsset, citiId);
    final double pAmundiEnd = findAssetPriceEurFromSnapshot(objectMapper, snap1EndAsset, amundiId);
    final double pEurEnd    = findAssetPriceEurFromSnapshot(objectMapper, snap1EndAsset, eurId);

    ensureNotNaN(pDbEnd, "DB price @ end");
    ensureNotNaN(pCitiEnd, "CITI price @ end");
    ensureNotNaN(pAmundiEnd, "AMUNDI price @ end");
    ensureNotNaN(pEurEnd, "EUR price @ end");

    final double v1End = QTY_DB * pDbEnd + QTY_CITI * pCitiEnd + QTY_AMUNDI * pAmundiEnd + QTY_EUR * pEurEnd;

    final double r1 = (v1End / v1Start) - 1.0;

    System.out.println("CONTRACT1_VALUE_EUR_2024_01_01=" + v1Start);
    System.out.println("CONTRACT1_VALUE_EUR_2025_01_01=" + v1End);
    System.out.printf("CONTRACT1_TOTAL_RETURN_2024=%.4f%%%n", r1 * 100.0);

    // ========= API102 Step 4: Contract #2 (EU-only value invested into USD) =========
    // EU value definition per homework: Deutsche Bank + Amundi + EUR cash (exclude Citi)
    final double euValueEurStart = QTY_DB * pDbStart + QTY_AMUNDI * pAmundiStart + QTY_EUR * pEurStart;
    System.out.println("EU_ONLY_VALUE_EUR_2024_01_01=" + euValueEurStart);

    // Convert EUR -> USD using "EUR per 1 USD" from /asset/assets for CURRENCY:USD.USD at start date
    final String usdAssetJsonStart = rawAssetApi.getAssets(ASSET_USD, WHEN_START);
    final double eurPerUsdStart = findUsdEurPer1UsdFromAssetApi(objectMapper, usdAssetJsonStart);
    if (Double.isNaN(eurPerUsdStart) || eurPerUsdStart <= 0) {
      throw new IllegalStateException(
        "Cannot read EUR-per-USD from /asset/assets for " + ASSET_USD + " at " + WHEN_START + ". Response: " + usdAssetJsonStart
      );
    }

    final double usdQtyForContract2 = euValueEurStart / eurPerUsdStart;

    System.out.println("EUR_PER_1_USD_2024_01_01=" + eurPerUsdStart);
    System.out.println("CONTRACT2_USD_QTY=" + usdQtyForContract2);

    // Build & create contract #2 (USD cash only)
    final String contractId2Requested = "contract_" + java.util.UUID.randomUUID();

    final ObjectNode contract2 = objectMapper.createObjectNode();
    contract2.put("contractId", contractId2Requested);
    contract2.put("when", WHEN_START);
    contract2.put("useCorporateActions", true);

    final ArrayNode investments2 = contract2.putArray("investments");
    investments2.add(investment(objectMapper, "POS_USD_CASH", usdId, usdQtyForContract2, "USD"));

    System.out.println("TEMP_CONTRACT_2_REQUEST=");
    System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contract2));

    final Response resp2 = rawPortfolioApi.createTemporaryContract(contract2);
    final String respBody2 = readBody(resp2);

    System.out.println("TEMP_CONTRACT_2_STATUS=" + resp2.status());
    System.out.println("TEMP_CONTRACT_2_HEADERS=" + resp2.headers());
    System.out.println("TEMP_CONTRACT_2_RESPONSE=");
    System.out.println(respBody2);

    if (resp2.status() < 200 || resp2.status() >= 300) {
      throw new IllegalStateException("TEMP_CONTRACT_2 failed. See response above.");
    }

    String contractId2 = objectMapper.readTree(respBody2).path("contractId").asText(null);
    if (contractId2 == null || contractId2.isBlank()) contractId2 = contractId2Requested;

    System.out.println("contractId_2=" + contractId2);

    // Snapshot for contract #2 at start/end (ASSET so we can price USD currency in EUR)
    final String snap2StartAsset = getSnapshotOrThrow(rawPortfolioApi, objectMapper, WHEN_START, "ASSET", contractId2, "EUR", "CONTRACT2_START");
    final String snap2EndAsset   = getSnapshotOrThrow(rawPortfolioApi, objectMapper, WHEN_END,   "ASSET", contractId2, "EUR", "CONTRACT2_END");

    // Contract #2 value in EUR: USD quantity * (EUR per 1 USD) at the date
    final double pUsdStartEur = findAssetPriceEurFromSnapshot(objectMapper, snap2StartAsset, usdId);
    final double pUsdEndEur   = findAssetPriceEurFromSnapshot(objectMapper, snap2EndAsset, usdId);

    ensureNotNaN(pUsdStartEur, "USD price(EUR per 1 USD) @ start from snapshot");
    ensureNotNaN(pUsdEndEur, "USD price(EUR per 1 USD) @ end from snapshot");

    final double v2Start = usdQtyForContract2 * pUsdStartEur;
    final double v2End   = usdQtyForContract2 * pUsdEndEur;
    final double r2 = (v2End / v2Start) - 1.0;

    System.out.println("CONTRACT2_VALUE_EUR_2024_01_01=" + v2Start);
    System.out.println("CONTRACT2_VALUE_EUR_2025_01_01=" + v2End);
    System.out.printf("CONTRACT2_TOTAL_RETURN_2024=%.4f%%%n", r2 * 100.0);

    // ========= API101 (risk profile) - unchanged =========
    final RiskProfileApi riskApi = Feign.builder()
      .decoder(new ApiResponseDecoder(objectMapper))
      .encoder(new JacksonEncoder(objectMapper))
      .requestInterceptor(auth)
      .target(RiskProfileApi.class, profilingBaseUrl);

    final RiskProfileRawGetApi rawGet = Feign.builder()
      .requestInterceptor(auth)
      .target(RiskProfileRawGetApi.class, profilingBaseUrl);

    List<String> ownerIds = Collections.singletonList(getOwnerIdFallback());
    List<String> tags = Collections.singletonList(TAG_NATURAL_PERSON);

    RiskProfileQuestionnaire created = riskApi.createRiskProfile(
      ownerIds, false, ACCEPT_LANGUAGE, false, tags
    );

    String riskProfileId = created.getId();
    System.out.println("CREATED_RISK_PROFILE_ID=" + riskProfileId);

    String lastModified = fetchLastModified(rawGet, riskProfileId, ACCEPT_LANGUAGE);
    System.out.println("LAST_MODIFIED_1=" + lastModified);

    RiskProfileAnswers answers = new RiskProfileAnswers();
    Answer a = new Answer();
    a.setQuestionId(QUESTION_ID);
    a.setAnswerId(ANSWER_ID);
    answers.addAnswersItem(a);

    riskApi.fillRiskProfile(riskProfileId, lastModified, answers, false, ACCEPT_LANGUAGE, null);
    System.out.println("FILL_1_OK");

    lastModified = fetchLastModified(rawGet, riskProfileId, ACCEPT_LANGUAGE);
    System.out.println("LAST_MODIFIED_2=" + lastModified);

    a.setJustification(JUSTIFICATION_TEXT);
    riskApi.fillRiskProfile(riskProfileId, lastModified, answers, false, ACCEPT_LANGUAGE, null);
    System.out.println("FILL_2_OK_WITH_JUSTIFICATION");
    System.out.println("FINAL_RISK_PROFILE_ID=" + riskProfileId);
  }

  // ===== Helpers =====

  private static String readBody(Response resp) throws Exception {
    if (resp == null || resp.body() == null) return "";
    return Util.toString(resp.body().asReader(StandardCharsets.UTF_8));
  }

  private static String getSnapshotOrThrow(
    RawPortfolioApi rawPortfolioApi,
    ObjectMapper objectMapper,
    String when,
    String aggregation,
    String contractId,
    String currency,
    String label
  ) throws Exception {
    Response r = rawPortfolioApi.getAssetsSnapshot(when, aggregation, contractId, currency);
    String body = readBody(r);

    System.out.println("ASSETS_SNAPSHOT_" + label + "_STATUS=" + r.status());
    System.out.println("ASSETS_SNAPSHOT_" + label + "_RESPONSE=");
    System.out.println(body);

    if (r.status() < 200 || r.status() >= 300) {
      throw new IllegalStateException("assets-snapshot failed (" + label + "). Body: " + body);
    }

    objectMapper.readTree(body);
    return body;
  }

  private static ObjectNode investment(ObjectMapper om, String positionId, String assetId, double qty, String currency) {
    ObjectNode n = om.createObjectNode();
    n.put("assetId", assetId);
    n.put("quantity", qty);
    n.put("currency", currency);

    // Required in your demo environment
//    n.put("positionId", positionId);
//    n.put("accountId", "tempAccount001");

    return n;
  }

  private static RequestInterceptor buildAuthInterceptor() {
    return new RequestInterceptor() {
      @Override
      public void apply(RequestTemplate template) {
        String token = System.getenv("X_ID_TOKEN");
        if (token == null || token.trim().isEmpty()) {
          throw new IllegalStateException(
            "Missing env var X_ID_TOKEN. Set it in IntelliJ Run Config (Environment variables)."
          );
        }
        template.header("X-ID-Token", token.trim());
      }
    };
  }

  private static String getOwnerIdFallback() {
    return "anand.naray@hcltech.com";
  }

  private static String fetchLastModified(RiskProfileRawGetApi rawGet, String riskProfileId, String acceptLanguage) {
    Response r = rawGet.getRiskProfileRaw(riskProfileId, acceptLanguage);
    for (Map.Entry<String, Collection<String>> h : r.headers().entrySet()) {
      if ("last-modified".equalsIgnoreCase(h.getKey())
        && h.getValue() != null
        && !h.getValue().isEmpty()) {
        return h.getValue().iterator().next();
      }
    }
    throw new IllegalStateException("No Last-Modified header found in GET /risk-profiles/{id} response.");
  }

  private static String extractFirstAssetId(ObjectMapper objectMapper, String json) throws Exception {
    JsonNode root = objectMapper.readTree(json);
    JsonNode arr = root.has("assets") ? root.get("assets") : root;
    if (!arr.isArray() || arr.size() == 0) {
      throw new IllegalStateException("No assets found in response: " + json);
    }
    return arr.get(0).path("id").asText();
  }

  private static double findAssetPriceEurFromSnapshot(ObjectMapper om, String snapshotJson, String assetId) throws Exception {
    JsonNode root = om.readTree(snapshotJson);
    JsonNode holdings = root.path("holdings");
    if (!holdings.isArray()) return Double.NaN;

    for (JsonNode holding : holdings) {
      JsonNode assets = holding.path("assets");
      if (!assets.isArray()) continue;

      for (JsonNode a : assets) {
        if (assetId.equals(a.path("id").asText())) {
          JsonNode dq = a.path("quote").path("displayQuote").path("amount");
          if (dq.isNumber()) return dq.asDouble();

          JsonNode q = a.path("quote").path("quote").path("amount");
          if (q.isNumber()) return q.asDouble();
        }
      }
    }
    return Double.NaN;
  }

  private static double findUsdEurPer1UsdFromAssetApi(ObjectMapper om, String usdAssetJson) throws Exception {
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

  private static void ensureNotNaN(double v, String label) {
    if (Double.isNaN(v)) {
      throw new IllegalStateException("Could not read " + label + " (NaN). Check snapshot JSON / asset IDs.");
    }
  }
}
