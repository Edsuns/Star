package io.github.edsuns.chaoxing;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.github.edsuns.chaoxing.model.Course;
import io.github.edsuns.chaoxing.model.Timing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Edsuns@qq.com on 2021/6/23.
 */
public class RemoteTest {
    String username;
    String password;
    String schoolId;

    Map<String, String> cookies;

    {
        try {
            // load config from config.properties
            Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("config.properties"));
            username = properties.getProperty("username");
            password = properties.getProperty("password");
            schoolId = properties.getProperty("schoolId");

            // login
            Map<String, String> result = Remote.login(username, password, schoolId);
            assertNotNull(result);
            cookies = result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void validateLogin() throws IOException {
        assertTrue(Remote.validateLogin(cookies));
    }

    @Test
    public void getAllCourses() throws IOException {
        List<Course> allCourses = Remote.getAllCourses(cookies);
        assertFalse(allCourses.isEmpty());
    }

    @Test
    public void getActiveTimingList() throws IOException {
        List<Course> allCourses = Remote.getAllCourses(cookies);
        boolean hasActive = false;
        outer:
        for (Course course : allCourses) {
            List<Timing> activeTimingList = Remote.getActiveTimingList(cookies, course);
            for (Timing timing : activeTimingList) {
                if (timing.state != Timing.State.UNKNOWN
                        && timing.type != Timing.Type.UNKNOWN) {
                    hasActive = true;
                    break outer;
                }
            }
        }
        assertTrue(hasActive);
    }

    @Test
    public void getName() throws IOException {
        String name = Remote.getName(cookies);
        assertNotNull(name);
    }
}
