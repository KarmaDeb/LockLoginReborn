package ml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.url.HttpUtil;
import ml.karmaconfigs.api.common.utils.url.URLUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main {

    private final static KarmaSource source = new KarmaSource() {
        @Override
        public String name() {
            return "LockLogin";
        }

        @Override
        public String version() {
            return null;
        }

        @Override
        public String description() {
            return null;
        }

        @Override
        public String[] authors() {
            return new String[0];
        }

        @Override
        public String updateURL() {
            return null;
        }
    };

    public static void main(String[] args) {
        System.out.println(mojangActive());
    }

    static boolean mojangActive() {
        boolean online = false;
        try {
            URL url = new URL("https://authserver.mojang.com/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                online = response.toString().toLowerCase().contains("\"status\":\"ok\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return online;
    }
}
