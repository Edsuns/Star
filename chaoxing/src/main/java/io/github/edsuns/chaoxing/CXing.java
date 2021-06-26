package io.github.edsuns.chaoxing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import io.github.edsuns.chaoxing.model.Course;
import io.github.edsuns.chaoxing.model.Timing;

/**
 * Created by Edsuns@qq.com on 2021/6/26.
 */
public class CXing {
    public interface CookieStorage {
        void saveCookies(@NotNull String username, @NotNull Map<String, String> cookies);

        @NotNull Map<String, String> getCookies(@NotNull String username);
    }

    public interface PhotoProvider {
        @NotNull InputStream getPhotoAsInputStream();
    }

    @NotNull
    private final String username;
    @Nullable
    private final String schoolId;
    @NotNull
    private final CookieStorage cookieStorage;

    public CXing(@NotNull String username, @NotNull CookieStorage cookieStorage) {
        this(username, null, cookieStorage);
    }

    public CXing(@NotNull String username, @Nullable String schoolId, @NotNull CookieStorage cookieStorage) {
        this.username = username;
        this.schoolId = schoolId;
        this.cookieStorage = cookieStorage;
    }

    @NotNull
    private Map<String, String> getCookies() {
        return cookieStorage.getCookies(username);
    }

    public boolean login(@NotNull String password) throws IOException {
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

    @NotNull
    public List<Course> getAllCourses() throws IOException {
        return Remote.getAllCourses(getCookies());
    }

    @NotNull
    public List<Timing> getActiveTimingList(@NotNull Course course) throws IOException {
        return Remote.getActiveTimingList(getCookies(), course);
    }

    public boolean normalOrPhotoTiming(@NotNull Timing timing, @NotNull PhotoProvider photoProvider) throws IOException {
        if (Remote.normalTiming(getCookies(), timing)) {
            return true;
        }
        return Remote.photoTiming(getCookies(), timing, photoProvider.getPhotoAsInputStream()) != null;
    }

    public boolean qrcodeTiming(@NotNull Timing timing, @NotNull String enc) throws IOException {
        return Remote.qrcodeTiming(getCookies(), timing, enc) != null;
    }

    public boolean gestureTiming(@NotNull Timing timing) throws IOException {
        return Remote.gestureTiming(getCookies(), timing);
    }

    public boolean locationTiming(@NotNull Timing timing, @NotNull String address,
                                  @Nullable String latitude, @Nullable String longitude) throws IOException {
        return Remote.locationTiming(getCookies(), timing, address, latitude, longitude) != null;
    }
}
