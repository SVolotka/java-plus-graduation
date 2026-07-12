package ru.practicum.compilation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.compilation.entity.Compilation;

import java.util.List;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    boolean existsByTitle(String title);

    @Query(value = "SELECT c.id FROM compilations c " +
            "WHERE (:pinned IS NULL OR c.pinned = :pinned) " +
            "ORDER BY c.id " +
            "LIMIT :size OFFSET :from", nativeQuery = true)
    List<Long> findCompilationIds(@Param("pinned") Boolean pinned,
                                  @Param("from") int from,
                                  @Param("size") int size);

    @Query("SELECT DISTINCT c FROM Compilation c LEFT JOIN FETCH c.events WHERE c.id IN :ids ORDER BY c.id")
    List<Compilation> findCompilationsWithEventsByIds(@Param("ids") List<Long> ids);
}