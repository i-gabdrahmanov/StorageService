package com.storage.storageservice.controller;

import com.storage.storageservice.service.impl.CacheService;
import lombok.RequiredArgsConstructor;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("api/v2/primaryCache")
@RequiredArgsConstructor
public class PrimaryCacheController {

    private static final String TYPE = "Primary";
    private final CacheService primaryCacheService;

   @GetMapping
    public String get(@RequestParam String key) {
       try {
           return (String) primaryCacheService.get(key) + " " + TYPE;
       } catch (InterruptedException | TimeoutException e) {
           throw new RuntimeException(e);
       } catch (MemcachedException e) {
          return "EMPTY OR BROKEN";
       }
   }

   @PostMapping("{key}/{value}/add")
    public void add(@PathVariable String key, @PathVariable String value) {
       try {
           primaryCacheService.set(key, value);
       } catch (InterruptedException | TimeoutException | MemcachedException e) {
           throw new RuntimeException(e);
       }
   }
}
