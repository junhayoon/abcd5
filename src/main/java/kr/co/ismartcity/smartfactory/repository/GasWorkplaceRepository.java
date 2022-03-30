package kr.co.ismartcity.smartfactory.repository;

import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import kr.co.ismartcity.smartfactory.entity.GasSensorWorkplace;


public interface GasWorkplaceRepository
extends CrudRepository<GasSensorWorkplace, Long>, QuerydslPredicateExecutor<GasSensorWorkplace> {

	GasSensorWorkplace save(GasSensorWorkplace iGasSensorWorkplace);

}
