package io.github.edsuns.chaoxing.model;

/**
 * Created by Edsuns@qq.com on 2021/6/24.
 */
public class Timing {
    public enum Type {
        UNKNOWN("error"), NORMAL_OR_PHOTO("签到"), QRCODE("二维码签到"), LOCATION("位置签到"), GESTURE("手势签到");

        public final String description;

        Type(String description) {
            this.description = description;
        }

        public static Type valueFrom(String description) {
            if (NORMAL_OR_PHOTO.description.equals(description)) {
                return NORMAL_OR_PHOTO;
            }
            if (QRCODE.description.equals(description)) {
                return QRCODE;
            }
            if (LOCATION.description.equals(description)) {
                return LOCATION;
            }
            if (GESTURE.description.equals(description)) {
                return GESTURE;
            }
            return UNKNOWN;
        }
    }

    public enum State {
        UNKNOWN("error"), UNSIGNED(""), SUCCESS("签到成功"), TIMEOUT("签到已过期");

        final String description;

        State(String description) {
            this.description = description;
        }

        public static State valueFrom(String description) {
            if (SUCCESS.description.equals(description)) {
                return SUCCESS;
            }
            if (TIMEOUT.description.equals(description)) {
                return TIMEOUT;
            }
            if (UNSIGNED.description.equals(description)) {
                return UNSIGNED;
            }
            return UNKNOWN;
        }
    }

    public Course course;
    public String activeId;
    public Type type;
    public State state;

    public Timing(Course course, String activeId) {
        this.course = course;
        this.activeId = activeId;
    }
}
