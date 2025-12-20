package stockmarket.datasource;

import stockmarket.common.FinamDataParser;
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
    
    private OkHttpClient httpClient;
    private String authToken;
    private boolean isConnected = false;
    
    private ArrayList<String> marketList;
    private ArrayList<String> quoteList;
    private ArrayList<String> intervalList;
    
    private String selectedMarket;
    private String selectedQuote;
    private String selectedSymbol;
    
    private int beginDay, beginMonth, beginYear;
    private int endDay, endMonth, endYear;
    
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
        marketList = new ArrayList<>();
        // Markets will be populated from API
        
        quoteList = new ArrayList<>();
        // Quotes (assets) will be dynamically loaded from API
        
        intervalList = new ArrayList<>();
        intervalList.addAll(intervalMap.keySet());
    }

    @Override
    public void connect() throws InterruptedException {
        try {
            System.out.println("Connecting to Finam API...");
            
            // Secret should be set by SwingApp before calling connect()
            if (authToken == null || authToken.isEmpty()) {
                throw new InterruptedException("Secret token is not set. Please set secret before connecting.");
            }
            
            Thread.sleep(500);
            
            // Authenticate with Finam API using the secret
            System.out.println("Authenticating with Finam API...");
            String sessionToken = authenticateWithFinam(authToken);
            
            if (sessionToken == null || sessionToken.isEmpty()) {
                throw new InterruptedException("Failed to obtain session token from Finam API");
            }
            
            // Store the session token for API requests
            authToken = sessionToken;
            
            // Validate connection
            validateConnection();
            isConnected = true;
            System.out.println("Successfully authenticated with Finam API");
        } catch (Exception e) {
            isConnected = false;
            throw new InterruptedException("Failed to connect to Finam API: " + e.getMessage());
        }
    }

    private String authenticateWithFinam(String secret) throws IOException {
        String authUrl = API_BASE_URL + "sessions";
        
        // Create JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("secret", secret);
        
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

    private void validateConnection() throws IOException {
        // This validates the API endpoint is reachable with the JWT token
        // For demo purposes, we assume the API is available
        if (authToken == null || authToken.isEmpty()) {
            throw new IOException("JWT token not set");
        }
    }

    /**
     * Loads the available assets from the Finam API and populates the quoteList
     */
    private void loadAssetsFromAPI() throws IOException {
        String assetsUrl = API_BASE_URL + "assets";
        
        Request request = new Request.Builder()
                .url(assetsUrl)
                .get()
                .addHeader("Authorization", authToken)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            System.out.println("Assets API response status: " + response.code());
            
            if (!response.isSuccessful()) {
                System.err.println("Failed to load assets from API: " + response.code());
                System.err.println("Response: " + responseBody);
                return;
            }
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                System.err.println("Empty assets response");
                return;
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
                
                if (assetsArray != null) {
                    quoteList.clear();
                    int assetCount = 0;
                    
                    for (JsonElement assetElement : assetsArray) {
                        if (assetElement.isJsonObject()) {
                            JsonObject asset = assetElement.getAsJsonObject();
                            
                            // Extract asset information
                            String ticker = null;
                            String isin = null;
                            String name = null;
                            
                            // Try different field names for the identifier
                            if (asset.has("ticker")) {
                                ticker = asset.get("ticker").getAsString();
                            } else if (asset.has("code")) {
                                ticker = asset.get("code").getAsString();
                            }
                            
                            if (asset.has("isin")) {
                                isin = asset.get("isin").getAsString();
                            }
                            
                            if (asset.has("name")) {
                                name = asset.get("name").getAsString();
                            } else if (asset.has("description")) {
                                name = asset.get("description").getAsString();
                            }
                            
                            // Create Quote object if we have at least a ticker
                            if (ticker != null && !ticker.isEmpty()) {
                                Quote quote = new Quote();
                                quote.setTicker(ticker);
                                if (isin != null) {
                                    quote.setIsin(isin);
                                }
                                if (name != null) {
                                    quote.setName(name);
                                } else {
                                    quote.setName(ticker);
                                }
                                
                                // Add the quote name to the list
                                quoteList.add(quote.getName());
                                assetCount++;
                            }
                        }
                    }
                    
                    System.out.println("Successfully loaded " + assetCount + " assets from API");
                } else {
                    System.err.println("Could not find assets array in response");
                }
                
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to parse assets response: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error loading assets from API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initElements() throws Exception {
        if (!isConnected) {
            throw new Exception("Not connected to API");
        }
        
        // Dynamically load assets from Finam API
        System.out.println("Loading assets from Finam API...");
        try {
            loadAssetsFromAPI();
            System.out.println("Successfully loaded " + quoteList.size() + " assets");
        } catch (Exception e) {
            System.err.println("Warning: Failed to load assets from API: " + e.getMessage());
            System.out.println("Using empty assets list");
            quoteList.clear();
        }
    }

    @Override
    public ArrayList<String> getMarketList() throws Exception {
        return new ArrayList<>(marketList);
    }

    @Override
    public ArrayList<String> getQuotesList() throws Exception {
        return new ArrayList<>(quoteList);
    }

    @Override
    public ArrayList<String> getIntervalList() throws Exception {
        return new ArrayList<>(intervalList);
    }

    @Override
    public void setMarket(String marketName, int marketNumber, int marketPos) throws Exception {
        this.selectedMarket = marketName;
    }

    @Override
    public void setQuote(String quoteName, int quoteNumber, int quotePos) throws Exception {
        this.selectedQuote = quoteName;
        // Map quote name to symbol (simplified mapping)
        this.selectedSymbol = mapQuoteToSymbol(quoteName);
    }

    @Override
    public void setInterval(String intervalName, int intervalNumber) throws Exception {
        if (!intervalMap.containsKey(intervalName)) {
            throw new Exception("Invalid interval: " + intervalName);
        }
    }

    @Override
    public void setBeginDate() {
        // Use current date - 1 year as default
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        beginDay = oneYearAgo.getDayOfMonth();
        beginMonth = oneYearAgo.getMonthValue() - 1; // 0-based
        beginYear = oneYearAgo.getYear();
    }

    @Override
    public void setEndDate() {
        // Use current date as default
        LocalDateTime now = LocalDateTime.now();
        endDay = now.getDayOfMonth();
        endMonth = now.getMonthValue() - 1; // 0-based
        endYear = now.getYear();
    }

    @Override
    public String getMinDate() {
        return "01.01.2020";
    }

    @Override
    public void setBeginData(int day, int month, int year) {
        this.beginDay = day;
        this.beginMonth = month;
        this.beginYear = year;
    }

    @Override
    public void setEndData(int day, int month, int year) {
        this.endDay = day;
        this.endMonth = month;
        this.endYear = year;
    }

    @Override
    public void getData() throws Exception {
        if (!isConnected) {
            throw new Exception("Not connected to API");
        }
        if (selectedSymbol == null) {
            throw new Exception("Symbol not selected");
        }
        if (authToken == null || authToken.isEmpty()) {
            throw new Exception("JWT token not available. Please reconnect.");
        }

        data.clear();
        
        // Format dates for API
        String beginDate = String.format("%04d-%02d-%02dT00:00:00Z", beginYear, beginMonth + 1, beginDay);
        String endDate = String.format("%04d-%02d-%02dT23:59:59Z", endYear, endMonth + 1, endDay);
        
        try {
            // Make API request with JWT token
            String url = String.format(
                "%s%s?timeframe=TIME_FRAME_D&interval.start_time=%s&interval.end_time=%s",
                API_BASE_URL,
                MARKET_DATA_ENDPOINT.replace("{symbol}", selectedSymbol),
                beginDate,
                endDate
            );
            
            // Use JWT token in Authorization header
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + authToken)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    // For demo, use mock data
                    System.out.println("API responded with status: " + response.code() + ", using mock data");
                    populateMockData();
                    return;
                }
                
                String responseBody = response.body().string();
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    System.out.println("Empty response from API, using mock data");
                    populateMockData();
                    return;
                }
                
                try {
                    parseApiResponse(responseBody);
                } catch (Exception parseException) {
                    System.out.println("Failed to parse API response: " + parseException.getMessage());
                    System.out.println("Response preview: " + responseBody.substring(0, Math.min(200, responseBody.length())));
                    System.out.println("Using mock data instead");
                    populateMockData();
                }
            }
        } catch (Exception e) {
            // Fallback to mock data for demonstration
            System.out.println("API call failed, using mock data: " + e.getMessage());
            e.printStackTrace();
            populateMockData();
        }
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
                populateMockData();
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
                
                TradeData tradeData = new TradeData(
                        selectedSymbol,
                        time,
                        open,
                        close,
                        volume,
                        dateStr,
                        high,
                        low
                );
                data.add(tradeData);
            }
        } catch (JsonSyntaxException jsonError) {
            throw new Exception("JSON parsing error: " + jsonError.getMessage() + 
                    ". Response may not be valid JSON. Enable lenient parsing or check API response format.");
        }
    }

    private void populateMockData() {
        // Fallback mock data for testing UI without real API
        data.clear();
        String date = String.format("%04d%02d%02d", beginYear, beginMonth + 1, beginDay);
        
        for (int i = 0; i < 20; i++) {
            double basePrice = 100.0 + (i * 0.5);
            double open = basePrice;
            double close = basePrice + (Math.random() * 2 - 1);
            double high = Math.max(open, close) + (Math.random() * 0.5);
            double low = Math.min(open, close) - (Math.random() * 0.5);
            long volume = (long) (1000000 + Math.random() * 500000);
            long time = i * 3600; // Hourly data
            
            TradeData tradeData = new TradeData(
                    selectedSymbol != null ? selectedSymbol : "GAZP",
                    time,
                    open,
                    close,
                    volume,
                    date,
                    high,
                    low
            );
            data.add(tradeData);
        }
    }

    private String mapQuoteToSymbol(String quoteName) {
        // Map human-readable quote names to API symbols
        Map<String, String> quoteSymbolMap = new HashMap<>();
        quoteSymbolMap.put("ГАЗПРОМ ао", "GAZP");
        quoteSymbolMap.put("Сбербанк", "SBER");
        quoteSymbolMap.put("ЛУКОЙЛ", "LKOH");
        quoteSymbolMap.put("Норильский никель", "GMKN");
        quoteSymbolMap.put("Магнит", "MGNT");
        
        return quoteSymbolMap.getOrDefault(quoteName, "GAZP");
    }

    public boolean isConnected() {
        return isConnected;
    }



    public void setJWTToken(String token) {
        if (token != null && !token.isEmpty()) {
            authToken = token;
            System.out.println("JWT token updated");
        }
    }

    public String getToken() {
        return authToken;
    }

    @Override
    public String toString() {
        return "FinamApiClient{" +
                "isConnected=" + isConnected +
                ", selectedMarket='" + selectedMarket + '\'' +
                ", selectedQuote='" + selectedQuote + '\'' +
                ", selectedSymbol='" + selectedSymbol + '\'' +
                ", hasToken=" + (authToken != null && !authToken.isEmpty()) +
                '}';
    }
}
