package stockmarket.io;

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
        // Create JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("secret", secretToken);
        
        // Create request with JSON body
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
                    System.out.println("Authentication response doesn't contain expected fields. Full response: " + responseBody);
                    throw new IOException("Authentication response missing session token/sid");
                }
            } catch (JsonSyntaxException e) {
                System.out.println("Failed to parse authentication response: " + e.getMessage());
                throw new IOException("Invalid JSON in authentication response: " + e.getMessage());
            }
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
            
            String ticker = "";
            String isin = "";
            String mic = "";
            String name = "";
            
            if (asset.has("ticker")) {
                ticker = asset.get("ticker").getAsString();
            }            
            if (asset.has("isin")) {
                isin = asset.get("isin").getAsString();
            }
            if (asset.has("mic")) {
                mic = asset.get("mic").getAsString();
            }
            if (asset.has("name")) {
                name = asset.get("name").getAsString();
            }
            
            Quote quote = new Quote();
            quote.ticker = ticker;
            quote.isin = isin;
            quote.name = name;
            if(exchangesNames.containsKey(mic)){
                quote.mic = exchangesNames.get(mic);
            } else{
                quote.mic = mic;
            }

            quoteList.add(quote);
        }
        
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
