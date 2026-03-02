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
import java.util.UUID;

import static com.aixigo.sample_projects.api_client_java.AssetPriceParser.findAssetPriceEurFromSnapshot;
import static com.aixigo.sample_projects.api_client_java.AssetPriceParser.findUsdEurPer1UsdFromAssetApi;

import static com.aixigo.sample_projects.api_client_java.AssetPriceParser.findAssetPriceEurFromSnapshot;
import static com.aixigo.sample_projects.api_client_java.AssetPriceParser.findUsdEurPer1UsdFromAssetApi;

public class Main {

  // ========= API101 (Client Profiling) =========
  private static final String PROFILING_BASE_URL = "https://demo.portal.aixigo.cloud/client-profiling";
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

    // Return Response so we can debug on non-2xx
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

  // ===== Result holder for API102 =====
  private static final class Api102Results {
    final String contract1Id;
    final String contract2Id;
    final double contract1ReturnPct;
    final double contract2ReturnPct;

    Api102Results(String c1Id, String c2Id, double c1Pct, double c2Pct) {
      this.contract1Id = c1Id;
      this.contract2Id = c2Id;
      this.contract1ReturnPct = c1Pct;
      this.contract2ReturnPct = c2Pct;
    }
  }

  public static void main(String[] args) throws Exception {

    final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerModule(new JavaTimeModule());

    final RequestInterceptor auth = buildAuthInterceptor();

    // Run API102 (contracts & returns)
    Api102Results api102 = executeApi102(mapper, auth, ANALYTICS_BASE_URL);

    // Run API101 (risk profile)
    String riskProfileId = executeApi101(mapper, auth, PROFILING_BASE_URL);

    // Print EXACT requested format (plus contract IDs)
    printFinalOutput(api102, riskProfileId);
  }

  // ============================================================
  // =========================== API102 =========================
  // ============================================================
  private static Api102Results executeApi102(ObjectMapper mapper,
                                             RequestInterceptor auth,
                                             String analyticsBaseUrl) throws Exception {

    final RawAssetApi rawAssetApi = Feign.builder()
      .decoder(new StringDecoder())
      .requestInterceptor(auth)
      .target(RawAssetApi.class, analyticsBaseUrl);

    final RawPortfolioApi rawPortfolioApi = Feign.builder()
      .encoder(new JacksonEncoder(mapper))
      .decoder(new StringDecoder())
      .requestInterceptor(auth)
      .target(RawPortfolioApi.class, analyticsBaseUrl);

    // Step 1: fetch asset IDs
    final String dbId     = extractFirstAssetId(mapper, rawAssetApi.getAssets(ISIN_DB, WHEN_START));
    final String citiId   = extractFirstAssetId(mapper, rawAssetApi.getAssets(ISIN_CITI, WHEN_START));
    final String amundiId = extractFirstAssetId(mapper, rawAssetApi.getAssets(ISIN_AMUNDI, WHEN_START));
    final String eurId    = extractFirstAssetId(mapper, rawAssetApi.getAssets(ASSET_EUR, WHEN_START));
    final String usdId    = extractFirstAssetId(mapper, rawAssetApi.getAssets(ASSET_USD, WHEN_START));

    // Step 2: create temporary contract #1
    final String contractId1 = createContract1(mapper, rawPortfolioApi, dbId, citiId, amundiId, eurId);

    // Step 3: assets-snapshot for start/end (ASSET, priced in EUR)
    final String snap1Start = getSnapshotOrThrow(rawPortfolioApi, mapper, WHEN_START, "ASSET", contractId1, "EUR", "CONTRACT1_START");
    final String snap1End   = getSnapshotOrThrow(rawPortfolioApi, mapper, WHEN_END,   "ASSET", contractId1, "EUR", "CONTRACT1_END");

    // Compute contract #1 value at start/end
    final double pDbStart     = priceOrThrow(mapper, snap1Start, dbId, "DB price @ start");
    final double pCitiStart   = priceOrThrow(mapper, snap1Start, citiId, "CITI price @ start");
    final double pAmundiStart = priceOrThrow(mapper, snap1Start, amundiId, "AMUNDI price @ start");
    final double pEurStart    = priceOrThrow(mapper, snap1Start, eurId, "EUR price @ start");

    final double v1Start =
      QTY_DB * pDbStart
        + QTY_CITI * pCitiStart
        + QTY_AMUNDI * pAmundiStart
        + QTY_EUR * pEurStart;

    final double pDbEnd     = priceOrThrow(mapper, snap1End, dbId, "DB price @ end");
    final double pCitiEnd   = priceOrThrow(mapper, snap1End, citiId, "CITI price @ end");
    final double pAmundiEnd = priceOrThrow(mapper, snap1End, amundiId, "AMUNDI price @ end");
    final double pEurEnd    = priceOrThrow(mapper, snap1End, eurId, "EUR price @ end");

    final double v1End =
      QTY_DB * pDbEnd
        + QTY_CITI * pCitiEnd
        + QTY_AMUNDI * pAmundiEnd
        + QTY_EUR * pEurEnd;

    final double r1 = (v1End / v1Start) - 1.0;

    // Step 4: EU-only value at start date (Deutsche Bank + Amundi + EUR cash)
    final double euValueEurStart =
      QTY_DB * pDbStart
        + QTY_AMUNDI * pAmundiStart
        + QTY_EUR * pEurStart;

    // Convert EUR -> USD using "EUR per 1 USD" from /asset/assets at start date
    final String usdAssetJsonStart = rawAssetApi.getAssets(ASSET_USD, WHEN_START);
    final double eurPerUsdStart = findUsdEurPer1UsdFromAssetApi(mapper, usdAssetJsonStart);
    if (Double.isNaN(eurPerUsdStart) || eurPerUsdStart <= 0) {
      throw new IllegalStateException(
        "Cannot read EUR-per-USD from /asset/assets for " + ASSET_USD + " at " + WHEN_START + ". Response: " + usdAssetJsonStart
      );
    }

    final double usdQtyForContract2 = euValueEurStart / eurPerUsdStart;

    // Step 5: create temporary contract #2 (USD cash only)
    final String contractId2 = createContract2Usd(mapper, rawPortfolioApi, usdId, usdQtyForContract2);

    // Step 6: snapshot for contract #2 at start/end
    final String snap2Start = getSnapshotOrThrow(rawPortfolioApi, mapper, WHEN_START, "ASSET", contractId2, "EUR", "CONTRACT2_START");
    final String snap2End   = getSnapshotOrThrow(rawPortfolioApi, mapper, WHEN_END,   "ASSET", contractId2, "EUR", "CONTRACT2_END");

    // Contract #2 value in EUR: USD quantity * (EUR per 1 USD) at the date
    final double pUsdStartEur = priceOrThrow(mapper, snap2Start, usdId, "USD price(EUR per 1 USD) @ start");
    final double pUsdEndEur   = priceOrThrow(mapper, snap2End, usdId, "USD price(EUR per 1 USD) @ end");

    final double v2Start = usdQtyForContract2 * pUsdStartEur;
    final double v2End   = usdQtyForContract2 * pUsdEndEur;
    final double r2 = (v2End / v2Start) - 1.0;

    // Return in percent (not fraction)
    return new Api102Results(contractId1, contractId2, r1 * 100.0, r2 * 100.0);
  }

