package io.github.edsuns.chaoxing;

import com.sun.tools.javac.util.Pair;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

import io.github.edsuns.net.HttpRequest;

/**
 * Created by Edsuns@qq.com on 2021/6/23.
 */
final class Remote {
    /**
     * API response state
     */
    enum State {
        LOGIN_SUCCESS, LOGIN_FAILED
    }

    /**
     * Login
     *
     * @param username phone number or schoolId
     * @param password password
     * @param schoolId can be empty if login with phone number
     * @return a pair of {@link Remote.State} and cookies
     * @throws IOException IOException
     */
    static Pair<State, Map<String, String>> login(String username, String password, String schoolId) throws IOException {
        HttpRequest request = new HttpRequest("https://passport2.chaoxing.com/api/login")
                .exec(HttpRequest.Method.GET,
                        HttpRequest.data("name", username)
                                .data("pwd", password)
                                .data("schoolid", schoolId)
                                .data("verify", 0)
                );
        JSONObject object = new JSONObject(request.getBody());
        if (!request.isBadStatus() && object.getBoolean("result")) {
            return Pair.of(State.LOGIN_SUCCESS, request.getCookies());
        }
        return Pair.of(State.LOGIN_FAILED, null);
    }

    /**
     * Validate login state
     *
     * @param cookies saved cookies
     * @return true if cookies is valid
     * @throws IOException IOException
     */
    static boolean validateLogin(Map<String, String> cookies) throws IOException {
        HttpRequest request = new HttpRequest("http://mooc1-1.chaoxing.com/api/workTestPendingNew")
                .cookies(cookies)
                .followRedirects(false)
                .get();
        return request.getStatus() == HttpURLConnection.HTTP_OK;
    }
}
