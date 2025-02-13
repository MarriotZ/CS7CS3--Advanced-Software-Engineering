package com.tcd.asc.damn.common.repository;

import com.tcd.asc.damn.common.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface
RouteRepository extends JpaRepository<Route, Long> {
}
