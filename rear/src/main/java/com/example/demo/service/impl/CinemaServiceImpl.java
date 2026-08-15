package com.example.demo.service.impl;

import com.example.demo.entity.Cinema;
import com.example.demo.mapper.CinemaMapper;
import com.example.demo.service.CinemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CinemaServiceImpl implements CinemaService {

    @Autowired
    private CinemaMapper cinemaMapper;

    @Override
    public List<Cinema> getAllCinemas() {
        return cinemaMapper.findByActive();
    }

    @Override
    public Cinema getCinemaById(Long id) {
        return cinemaMapper.findById(id);
    }

    @Override
    public List<Cinema> getCinemasByCity(String city) {
        return cinemaMapper.findByCity(city);
    }
    @Override
    public List<Cinema> getCinemasByName(String name) {
        return cinemaMapper.findByName(name);
    }

    // 搜索影院
    // 【技术说明】根据影院名称搜索影院，返回所有匹配的影院
    // 【技术说明】如果名称为空或为空格，返回所有活跃的影院
    @Override
    public List<Cinema> searchCinemas(String name) {
        if (name != null && !name.trim().isEmpty()) {
            return cinemaMapper.findByName(name.trim());
        }
        return cinemaMapper.findByActive();
    }

    @Override
    public Cinema addCinema(Cinema cinema) {
        cinemaMapper.insert(cinema);
        return cinema;
    }

    @Override
    public void updateCinema(Cinema cinema) {
        cinemaMapper.update(cinema);
    }

    @Override
    public void deleteCinema(Long id) {
        cinemaMapper.deleteById(id);
    }
}
