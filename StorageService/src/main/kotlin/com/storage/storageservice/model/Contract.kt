package com.storage.storageservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity
class Contract() : Document() {

    @Column(nullable = false)
    var contractText: String = ""
}