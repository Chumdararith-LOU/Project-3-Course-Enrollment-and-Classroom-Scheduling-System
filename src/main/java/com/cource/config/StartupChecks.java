package com.cource.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartupChecks implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);

    private final JdbcTemplate jdbcTemplate;

    public StartupChecks(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            Integer offerings = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_offerings", Integer.class);
            log.info("[STARTUP CHECK] course_offerings count = {}", offerings);
        } catch (Exception ex) {
            log.warn("[STARTUP CHECK] Unable to query course_offerings: {}", ex.getMessage());
        }

        try {
            Integer cl = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_lecturers", Integer.class);
            log.info("[STARTUP CHECK] course_lecturers count = {}", cl);
        } catch (Exception ex) {
            log.warn("[STARTUP CHECK] course_lecturers missing or inaccessible: {}", ex.getMessage());
            log.warn(
                    "[STARTUP CHECK] If missing, run src/main/resources/db/fix/create_course_lecturers.sql and then assign_lecturer_bulk.sql or run Flyway migrations.");
        }
    }
}
