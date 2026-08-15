package com.example.demo.service;

import com.example.demo.entity.Cinema;

import java.util.List;

public interface CinemaService {
    List<Cinema> getAllCinemas();
    Cinema getCinemaById(Long id);
    List<Cinema> getCinemasByCity(String city);
    List<Cinema> getCinemasByName(String name);
    List<Cinema> searchCinemas(String name);
    Cinema addCinema(Cinema cinema);
    void updateCinema(Cinema cinema);
    void deleteCinema(Long id);
}
