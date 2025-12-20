package stockmarket.datasource;

import stockmarket.model.Quote;
import stockmarket.model.TradeData;
import stockmarket.utils.ParseUtils;
import com.google.gson.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FinamApiClient extends DataSourceBase {

    private static final String API_BASE_URL = "https://api.finam.ru/v1/";
    private static final String ASSETS_URL = API_BASE_URL + "assets";

    private OkHttpClient httpClient;
    private String jwtToken;
    private String secretToken;
    private boolean isConnected = false;

    private ArrayList<String> intervalList;

    private Map<String, String> intervalMap;

    public FinamApiClient() {
        this.data = new ArrayList<>();
        this.httpClient = new OkHttpClient();
        initIntervalMap();
        initDataLists();
    }

    private void initIntervalMap() {
        intervalMap = new HashMap<>();
        intervalMap.put("Тики", "TICKS");
        intervalMap.put("1 мин", "1min");
        intervalMap.put("5 мин", "5min");
        intervalMap.put("10 мин", "10min");
        intervalMap.put("15 мин", "15min");
        intervalMap.put("30 мин", "30min");
        intervalMap.put("1 час", "1hour");
        intervalMap.put("1 день", "1day");
        intervalMap.put("1 неделя", "1week");
        intervalMap.put("1 месяц", "1month");
    }

    private void initDataLists() {                
        intervalList = new ArrayList<>();
        intervalList.addAll(intervalMap.keySet());
    }

    @Override
    public void connect() throws InterruptedException {
        try {
            System.out.println("Connecting to Finam API...");
            
            // Secret should be set by SwingApp before calling connect()
            if (secretToken == null || secretToken.isEmpty()) {
                throw new InterruptedException("Secret token is not set. Please set secret before connecting.");
            }
            
            // Authenticate with Finam API using the secret
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

    private String authenticateWithFinam() throws IOException {
        String authUrl = API_BASE_URL + "sessions";
        
        // Create JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("secret", secretToken);
        
        System.out.println("Sending authentication request to " + authUrl);
        
        // Create request with JSON body
        okhttp3.MediaType jsonMediaType = okhttp3.MediaType.get("application/json; charset=utf-8");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(requestBody.toString(), jsonMediaType);
        
        Request request = new Request.Builder()
                .url(authUrl)
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
            
            // Parse the response to extract session token
            try {
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                
                // Extract session ID or token from response
                if (responseJson.has("sid")) {
                    String sessionId = responseJson.get("sid").getAsString();
                    System.out.println("Session established: " + sessionId);
                    return sessionId;
                } else if (responseJson.has("token")) {
                    String token = responseJson.get("token").getAsString();
                    System.out.println("Token obtained from authentication");
                    return token;
                } else {
                    // Log full response for debugging
                    System.err.println("Authentication response doesn't contain expected fields. Full response: " + responseBody);
                    throw new IOException("Authentication response missing session token/sid");
                }
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to parse authentication response: " + e.getMessage());
                throw new IOException("Invalid JSON in authentication response: " + e.getMessage());
            }
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
            String responseBody = response.body().string();
            
            System.out.println("Assets API response status: " + response.code());
            
            if (!response.isSuccessful()) {
                System.err.println("Failed to load assets from API: " + responseBody);
                return quoteList;
            }
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                System.err.println("Empty assets response");
                return quoteList;
            }
            
            // Parse the assets response
            try {
                JsonElement element = JsonParser.parseString(responseBody);
                JsonArray assetsArray = null;
                
                // Handle different response formats
                if (element.isJsonArray()) {
                    assetsArray = element.getAsJsonArray();
                } else if (element.isJsonObject()) {
                    JsonObject responseObj = element.getAsJsonObject();
                    // Try common field names for assets array
                    if (responseObj.has("assets")) {
                        assetsArray = responseObj.getAsJsonArray("assets");
                    } else if (responseObj.has("data")) {
                        assetsArray = responseObj.getAsJsonArray("data");
                    } else if (responseObj.has("payload")) {
                        assetsArray = responseObj.getAsJsonArray("payload");
                    }
                }
                
                if (assetsArray == null) {
                    System.err.println("Could not find assets array in response");
                    return quoteList;
                }
                                
                for (JsonElement assetElement : assetsArray) {
                    if (!assetElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject asset = assetElement.getAsJsonObject();
                    
                    // Extract asset information
                    String ticker = "";
                    String isin = "";
                    String mic = "";
                    String name = "";
                    
                    // Try different field names for the identifier
                    if (asset.has("ticker")) {
                        ticker = asset.get("ticker").getAsString();
                    } else if (asset.has("code")) {
                        ticker = asset.get("code").getAsString();
                    }
                    
                    if (asset.has("isin")) {
                        isin = asset.get("isin").getAsString();
                    }
                    if (asset.has("mic")) {
                        mic = asset.get("mic").getAsString();
                    }
                    
                    if (asset.has("name")) {
                        name = asset.get("name").getAsString();
                    } else if (asset.has("description")) {
                        name = asset.get("description").getAsString();
                    }
                    
                    Quote quote = new Quote();
                    quote.ticker = ticker;
                        quote.isin = isin;
                    if (!name.isEmpty()) {
                        quote.name = name;
                    } else {
                        quote.name = ticker;
                    }
                    if (mic != null) {
                        quote.mic = mic;
                    }
                    
                    // Add the quote name to the list
                    quoteList.add(quote);
                }
                
                System.out.println("Successfully loaded " + quoteList.size() + " assets from API");               
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to parse assets response: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error loading assets from API: " + e.getMessage());
            e.printStackTrace();
        }

        return quoteList;
    }

    @Override
    public ArrayList<String> getIntervalList() throws Exception {
        return new ArrayList<>(intervalList);
    }

    public void setSecretToken(String token) throws IOException {
        if (token == null || token.isEmpty()) {
            throw new IOException("Auth token must be non-empty");
        }
        this.secretToken = token;
    }

    private void parseApiResponse(String jsonResponse) throws Exception {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new Exception("Empty response body");
        }

        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray barsArray = root.getAsJsonArray("bars");

            if (barsArray == null) {
                // Check if response contains error message
                if (root.has("error")) {
                    throw new Exception("API Error: " + root.get("error").getAsString());
                }
                throw new Exception("Invalid API response format - 'bars' array not found");
            }

            if (barsArray.size() == 0) {
                System.out.println("API returned empty bars array, using mock data");
                return;
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (JsonElement element : barsArray) {
                JsonObject bar = element.getAsJsonObject();

                double open = bar.get("o").getAsDouble();
                double high = bar.get("h").getAsDouble();
                double low = bar.get("l").getAsDouble();
                double close = bar.get("c").getAsDouble();
                long volume = bar.get("v").getAsLong();
                String timestamp = bar.get("ts").getAsString();

                // Parse timestamp
                LocalDateTime dateTime = LocalDateTime.parse(timestamp.substring(0, 19));
                String dateStr = dateTime.format(dateFormatter);
                long time = dateTime.getHour() * 3600 + dateTime.getMinute() * 60;

                // TradeData tradeData = new TradeData(
                //         config.getSelectedSymbol(),
                //         time,
                //         open,
                //         close,
                //         volume,
                //         dateStr,
                //         high,
                //         low
                // );
                // data.add(tradeData);
            }
        } catch (JsonSyntaxException jsonError) {
            throw new Exception("JSON parsing error: " + jsonError.getMessage() +
                    ". Response may not be valid JSON. Enable lenient parsing or check API response format.");
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getToken() {
        return jwtToken;
    }

    @Override
    public String toString() {
        return "FinamApi";
    }
}
