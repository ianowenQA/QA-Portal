package com.qa.portal.reflection.persistence.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.qa.portal.common.persistence.entity.TraineeEntity;
import com.qa.portal.common.persistence.entity.TrainerEntity;
import com.qa.portal.reflection.persistence.entity.ReflectionEntity;

@Repository
public interface ReflectionRepository extends JpaRepository<ReflectionEntity, Integer> {

	Set<ReflectionEntity> findAllByResponder(TraineeEntity trainee);

	Set<ReflectionEntity> findByResponderId(Integer id);

	Set<ReflectionEntity> findByReviewerId(Integer id);

	Optional<ReflectionEntity> findByReviewerAndFormDate(TrainerEntity trainer, LocalDate date);

}
