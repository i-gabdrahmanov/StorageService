//package com.storage.storageservice.config;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.stereotype.Component;
//
//import java.sql.Driver;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//import java.util.Enumeration;
//
//@Component
//public class IgniteJdbcDestroyer {
//
//    @PostConstruct
//    public void checkDrivers() {
//        Enumeration<Driver> drivers = DriverManager.getDrivers();
//        while (drivers.hasMoreElements()) {
//            Driver driver = drivers.nextElement();
//            System.out.println("Loaded driver: " + driver.getClass().getName());
//        }
//    }
//    @PostConstruct
//    public void cleanDrivers() {
//        Enumeration<Driver> drivers = DriverManager.getDrivers();
//        while (drivers.hasMoreElements()) {
//            Driver driver = drivers.nextElement();
//            if (driver.getClass().getName().contains("ignite")) {
//                try {
//                    DriverManager.deregisterDriver(driver);
//                    try {
//                        Class.forName("org.apache.ignite.IgniteJdbcThinDriver");
//                        System.out.println("ОШИБКА: Драйвер всё ещё доступен!");
//                    } catch (ClassNotFoundException e) {
//                        System.out.println("УСПЕХ: Драйвер отключен");
//                    }
//                } catch (SQLException ignored) {}
//            }
//        }
//    }
//}
