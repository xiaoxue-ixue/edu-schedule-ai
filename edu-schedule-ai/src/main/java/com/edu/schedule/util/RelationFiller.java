package com.edu.schedule.util;

import com.edu.schedule.entity.*;
import com.edu.schedule.mapper.CampusMapper;
import com.edu.schedule.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 关联字段回填工具：把 campusId -> campusName、teacherId -> teacherName
 * 在 Controller 返回前端之前调用，避免前端显示空白的 ID。
 */
@Component
public class RelationFiller {

    @Autowired private CampusMapper campusMapper;
    @Autowired private TeacherMapper teacherMapper;

    public void fillRooms(List<Room> list) {
        if (list == null || list.isEmpty()) return;
        Map<Long, String> campus = loadCampusMap();
        for (Room r : list) if (r.getCampusId() != null) r.setCampusName(campus.get(r.getCampusId()));
    }

    public void fillTeachers(List<Teacher> list) {
        if (list == null || list.isEmpty()) return;
        Map<Long, String> campus = loadCampusMap();
        for (Teacher t : list) if (t.getCampusId() != null) t.setCampusName(campus.get(t.getCampusId()));
    }

    public void fillClazz(List<Clazz> list) {
        if (list == null || list.isEmpty()) return;
        Map<Long, String> campus = loadCampusMap();
        for (Clazz c : list) if (c.getCampusId() != null) c.setCampusName(campus.get(c.getCampusId()));
    }

    public void fillCourses(List<Course> list) {
        if (list == null || list.isEmpty()) return;
        Map<Long, String> teacher = loadTeacherMap();
        for (Course c : list) if (c.getTeacherId() != null) c.setTeacherName(teacher.get(c.getTeacherId()));
    }

    public void fillOne(Course c) { if (c != null) fillCourses(List.of(c)); }
    public void fillOne(Room r)   { if (r != null) fillRooms(List.of(r)); }
    public void fillOne(Teacher t){ if (t != null) fillTeachers(List.of(t)); }
    public void fillOne(Clazz c)  { if (c != null) fillClazz(List.of(c)); }

    private Map<Long, String> loadCampusMap() {
        Map<Long, String> map = new HashMap<>();
        for (Campus c : campusMapper.selectList(null)) map.put(c.getId(), c.getCampusName());
        return map;
    }

    private Map<Long, String> loadTeacherMap() {
        Map<Long, String> map = new HashMap<>();
        for (Teacher t : teacherMapper.selectList(null)) map.put(t.getId(), t.getName());
        return map;
    }
}
