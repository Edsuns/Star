package io.github.edsuns.chaoxing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.github.edsuns.chaoxing.model.Course;
import io.github.edsuns.chaoxing.model.Timing;

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
public class CXing {
    public interface CookieStorage {
        void saveCookies(String username, Map<String, String> cookies);

        Map<String, String> getCookies(String username);
    }

    public interface PhotoProvider {
        InputStream getPhotoAsInputStream();
    }

    private final String username;
    @Nullable
    private final String schoolId;

    private final CookieStorage cookieStorage;

    public CXing(String username, CookieStorage cookieStorage) {
        this(username, null, cookieStorage);
    }

    public CXing(String username, @Nullable String schoolId, CookieStorage cookieStorage) {
        this.username = username;
        this.schoolId = schoolId;
        this.cookieStorage = cookieStorage;
    }

    private Map<String, String> getCookies() {
        return cookieStorage.getCookies(username);
    }

    public boolean login(String password) throws IOException {
        Map<String, String> cookies =
                Remote.login(username, password, schoolId == null ? "" : schoolId);
        if (cookies != null) {
            cookieStorage.saveCookies(username, cookies);
            return true;
        }
        return false;
    }

    public boolean validateLogin() throws IOException {
        return Remote.validateLogin(getCookies());
    }

    public List<Course> getAllCourses() throws IOException {
        return Remote.getAllCourses(getCookies());
    }

    public List<Timing> getActiveTimingList(Course course) throws IOException {
        return getActiveTimingList(course, false);
    }

    public List<Timing> getActiveTimingList(Course course, boolean includesEnded) throws IOException {
        return Remote.getActiveTimingList(getCookies(), course, includesEnded);
    }

    public boolean normalOrPhotoTiming(Timing timing, PhotoProvider photoProvider) throws IOException {
        if (Remote.normalTiming(getCookies(), timing)) {
            return true;
        }
        return Remote.photoTiming(getCookies(), timing, photoProvider.getPhotoAsInputStream()) != null;
    }

    public boolean qrcodeTiming(Timing timing, String enc) throws IOException {
        return Remote.qrcodeTiming(getCookies(), timing, enc) != null;
    }

    public boolean gestureTiming(Timing timing) throws IOException {
        return Remote.gestureTiming(getCookies(), timing);
    }

    public boolean locationTiming(Timing timing, String address,
                                  @Nullable String latitude, @Nullable String longitude) throws IOException {
        return Remote.locationTiming(getCookies(), timing, address, latitude, longitude) != null;
    }
}
