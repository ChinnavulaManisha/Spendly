package com.spendly.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for parsing and writing JSON with standard HTTP responses.
 */
public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    public static Gson getGson() {
        return GSON;
    }

    /**
     * Serializes any Java object to JSON string.
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /**
     * Deserializes JSON string to target class.
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }

    /**
     * Deserializes JSON string to generic type.
     */
    public static <T> T fromJson(String json, Type typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }

    /**
     * Reads JSON body from HttpServletRequest.
     */
    public static String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Reads JSON body into a JsonObject.
     */
    public static JsonObject readJsonObject(HttpServletRequest request) throws IOException {
        String body = readRequestBody(request);
        if (body == null || body.trim().isEmpty()) {
            return new JsonObject();
        }
        JsonElement elem = JsonParser.parseString(body);
        return elem.isJsonObject() ? elem.getAsJsonObject() : new JsonObject();
    }

    /**
     * Writes JSON response with status code and Content-Type: application/json.
     */
    public static void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            out.print(toJson(data));
            out.flush();
        }
    }

    /**
     * Sends standard success response: {"success": true, "message": "...", "data": ...}
     */
    public static void sendSuccess(HttpServletResponse response, String message, Object data) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", message);
        map.put("data", data);
        sendJsonResponse(response, HttpServletResponse.SC_OK, map);
    }

    public static void sendSuccess(HttpServletResponse response, Object data) throws IOException {
        sendSuccess(response, "Success", data);
    }

    /**
     * Sends standard error response: {"success": false, "message": "..."}
     */
    public static void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        sendJsonResponse(response, statusCode, map);
    }
}