  private static String createContract1(ObjectMapper mapper,
                                        RawPortfolioApi rawPortfolioApi,
                                        String dbId,
                                        String citiId,
                                        String amundiId,
                                        String eurId) throws Exception {
    final String requested = "contract_" + UUID.randomUUID();

    final ObjectNode contract = mapper.createObjectNode();
    contract.put("contractId", requested);
    contract.put("when", WHEN_START);
    contract.put("useCorporateActions", true);

    final ArrayNode investments = contract.putArray("investments");
    investments.add(investment(mapper, dbId, QTY_DB, "EUR"));
    investments.add(investment(mapper, citiId, QTY_CITI, "USD"));
    investments.add(investment(mapper, amundiId, QTY_AMUNDI, "EUR"));
    investments.add(investment(mapper, eurId, QTY_EUR, "EUR"));

    final Response resp = rawPortfolioApi.createTemporaryContract(contract);
    final String body = readBody(resp);

    if (resp.status() < 200 || resp.status() >= 300) {
      throw new IllegalStateException("TEMP_CONTRACT_1 failed. Body: " + body);
    }

    String id = mapper.readTree(body).path("contractId").asText(null);
    if (id == null || id.isBlank()) id = requested;
    return id;
  }

  private static String createContract2Usd(ObjectMapper mapper,
                                           RawPortfolioApi rawPortfolioApi,
                                           String usdId,
                                           double usdQty) throws Exception {
    final String requested = "contract_" + UUID.randomUUID();

    final ObjectNode contract = mapper.createObjectNode();
    contract.put("contractId", requested);
    contract.put("when", WHEN_START);
    contract.put("useCorporateActions", true);

    final ArrayNode investments = contract.putArray("investments");
    investments.add(investment(mapper, usdId, usdQty, "USD"));

    final Response resp = rawPortfolioApi.createTemporaryContract(contract);
    final String body = readBody(resp);

    if (resp.status() < 200 || resp.status() >= 300) {
      throw new IllegalStateException("TEMP_CONTRACT_2 failed. Body: " + body);
    }

    String id = mapper.readTree(body).path("contractId").asText(null);
    if (id == null || id.isBlank()) id = requested;
    return id;
  }

