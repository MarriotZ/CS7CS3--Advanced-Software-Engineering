package com.tcd.asc.damn.common.repository;

import com.tcd.asc.damn.common.entity.DublinBikeStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DublinBikeStationRepository extends JpaRepository<DublinBikeStation, Integer> {
}