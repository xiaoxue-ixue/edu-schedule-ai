package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.RoomAvailableTime;
import com.edu.schedule.mapper.RoomAvailableTimeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room-available-time")
public class RoomAvailableTimeController {
    @Autowired
    private RoomAvailableTimeMapper mapper;

    @GetMapping
    public Result<PageResult<RoomAvailableTime>> page(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      @RequestParam(required = false) Long roomId) {
        PageHelper.startPage(page, size);
        PageInfo<RoomAvailableTime> info = new PageInfo<>(mapper.selectList(roomId));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @PostMapping
    public Result<Void> create(@RequestBody RoomAvailableTime e) {
        mapper.insert(e);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }
}
