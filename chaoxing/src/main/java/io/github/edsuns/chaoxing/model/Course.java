package io.github.edsuns.chaoxing.model;

/**
 * Created by Edsuns@qq.com on 2021/6/24.
 */
public class Course {
    public String name;
    public String id;
    public String classId;

    public Course() {
    }

    public Course(String name, String id, String classId) {
        this.name = name;
        this.id = id;
        this.classId = classId;
    }
}
