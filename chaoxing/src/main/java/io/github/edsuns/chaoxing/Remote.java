package io.github.edsuns.chaoxing;

import com.sun.tools.javac.util.Pair;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.edsuns.chaoxing.model.Course;
import io.github.edsuns.chaoxing.model.Timing;

/**
 * Created by Edsuns@qq.com on 2021/6/23.
 */
final class Remote {
    private Remote() {
    }

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
        Connection.Response response = Jsoup.connect("https://passport2.chaoxing.com/api/login")
                .data("name", username)
                .data("pwd", password)
                .data("schoolid", schoolId)
                .data("verify", String.valueOf(0))
                .execute();
        JSONObject object = new JSONObject(response.body());
        if (response.statusCode() == HttpURLConnection.HTTP_OK && object.getBoolean("result")) {
            return Pair.of(State.LOGIN_SUCCESS, response.cookies());
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
}
