package ru.practicum.repository;

import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EndpointHit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT e.app as app, e.uri as uri, COUNT(e) as hits " +
            "FROM EndpointHit e " +
            "WHERE e.created BETWEEN :start AND :end " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(e) DESC")
    List<Tuple> findStats(@Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    @Query("SELECT e.app as app, e.uri as uri, COUNT(e) as hits " +
            "FROM EndpointHit e " +
            "WHERE e.created BETWEEN :start AND :end " +
            "AND e.uri IN :uris " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(e) DESC")
    List<Tuple> findStatsByUris(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("uris") List<String> uris);

    @Query("SELECT e.app as app, e.uri as uri, COUNT(DISTINCT e.ip) as hits " +
            "FROM EndpointHit e " +
            "WHERE e.created BETWEEN :start AND :end " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC")
    List<Tuple> findUniqueStats(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    @Query("SELECT e.app as app, e.uri as uri, COUNT(DISTINCT e.ip) as hits " +
            "FROM EndpointHit e " +
            "WHERE e.created BETWEEN :start AND :end " +
            "AND e.uri IN :uris " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC")
    List<Tuple> findUniqueStatsByUris(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("uris") List<String> uris);
}