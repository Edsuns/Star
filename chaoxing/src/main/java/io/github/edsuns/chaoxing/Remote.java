package io.github.edsuns.chaoxing;

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
     * Validate login state
     *
     * @param cookies saved cookies
     * @return true if cookies is valid
     * @throws IOException IOException
     */
    static boolean validateLogin(Map<String, String> cookies) throws IOException {
        Connection.Response response =
                Jsoup.connect("http://mooc1-1.chaoxing.com/api/workTestPendingNew")
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
        Document document = Jsoup.connect("http://mooc1-2.chaoxing.com/visit/interaction")
                .cookies(cookies)
                .get();
        Elements courseIds = document.select("input[name=courseId]");
        Elements classIds = document.select("input[name=classId]");
        Elements courseNames = document.select("h3.clearfix>a:first-child");
        if (courseIds.size() != classIds.size() || courseIds.size() != courseNames.size()) {
            throw new RuntimeException("Wrong courses data!");
        }
        ArrayList<Course> courses = new ArrayList<>();
        for (int i = 0; i < courseIds.size(); i++) {
            String courseName = courseNames.get(i).text();
            String courseId = courseIds.get(i).attr("value");
            String classId = classIds.get(0).attr("value");
            courses.add(new Course(courseName, courseId, classId));
        }
        return courses;
    }

    private static String getTimingState(Map<String, String> cookies, Course course, String activeId) throws IOException {
        Document document =
                Jsoup.connect("https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/preSign")
                        .cookies(cookies)
                        .data("courseId", course.id)
                        .data("classId", course.classId)
                        .data("activeId", activeId)
                        .get();
        Elements elements = document.select(".qd_Success span");
        if (elements.size() == 0) {
            return "";
        }
        if (elements.size() > 1) {
            throw new RuntimeException("Wrong selected activeId!");
        }
        return elements.get(0).text();
    }

    /**
     * Fetch active timing list
     *
     * @param cookies cookies
     * @param course  target
     * @return a list of {@link Timing}
     * @throws IOException IOException
     */
    static List<Timing> getActiveTimingList(Map<String, String> cookies, Course course) throws IOException {
        Document document =
                Jsoup.connect("https://mobilelearn.chaoxing.com/widget/pcpick/stu/index")
                        .cookies(cookies)
                        .data("courseId", course.id)
                        .data("jclassId", course.classId)
                        .get();
        Elements elements = document.select("#startList>div>div:first-child");
        List<Timing> activeTimingList = new ArrayList<>();
        for (Element element : elements) {
            Elements taskName = element.select("dl a dd");
            if (taskName.size() != 1 || !"签到".equals(taskName.get(0).text())) {
                continue;
            }
            String onclick = element.attr("onclick");// activeDetail(4000001234567,2,null)
            String activeId = onclick.substring(13, onclick.length() - 8);
            Timing timing = new Timing(course, activeId);
            timing.type = Timing.Type.valueFrom(element.select("div>a:first-child").text());
            timing.state = Timing.State.valueFrom(getTimingState(cookies, course, activeId));
            activeTimingList.add(timing);
        }
        return activeTimingList;
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
                        .data("fid", "39037").get();
        return document.title().contains("签到成功");
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
        String objectId = uploadImage(cookies, inputStream);
        Connection.Response response =
                Jsoup.connect("https://mobilelearn.chaoxing.com/pptSign/stuSignajax")
                        .data("name", "")
                        .data("activeId", timing.activeId)
                        .data("address", "中国")
                        .data("uid", "")
                        .data("fid", "")
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
        Connection.Response response = Jsoup.connect("https://mobilelearn.chaoxing.com/pptSign/stuSignajax")
                .cookies(cookies)
                .data("enc", enc)
                .data("name", timing.course.name)
                .data("activeId", timing.activeId)
                .data("uid", "")
                .data("clientip", "")
                .data("useragent", HttpConnection.DEFAULT_UA)
                .data("latitude", "-1")
                .data("longitude", "-1")
                .data("fid", "")
                .data("appType", "15")
                .execute();
        String text = response.body();
        if (text.contains("success")) {
            return text;// example: {"name": "", "date": "01-01 16:26", "status": "success"}
        }
        return null;
    }

    /**
     * GestureTiming
     *
     * @param cookies cookies
     * @param timing  timing
     * @return true if success
     * @throws IOException IOException
     */
    static boolean gestureTiming(Map<String, String> cookies, Timing timing) throws IOException {
        if (timing.type != Timing.Type.GESTURE) {
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
        Connection.Response response = Jsoup.connect("https://mobilelearn.chaoxing.com/pptSign/stuSignajax")
                .cookies(cookies)
                .data("name", timing.course.name)
                .data("activeId", timing.activeId)
                .data("address", address)
                .data("uid", "")
                .data("clientip", "")
                .data("useragent", HttpConnection.DEFAULT_UA)
                .data("latitude", latitude)
                .data("longitude", longitude)
                .data("fid", "")
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
                .data("file", "", inputStream, "image/webp,image/*")
                .execute();
        JSONObject object = new JSONObject(response.body());
        return object.getString("objectId");
    }
}
