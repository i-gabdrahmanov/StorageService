//package com.storage.storageservice.config;
//
//import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.sql.Driver;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//import java.util.Enumeration;
//
//@Configuration
//public class EarlyJdbcCleanupConfig {
//
//    @Bean
//    public static BeanFactoryPostProcessor earlyJdbcCleanup() {
//        return beanFactory -> {
//            // Выполняется ДО инициализации любых бинов
//            cleanupIgniteJdbcDriver();
//        };
//    }
//
//    private static void cleanupIgniteJdbcDriver() {
//        try {
//            Enumeration<Driver> drivers = DriverManager.getDrivers();
//            while (drivers.hasMoreElements()) {
//                Driver driver = drivers.nextElement();
//                if (driver.getClass().getName().contains("ignite")) {
//                    try {
//                        DriverManager.deregisterDriver(driver);
//                        System.out.println("SUCCESS: Ignite JDBC driver deregistered");
//                    } catch (SQLException e) {
//                        System.err.println("ERROR deregistering driver: " + e.getMessage());
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("ERROR during driver cleanup: " + e.getMessage());
//        }
//    }
//}
