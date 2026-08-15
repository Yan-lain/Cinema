package com.example.demo.service.impl;

import com.example.demo.entity.Seat;
import com.example.demo.entity.Schedule;
import com.example.demo.mapper.ScheduleMapper;
import com.example.demo.mapper.ScheduleSeatMapper;
import com.example.demo.mapper.SeatMapper;
import com.example.demo.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 座位服务实现类
 * 【技术说明】用于实现座位服务接口
 * 【功能说明】提供座位相关的业务逻辑实现
 * 【依赖说明】依赖SeatMapper，用于数据库操作
 * 【接口说明】提供座位相关的接口，如查询所有座位、根据ID查询座位、根据影院ID查询座位等
 * 【返回值说明】返回JSON格式的响应体，包含状态码、消息、数据等
 * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
 * 【异常说明】处理可能的异常情况，如座位不存在等
 * */
@Service
public class SeatServiceImpl implements SeatService {

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private ScheduleSeatMapper scheduleSeatMapper;

    @Override
    public List<Seat> getAllSeats() {
        return seatMapper.findAll();
    }

    @Override
    public Seat getSeatById(Long id) {
        return seatMapper.findById(id);
    }

    @Override
    public List<Seat> getSeatsByHallId(Long hallId) {
        return seatMapper.findByHallId(hallId);
    }

    /**
     * 批量添加座位
     * 【技术说明】用于批量添加座位
     * 【功能说明】根据请求体中的参数，批量添加指定影院的座位
     * 【依赖说明】依赖SeatMapper，用于数据库操作
     * 【接口说明】提供POST方法，用于处理批量添加座位请求
     * 【返回值说明】返回JSON格式的响应体，包含状态码、消息、数据等
     * 【参数说明】根据请求体不同，参数不同，具体请参考接口文档
     * 【异常说明】处理可能的异常情况，如影院不存在等
     * */
    @Override
    public void batchAddSeats(Long hallId, int rows, int cols, int startRow, int startCol) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Seat seat = new Seat();
                seat.setHallId(hallId);
                seat.setRowNum(startRow + i);
                seat.setColNum(startCol + j);
                seat.setSeatNumber((startRow + i) + "-" + (startCol + j));
                seatMapper.insert(seat);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getSeatsByScheduleId(Long scheduleId) {
        Schedule schedule = scheduleMapper.findById(scheduleId);
        if (schedule == null) {
            throw new RuntimeException("场次不存在");
        }

        List<Seat> seats = seatMapper.findByHallId(schedule.getHallId());
        List<Long> unavailableSeatIds = scheduleSeatMapper.findUnavailableSeatIdsByScheduleId(scheduleId);

        List<Map<String, Object>> seatList = new ArrayList<>();
        for (Seat seat : seats) {
            Map<String, Object> seatMap = new HashMap<>();
            seatMap.put("id", seat.getId());
            seatMap.put("hallId", seat.getHallId());
            seatMap.put("row", seat.getRowNum());
            seatMap.put("col", seat.getColNum());
            seatMap.put("seatNumber", seat.getSeatNumber());
            seatMap.put("status", unavailableSeatIds.contains(seat.getId()) ? "sold" : "available");
            seatList.add(seatMap);
        }
        return seatList;
    }

    @Override
    public void batchUpdateStatus(List<Long> seatIds) {
        for (Long seatId : seatIds) {
            Seat seat = seatMapper.findById(seatId);
            if (seat != null) {
                seatMapper.update(seat);
            }
        }
    }
}