  // ============================================================
  // =========================== API101 =========================
  // ============================================================
  private static String executeApi101(ObjectMapper mapper,
                                      RequestInterceptor auth,
                                      String profilingBaseUrl) {

    final RiskProfileApi riskApi = Feign.builder()
      .decoder(new ApiResponseDecoder(mapper))
      .encoder(new JacksonEncoder(mapper))
      .requestInterceptor(auth)
      .target(RiskProfileApi.class, profilingBaseUrl);

    final RiskProfileRawGetApi rawGet = Feign.builder()
      .requestInterceptor(auth)
      .target(RiskProfileRawGetApi.class, profilingBaseUrl);

    List<String> ownerIds = Collections.singletonList(getOwnerId());
    List<String> tags = Collections.singletonList(TAG_NATURAL_PERSON);

    RiskProfileQuestionnaire created = riskApi.createRiskProfile(
      ownerIds, false, ACCEPT_LANGUAGE, false, tags
    );

    String riskProfileId = created.getId();

    String lastModified = fetchLastModified(rawGet, riskProfileId, ACCEPT_LANGUAGE);

    RiskProfileAnswers answers = new RiskProfileAnswers();
    Answer a = new Answer();
    a.setQuestionId(QUESTION_ID);
    a.setAnswerId(ANSWER_ID);
    answers.addAnswersItem(a);

    riskApi.fillRiskProfile(riskProfileId, lastModified, answers, false, ACCEPT_LANGUAGE, null);

    lastModified = fetchLastModified(rawGet, riskProfileId, ACCEPT_LANGUAGE);

    a.setJustification(JUSTIFICATION_TEXT);
    riskApi.fillRiskProfile(riskProfileId, lastModified, answers, false, ACCEPT_LANGUAGE, null);

    return riskProfileId;
  }

  // ============================================================
  // ====================== OUTPUT FORMAT =======================
  // ============================================================
  private static void printFinalOutput(Api102Results api102, String riskProfileId) {

    System.out.println("=============================");
    System.out.println("API102 RESULTS");
    System.out.println("=============================");
    System.out.println("Temporary Contract 1 ID:");
    System.out.println(api102.contract1Id);
    System.out.println();
    System.out.println("Temporary Contract 2 ID:");
    System.out.println(api102.contract2Id);
    System.out.println();
    System.out.println("Contract 1 (European Portfolio)");
    System.out.printf("Return 2024: %.4f%%%n%n", api102.contract1ReturnPct);

    System.out.println("Contract 2 (USD Investment)");
    System.out.printf("Return 2024: %.4f%%%n", api102.contract2ReturnPct);
    System.out.println("=============================");
    System.out.println();
    System.out.println("API101 RESULTS");
    System.out.println("=============================");
    System.out.println("Risk Profile ID:");
    System.out.println(riskProfileId);
    System.out.println("=============================");
  }

  // ============================================================
  // =========================== HELPERS ========================
  // ============================================================
  private static double priceOrThrow(ObjectMapper mapper, String snapshotJson, String assetId, String label) throws Exception {
    double v = findAssetPriceEurFromSnapshot(mapper, snapshotJson, assetId);
    if (Double.isNaN(v)) {
      throw new IllegalStateException("Could not read " + label + " (NaN). Check snapshot JSON / asset IDs.");
    }
    return v;
  }

  private static String readBody(Response resp) throws Exception {
    if (resp == null || resp.body() == null) return "";
    return Util.toString(resp.body().asReader(StandardCharsets.UTF_8));
  }

  private static String getSnapshotOrThrow(
    RawPortfolioApi rawPortfolioApi,
    ObjectMapper mapper,
    String when,
    String aggregation,
    String contractId,
    String currency,
    String label
  ) throws Exception {

    Response r = rawPortfolioApi.getAssetsSnapshot(when, aggregation, contractId, currency);
    String body = readBody(r);

    if (r.status() < 200 || r.status() >= 300) {
      throw new IllegalStateException("assets-snapshot failed (" + label + "). Body: " + body);
    }

    mapper.readTree(body);
    return body;
  }

  private static ObjectNode investment(ObjectMapper om, String assetId, double qty, String currency) {
    ObjectNode n = om.createObjectNode();
    n.put("assetId", assetId);
    n.put("quantity", qty);
    n.put("currency", currency);
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

  private static String getOwnerId() {
    // Avoid hardcoding your email in GitHub
    return System.getenv().getOrDefault("OWNER_ID", "demo.user@example.com");
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

  private static String extractFirstAssetId(ObjectMapper mapper, String json) throws Exception {
    JsonNode root = mapper.readTree(json);
    JsonNode arr = root.has("assets") ? root.get("assets") : root;
    if (!arr.isArray() || arr.size() == 0) {
      throw new IllegalStateException("No assets found in response: " + json);
    }
    return arr.get(0).path("id").asText();
  }
}
