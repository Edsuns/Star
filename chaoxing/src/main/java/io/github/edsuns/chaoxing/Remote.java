package io.github.edsuns.chaoxing;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.github.edsuns.chaoxing.model.Course;
import io.github.edsuns.chaoxing.model.Timing;

/**
 * Created by Edsuns@qq.com on 2021/6/23.
 */
final class Remote {
    private Remote() {
    }

    /**
     * Login
     *
     * @param username phone number or schoolId
     * @param password password
     * @param schoolId can be empty if login with phone number
     * @return cookies, null if login failed
     * @throws IOException IOException
     */
    @Nullable
    static Map<String, String> login(String username, String password, String schoolId) throws IOException {
        Connection.Response response = Jsoup.connect("https://passport2.chaoxing.com/api/login")
                .data("name", username)
                .data("pwd", password)
                .data("schoolid", schoolId)
                .data("verify", "0")
                .execute();
        JSONObject object = new JSONObject(response.body());
        if (response.statusCode() == HttpURLConnection.HTTP_OK && object.getBoolean("result")) {
            return response.cookies();
        }
        return null;
    }

    /**
     * getName
     *
     * @param cookies saved cookies
     * @return name, null if get failed
     * @throws IOException IOException
     */
    static String getName(Map<String, String> cookies) throws IOException {
        Document document = Jsoup.connect("https://i.mooc.chaoxing.com/space/index")
                .cookies(cookies)
                .followRedirects(false)
                .get();
        Elements elements = document.select("#space_nickname > p");
        if (elements.size() == 1) {
            return elements.get(0).text();
        }
        return null;
    }

    /**
     * Validate login state
     *
     * @param cookies saved cookies
     * @return true if cookies is valid
     * @throws IOException IOException
     */
    static boolean validateLogin(Map<String, String> cookies) throws IOException {
        // protocol of the url must be https, which can avoid network portal redirection
        Connection.Response response =
                Jsoup.connect("https://mooc1-1.chaoxing.com/api/workTestPendingNew")
                        .cookies(cookies)
                        .followRedirects(false)// if cookies is invalid, it will redirect to login page
                        .execute();
        return response.statusCode() == HttpURLConnection.HTTP_OK;
    }

    /**
     * Fetch all courses
     *
     * @param cookies cookies
     * @return a list of {@link Course}
     * @throws IOException IOException
     */
    static List<Course> getAllCourses(Map<String, String> cookies) throws IOException {
        Document document = Jsoup.connect("https://mooc1-2.chaoxing.com/visit/courselistdata")
                .cookies(cookies)
                .data("courseType", "1")
                .data("courseFolderId", "0")
                .data("courseFolderSize", "0")
                .post();
        Elements courseElements = document.select(".course-list .course");
        ArrayList<Course> courses = new ArrayList<>();
        for (int i = 0; i < courseElements.size(); i++) {
            Element element = courseElements.get(i);
            Elements nameElements = element.select(".course-info .course-name");
            if (nameElements.size() != 1) {
                throw new RuntimeException("Wrong courses data!");
            }
            final String courseName = nameElements.text();
            String courseId = element.attr("courseid");
            String classId = element.attr("clazzid");
            courses.add(new Course(courseName, courseId, classId));
        }
        return courses;
    }

    private static Timing.State getTimingState(Map<String, String> cookies, Course course, String activeId) throws IOException {
        Document document =
                Jsoup.connect("https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/preSign")
                        .cookies(cookies)
                        .data("courseId", course.id)
                        .data("classId", course.classId)
                        .data("activeId", activeId)
                        .get();
        Elements elements = document.select(".qd_Success dl span");
        if (elements.size() == 0) {
            return Timing.State.UNSIGNED;
        }
        if (elements.size() > 1) {
            throw new RuntimeException("Wrong selected activeId!");
        }
        return Timing.State.valueFrom(elements.get(0).text());
    }

