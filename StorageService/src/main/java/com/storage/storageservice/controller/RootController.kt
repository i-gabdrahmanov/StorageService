package com.storage.storageservice.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class RootController {

    @RequestMapping("/")
    fun root(): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }
}