package com.example.demo.service;

import com.example.demo.entity.Hall;

import java.util.List;

public interface HallService {
    List<Hall> getAllHalls(Long cinemaId);
    Hall getHallById(Long id);
    Hall addHall(Hall hall);
    void updateHall(Long id, Hall hall);
    void updateHallStatus(Long id, String status);
    void deleteHall(Long id);
}
