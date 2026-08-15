package com.example.demo.service;

import com.example.demo.entity.Seat;

import java.util.List;
import java.util.Map;

public interface SeatService {
    List<Seat> getAllSeats();
    Seat getSeatById(Long id);
    List<Seat> getSeatsByHallId(Long hallId);
    void batchAddSeats(Long hallId, int rows, int cols, int startRow, int startCol);
    List<Map<String, Object>> getSeatsByScheduleId(Long scheduleId);
    void batchUpdateStatus(List<Long> seatIds);
}
