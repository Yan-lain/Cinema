package com.example.demo.service.impl;

import com.example.demo.entity.Hall;
import com.example.demo.mapper.HallMapper;
import com.example.demo.service.HallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HallServiceImpl implements HallService {

    @Autowired
    private HallMapper hallMapper;

    @Override
    public List<Hall> getAllHalls(Long cinemaId) {
        if (cinemaId != null) {
            return hallMapper.findByCinemaId(cinemaId);
        }
        return hallMapper.findAll();
    }

    @Override
    public Hall getHallById(Long id) {
        return hallMapper.findById(id);
    }

    @Override
    public Hall addHall(Hall hall) {
        if (hall.getStatus() == null) {
            hall.setStatus("active");
        }
        hallMapper.insert(hall);
        return hall;
    }

    @Override
    public void updateHall(Long id, Hall hall) {
        hall.setId(id);
        hallMapper.update(hall);
    }

    @Override
    public void updateHallStatus(Long id, String status) {
        Hall hall = hallMapper.findById(id);
        if (hall == null) {
            throw new RuntimeException("放映厅不存在");
        }
        hall.setStatus(status);
        hallMapper.update(hall);
    }

    @Override
    public void deleteHall(Long id) {
        Hall hall = hallMapper.findById(id);
        if (hall == null) {
            throw new RuntimeException("放映厅不存在");
        }
        hallMapper.deleteById(id);
    }
}
