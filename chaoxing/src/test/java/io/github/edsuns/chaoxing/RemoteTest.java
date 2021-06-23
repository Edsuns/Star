package io.github.edsuns.chaoxing;

import com.sun.tools.javac.util.Pair;

import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * Created by Edsuns@qq.com on 2021/6/23.
 */
public class RemoteTest {
    String username;
    String password;
    String schoolId;

    {
        try {
            // load config from config.properties
            Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("config.properties"));
            username = properties.getProperty("username");
            password = properties.getProperty("password");
            schoolId = properties.getProperty("schoolId");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void login() throws IOException {
        Pair<Remote.State, Map<String, String>> result = Remote.login(username, password, schoolId);
        assertTrue(Remote.validateLogin(result.snd));
    }
}