    /**
     * Fetch active timing list
     *
     * @param cookies       cookies
     * @param course        target
     * @param includesEnded true to include ended timings
     * @return a list of {@link Timing}
     * @throws IOException IOException
     */
    static List<Timing> getActiveTimingList(Map<String, String> cookies,
                                            Course course, boolean includesEnded) throws IOException {
        String json =
                Jsoup.connect("https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist")
                        .ignoreContentType(true)
                        .cookies(cookies)
                        .data("showNotStartedActive", "0")
                        .data("activeType", "2")// activeType == 2 是签到活动
                        .data("courseId", course.id)
                        .data("classId", course.classId)
                        .data("fid", "0")
                        .get()
                        .text();
        JSONArray arr = new JSONObject(json).getJSONObject("data").getJSONArray("activeList");
        List<Timing> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject a = arr.getJSONObject(i);
            // 跳过非签到活动
            if (a.getInt("type") != 2) continue;
            // status == 1 是进行中的活动
            if (!includesEnded && a.getInt("status") != 1) continue;
            String activeId = a.getString("id");
            Timing timing = new Timing(course, activeId);
            timing.state = getTimingState(cookies, course, activeId);
            timing.type = Timing.Type.valueFrom(a.getString("nameOne"));
            timing.time = a.getLong("startTime");
            result.add(timing);
        }
        return result;
    }

    /**
     * NormalTiming
     *
     * @param cookies cookies
     * @param timing  timing
     * @return true if do timing successfully
     * @throws IOException IOException
     */
    static boolean normalTiming(Map<String, String> cookies, Timing timing) throws IOException {
        if (timing.type != Timing.Type.NORMAL_OR_PHOTO) {
            throw new IllegalArgumentException("Mismatched Timing!");
        }
        Document document =
                Jsoup.connect("https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/preSign")
                        .cookies(cookies)
                        .data("courseId", timing.course.id)
                        .data("classId", timing.course.classId)
                        .data("activeId", timing.activeId)
                        .data("fid", cookies.get("fid"))
                        .get();
        return document.title().contains("签到成功");
    }

    /**
     * GestureOrCodeTiming
     *
     * @param cookies cookies
     * @param timing  timing
     * @return true if success
     * @throws IOException IOException
     */
    static boolean gestureOrCodeTiming(Map<String, String> cookies, Timing timing) throws IOException {
        if (timing.type != Timing.Type.GESTURE && timing.type != Timing.Type.CODE) {
            throw new IllegalArgumentException("Mismatched Timing!");
        }
        Document document =
                Jsoup.connect("https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/signIn")
                        .cookies(cookies)
                        .data("courseId", timing.course.id)
                        .data("classId", timing.course.classId)
                        .data("activeId", timing.activeId)
                        .get();
        return document.title().contains("签到成功");
    }

    private static void preSign(Map<String, String> cookies, Timing timing) throws IOException {
        Connection.Response resp = Jsoup.connect("https://mobilelearn.chaoxing.com/newsign/preSign")
                .cookies(cookies)
                .data("activePrimaryId", timing.activeId)
                .data("general", "1")
                .data("sys", "1")
                .data("ls", "1")
                .data("appType", "15")
                .data("ut", "s")
                .execute();
        if (resp.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("pre-sign failed");
        }
    }

    /**
     * PhotoTiming
     *
     * @param cookies     cookies
     * @param timing      timing
     * @param inputStream inputStream of the photo
     * @return null if failed to do timing
     * @throws IOException IOException
     */
    @Nullable
    static String photoTiming(Map<String, String> cookies, Timing timing, InputStream inputStream) throws IOException {
        preSign(cookies, timing);
        String objectId = uploadImage(cookies, inputStream);
        String name = getName(cookies);
        Connection.Response response =
                Jsoup.connect("https://mobilelearn.chaoxing.com/pptSign/stuSignajax")
                        .cookies(cookies)
                        .data("name", name != null ? name : timing.course.name)
                        .data("activeId", timing.activeId)
                        .data("address", "中国")
                        .data("uid", cookies.get("UID"))
                        .data("fid", cookies.get("fid"))
                        .data("appType", "15")
                        .data("ifTiJiao", "1")
                        .data("objectId", objectId)
                        .execute();
        String text = response.body();
        if (text.contains("success")) {
            return text;// example: {"name": "", "date": "01-01 16:26", "status": "success"}
        }
        return null;
    }

    /**
     * QRCodeTiming
     *
     * @param cookies cookies
     * @param timing  timing
     * @param enc     extracted from a timing QRCode which is allowed to be expired
     * @return null if failed to do timing
     * @throws IOException IOException
     */
    @Nullable
    static String qrcodeTiming(Map<String, String> cookies, Timing timing, String enc) throws IOException {
        if (timing.type != Timing.Type.QRCODE) {
            throw new IllegalArgumentException("Mismatched Timing!");
        }
        preSign(cookies, timing);
        String name = getName(cookies);
        Connection.Response response = Jsoup.connect("https://mobilelearn.chaoxing.com/pptSign/stuSignajax")
                .cookies(cookies)
                .data("enc", enc)
                .data("name", name != null ? name : timing.course.name)
                .data("activeId", timing.activeId)
                .data("uid", cookies.get("UID"))
                .data("clientip", "")
                .data("useragent", HttpConnection.DEFAULT_UA)
                .data("latitude", "-1")
                .data("longitude", "-1")
                .data("fid", cookies.get("fid"))
                .data("appType", "15")
                .execute();
        String text = response.body();
        if (text.contains("success") || text.contains("已签到")) {
            return text;// example: {"name": "", "date": "01-01 16:26", "status": "success"}
        }
        return null;
    }

    /**
     * LocationTiming
     *
     * @param cookies   cookies
     * @param timing    timing
     * @param address   address name displayed on the screen
     * @param latitude  latitude
     * @param longitude longitude
     * @return null if failed to do timing
     * @throws IOException IOException
     */
    @Nullable
    static String locationTiming(Map<String, String> cookies, Timing timing, String address,
                                 @Nullable String latitude, @Nullable String longitude) throws IOException {
        if (timing.type != Timing.Type.LOCATION) {
            throw new IllegalArgumentException("Mismatched Timing!");
        }
        if (latitude == null) {
            latitude = "-1";
        }
        if (longitude == null) {
            longitude = "-1";
        }
        preSign(cookies, timing);
        String name = getName(cookies);
        Connection.Response response = Jsoup.connect("https://mobilelearn.chaoxing.com/pptSign/stuSignajax")
                .cookies(cookies)
                .data("name", name != null ? name : timing.course.name)
                .data("activeId", timing.activeId)
                .data("address", address)
                .data("uid", cookies.get("UID"))
                .data("clientip", "")
                .data("useragent", HttpConnection.DEFAULT_UA)
                .data("latitude", latitude)
                .data("longitude", longitude)
                .data("fid", cookies.get("fid"))
                .data("appType", "15")
                .data("ifTiJiao", "1")
                .execute();
        String text = response.body();
        if (text.contains("success")) {
            return text;// example: {"name": "", "date": "01-01 16:26", "status": "success"}
        }
        return null;
    }

    /**
     * Get the token for uploading
     *
     * @param cookies cookies
     * @return token
     * @throws IOException IOException
     */
    @Nullable
    private static String getToken(Map<String, String> cookies) throws IOException {
        Connection.Response response =
                Jsoup.connect("https://pan-yz.chaoxing.com/api/token/uservalid")
                        .cookies(cookies)
                        .execute();
        JSONObject object = new JSONObject(response.body());
        if (object.getBoolean("result")) {
            return object.getString("_token");
        }
        return null;
    }

    /**
     * Upload an image
     *
     * @param cookies     cookies
     * @param inputStream inputStream of the image
     * @return objectId
     * @throws IOException IOException
     */
    private static String uploadImage(Map<String, String> cookies, InputStream inputStream) throws IOException {
        final String token = getToken(cookies);
        final String uid = cookies.get("UID");
        Connection.Response response = Jsoup.connect("https://pan-yz.chaoxing.com/upload")
                .method(Connection.Method.POST)
                .cookies(cookies)
                .data("puid", uid)
                .data("_token", token)
                .data("file", "photo.jpg", inputStream, "application/octet-stream")
                .execute();
        JSONObject object = new JSONObject(response.body());
        if (object.getBoolean("result")) {
            return object.getString("objectId");
        }
        throw new IOException(response.body());
    }
}
