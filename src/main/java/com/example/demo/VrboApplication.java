package com.example.demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

@SpringBootApplication
public class VrboApplication {

    public static void main(String[] args) {
        String address = "72 W Monroe St, Chicago, IL 60603, USA";
        int pageSize = 50;
        try {
            URL url = new URL("https://www.vrbo.com/graphql");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            setRequestHeaders(conn);

            String requestBody = createRequestBody(address, pageSize);
            System.out.println("Request Body:" + requestBody);
            sendRequest(conn, requestBody);

            String response = getResponse(conn);
            generateCSV(response);

            System.out.println("CSV file generated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String getResponse(HttpURLConnection conn) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response.toString();
    }

    private static void sendRequest(HttpURLConnection conn, String requestBody) throws Exception {
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    private static String createRequestBody(String address, int pageSize) {
        String jsonInputString = String.format("""
                {
                    "operationName": "LodgingPwaPropertySearch",
                    "variables": {
                        "context": {
                            "siteId": 9001001,
                            "locale": "en_US",
                            "eapid": 1,
                            "currency": "USD",
                            "device": {"type": "DESKTOP"},
                            "identity": {
                                "duaid": "65cbd87c-ebb5-ab83-a4c1-812db78bb787",
                                "expUserId": "-1",
                                "tuid": "-1",
                                "authState": "ANONYMOUS"
                            },
                            "privacyTrackingState": "CAN_TRACK",
                            "debugContext": {"abacusOverrides": []}
                        },
                        "criteria": {
                            "primary": {
                                "dateRange": {
                                    "checkInDate": {"day": 1, "month": 3, "year": 2024},
                                    "checkOutDate": {"day": 5, "month": 3, "year": 2024}
                                },
                                "destination": {
                                    "regionName": "%s",
                                    "regionId": null,
                                    "coordinates": null,
                                    "pinnedPropertyId": null,
                                    "propertyIds": null,
                                    "mapBounds": null
                                },
                                "rooms": [{"adults": 2, "children": []}]
                            },
                            "secondary": {
                                "counts": [
                                    {"id": "resultsStartingIndex", "value": 150},
                                    {"id": "resultsSize", "value": %d}
                                ],
                                "booleans": [],
                                "selections": [
                                    {"id": "sort", "value": "RECOMMENDED"},
                                    {"id": "privacyTrackingState", "value": "CAN_TRACK"},
                                    {"id": "useRewards", "value": "SHOP_WITHOUT_POINTS"},
                                    {"id": "searchId", "value": "d1342ebe-2e4c-4c8d-8838-a3967204a6f2"}
                                ],
                                "ranges": []
                            }
                        },
                        "destination": {
                            "regionName": null,
                            "regionId": null,
                            "coordinates": null,
                            "pinnedPropertyId": null,
                            "propertyIds": null,
                            "mapBounds": null
                        },
                        "shoppingContext": {"multiItem": null},
                        "returnPropertyType": false,
                        "includeDynamicMap": true
                    },
                    "extensions": {
                        "persistedQuery": {
                            "sha256Hash": "e4ffcd90dd44f01455f9ddd89228915a177f9ec674f0df0db442ea1b20f551c3",
                            "version": 1
                        }
                    }
                }""", address, pageSize);
        return jsonInputString;
    }

    private static void setRequestHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("authority", "www.vrbo.com");
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("accept-language", "en-GB,en-US;q=0.9,en;q=0.8,no;q=0.7,de;q=0.6");
        conn.setRequestProperty("cache-control", "no-cache");
        conn.setRequestProperty("client-info", "shopping-pwa,unknown,unknown");
        conn.setRequestProperty("origin", "https://www.vrbo.com");
        conn.setRequestProperty("pragma", "no-cache");
        conn.setRequestProperty("referer",
                "https://www.vrbo.com/search?adults=2&destination=73%20W%20Monroe%20St%2C%20Chicago%2C%20IL%2060603%2C%20USA&endDate=2024-03-05&latLong=&mapBounds=&pwaDialog=&regionId&semdtl=&sort=RECOMMENDED&startDate=2024-03-01&theme=&userIntent=");
        conn.setRequestProperty("sec-ch-ua",
                "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
        conn.setRequestProperty("sec-ch-ua-mobile", "?0");
        conn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
        conn.setRequestProperty("sec-fetch-dest", "empty");
        conn.setRequestProperty("sec-fetch-mode", "cors");
        conn.setRequestProperty("sec-fetch-site", "same-origin");
        conn.setRequestProperty("user-agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("x-enable-apq", "true");
        conn.setRequestProperty("x-page-id", "page.Hotel-Search,H,20");
    }

    private static void generateCSV(String response) throws Exception {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray listings = jsonResponse.getJSONObject("data").getJSONObject("propertySearch")
                .getJSONArray("propertySearchListings");

        try (FileWriter csvWriter = new FileWriter("listings.csv")) {
            csvWriter.append("Listing ID,Listing Title,Nightly Price,Listing URL\n");

            for (int i = 0; i < listings.length(); i++) {
                JSONObject listing = listings.getJSONObject(i);
                String id = listing.getString("id");
                String title = listing.getJSONObject("headingSection").getString("heading");
                String url = listing.getJSONObject("cardLink").getJSONObject("resource").getString("value");
                String price = listing.getJSONObject("priceSection").getJSONArray("priceSummary")
                        .getJSONObject(0).getJSONArray("displayMessages").getJSONObject(0)
                        .getJSONObject("price").getString("formatted");

                csvWriter.append(String.format("%s,%s,%s,%s\n", id, title, price, url));
            }
        }
        System.out.println(listings);

    }
}