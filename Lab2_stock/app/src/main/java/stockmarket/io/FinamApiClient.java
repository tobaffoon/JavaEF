package stockmarket.io;

import stockmarket.model.Bar;
import stockmarket.model.Quote;
import stockmarket.utils.TimeUtils;

import com.google.gson.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinamApiClient implements DataSourceBase {
    private static final String API_BASE_URL = "https://api.finam.ru/v1/";
    private static final String AUTH_URL = API_BASE_URL + "sessions";
    private static final String ASSETS_URL = API_BASE_URL + "assets";
    private static final String EXCHANGES_URL = API_BASE_URL + "exchanges";

    private OkHttpClient httpClient;
    private String jwtToken;
    private String secretToken;
    private boolean isConnected = false;

    private Map<String, String> exchangesNames;

    public FinamApiClient() {
        this.httpClient = new OkHttpClient();
    }

    public void setSecretToken(String token) throws IOException {
        if (token == null || token.isEmpty()) {
            throw new IOException("Auth token must be non-empty");
        }
        this.secretToken = token;
    }

    public String getToken() {
        return jwtToken;
    }

    @Override
    public void connect() throws Exception {
        try {
            System.out.println("Connecting to Finam API...");
            
            if (secretToken == null || secretToken.isEmpty()) {
                throw new InterruptedException("Secret token is not set. Please set secret before connecting.");
            }
            
            System.out.println("Authenticating with Finam API...");
            String sessionToken = authenticateWithFinam();
            
            if (sessionToken == null || sessionToken.isEmpty()) {
                throw new InterruptedException("Failed to obtain session token from Finam API");
            }

            jwtToken = sessionToken;
            isConnected = true;
            System.out.println("Successfully authenticated with Finam API");
        } catch (Exception e) {
            isConnected = false;
            throw new InterruptedException("Failed to connect to Finam API: " + e.getMessage());
        }
    }

    @Override
    public ArrayList<Quote> getQuotesList() throws Exception {
        var quoteList = new ArrayList<Quote>();

        Request request = new Request.Builder()
                .url(ASSETS_URL)
                .get()
                .addHeader("Authorization", jwtToken)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            System.out.println("Fetching assets and markets from Finam...");

            String responseBody = response.body().string();
            
            System.out.println("Assets API response status: " + response.code());
            
            if (!response.isSuccessful()) {
                System.out.println("Failed to load assets from API: " + responseBody);
                return quoteList;
            }
            if (exchangesNames == null) {
                exchangesNames = getExchangesNames();
            }
            quoteList = parseAssetsResponse(responseBody);
        } catch (Exception e) {
            System.out.println("Error loading assets from API: " + e.getMessage());
            e.printStackTrace();
        }

        return quoteList;
    }

    private String authenticateWithFinam() throws IOException {        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("secret", secretToken);
        
        okhttp3.MediaType jsonMediaType = okhttp3.MediaType.get("application/json; charset=utf-8");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(requestBody.toString(), jsonMediaType);
        
        Request request = new Request.Builder()
                .url(AUTH_URL)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            System.out.println("Auth response status: " + response.code());
            System.out.println("Auth response: " + responseBody.substring(0, Math.min(200, responseBody.length())));
            
            if (!response.isSuccessful()) {
                throw new IOException("Authentication failed with status: " + response.code());
            }
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new IOException("Empty authentication response");
            }
            
            try {
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                
                if (responseJson.has("sid")) {
                    String sessionId = responseJson.get("sid").getAsString();
                    System.out.println("Session established: " + sessionId);
                    return sessionId;
                } else if (responseJson.has("token")) {
                    String token = responseJson.get("token").getAsString();
                    System.out.println("Token obtained from authentication");
                    return token;
                } else {
                    System.out.println("Authentication response doesn't contain expected fields. Full response: " + responseBody);
                    throw new IOException("Authentication response missing session token/sid");
                }
            } catch (JsonSyntaxException e) {
                throw new IOException("Invalid JSON in authentication response: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Bar> getBars(String symbol, LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        String url = API_BASE_URL + "instruments/" + symbol + "/bars";
        String startTimeStr = TimeUtils.formatFinamDateTime(startTime);
        String endTimeStr = TimeUtils.formatFinamDateTime(endTime);

        System.out.println("Fetching bars for symbol: " + symbol + 
            ", from " + startTimeStr + 
            " to " + endTimeStr);

        Request request = new Request.Builder()
                .url(url + "?interval.start_time=" + startTimeStr 
                    + "&interval.end_time=" + endTimeStr
                    + "&timeframe=TIME_FRAME_D"
                )
                .get()
                .addHeader("Authorization", jwtToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();

            System.out.println("Bars API response status: " + response.code());

            if (!response.isSuccessful()) {
                throw new Exception("Failed to fetch bars: " + responseBody);
            }

            return parseBarsResponse(responseBody);
        } catch (Exception e) {
            System.out.println("Error fetching bars: " + e.getMessage());
            throw e;
        }
    }

    private List<Bar> parseBarsResponse(String jsonResponse) throws Exception {
        List<Bar> bars = new ArrayList<>();

        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray barsArray = root.getAsJsonArray("bars");

        if (barsArray == null) {
            throw new Exception("Invalid API response format - 'bars' array not found");
        }

        if (barsArray.size() == 0) {
            throw new Exception("API returned empty bars array");
        }

        for (JsonElement element : barsArray) {
            JsonObject barObj = element.getAsJsonObject();

            // Parse timestamp
            LocalDateTime timestamp = null;
            if (barObj.has("timestamp")) {
                String timestampStr = barObj.get("timestamp").getAsString();
                timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
            }

            BigDecimal open = BigDecimal.ZERO;
            if (barObj.has("open") && barObj.get("open").isJsonObject()) {
                open = new BigDecimal(barObj.getAsJsonObject("open").get("value").getAsString());
            }

            BigDecimal high = BigDecimal.ZERO;
            if (barObj.has("high") && barObj.get("high").isJsonObject()) {
                high = new BigDecimal(barObj.getAsJsonObject("high").get("value").getAsString());
            }

            BigDecimal low = BigDecimal.ZERO;
            if (barObj.has("low") && barObj.get("low").isJsonObject()) {
                low = new BigDecimal(barObj.getAsJsonObject("low").get("value").getAsString());
            }

            BigDecimal close = BigDecimal.ZERO;
            if (barObj.has("close") && barObj.get("close").isJsonObject()) {
                close = new BigDecimal(barObj.getAsJsonObject("close").get("value").getAsString());
            }

            BigDecimal volume = BigDecimal.ZERO;
            if (barObj.has("volume") && barObj.get("volume").isJsonObject()) {
                volume = new BigDecimal(barObj.getAsJsonObject("volume").get("value").getAsString());
            }

            Bar bar = new Bar(timestamp, open, high, low, close, volume);
            bars.add(bar);
        }

        System.out.println("Successfully loaded " + bars.size() + " entries from API");   

        return bars;
    }

    private HashMap<String, String> getExchangesNames() throws Exception {
        var exchangeMap = new HashMap<String, String>();

        Request request = new Request.Builder()
                .url(EXCHANGES_URL)
                .get()
                .addHeader("Authorization", jwtToken)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            System.out.println("Exchanges API response status: " + response.code());
            
            if (!response.isSuccessful()) {
                System.out.println("Failed to load exchanges from API: " + responseBody);
                return exchangeMap;
            }
            exchangeMap = parseExchangesMap(responseBody);
        } catch (Exception e) {
            System.out.println("Error loading exchanges from API: " + e.getMessage());
            e.printStackTrace();
        }

        return exchangeMap;
    }

    private ArrayList<Quote> parseAssetsResponse(String jsonResponse) throws Exception {
        var quoteList = new ArrayList<Quote>();
        JsonElement element = JsonParser.parseString(jsonResponse);
        JsonArray assetsArray = null;
        
        if (element.isJsonObject()) {
            JsonObject responseObj = element.getAsJsonObject();
            if (responseObj.has("assets")) {
                assetsArray = responseObj.getAsJsonArray("assets");
            }
        }
        
        if (assetsArray == null) {
            System.out.println("Could not find assets array in response");
            return quoteList;
        }
                        
        for (JsonElement assetElement : assetsArray) {
            if (!assetElement.isJsonObject()) {
                continue;
            }
            JsonObject asset = assetElement.getAsJsonObject();
            
            String mic = "";
            String name = "";
            String symbol = "";
            
            if (asset.has("mic")) {
                mic = asset.get("mic").getAsString().trim();
            }
            if (asset.has("name")) {
                name = asset.get("name").getAsString().trim();
            }
            if (asset.has("symbol")) {
                symbol = asset.get("symbol").getAsString().trim();
            }
            
            if(name.isEmpty()){
                continue;
            }

            if(exchangesNames.containsKey(mic)){
                mic = exchangesNames.get(mic);
            }

            Quote quote = new Quote(name, mic, symbol);
            quoteList.add(quote);
        }
        
        quoteList.sort((q1, q2) -> q1.name().compareToIgnoreCase(q2.name()));

        System.out.println("Successfully loaded " + quoteList.size() + " assets from API");               
    
        return quoteList;
    }

    private HashMap<String, String> parseExchangesMap(String jsonResponse) throws Exception {
        var exchangeMap = new HashMap<String, String>();
        JsonElement element = JsonParser.parseString(jsonResponse);
        JsonArray exchangesArray = null;
        
        if (element.isJsonObject()) {
            JsonObject responseObj = element.getAsJsonObject();
            if (responseObj.has("exchanges")) {
                exchangesArray = responseObj.getAsJsonArray("exchanges");
            }
        }
        
        if (exchangesArray == null) {
            System.out.println("Could not find exchanges array in response");
            return exchangeMap;
        }
                        
        for (JsonElement exchangeElement : exchangesArray) {
            if (!exchangeElement.isJsonObject()) {
                continue;
            }
            JsonObject asset = exchangeElement.getAsJsonObject();
            
            String mic = "";
            String name = "";
            
            if (asset.has("mic")) {
                mic = asset.get("mic").getAsString();
            }
            if (asset.has("name")) {
                name = asset.get("name").getAsString();
            }
            
            exchangeMap.put(mic, name);
        }
        
        System.out.println("Successfully loaded " + exchangeMap.size() + " exchanges from API");               
    
        return exchangeMap;
    }

    @Override
    public String toString() {
        return "FinamApi";
    }
}
