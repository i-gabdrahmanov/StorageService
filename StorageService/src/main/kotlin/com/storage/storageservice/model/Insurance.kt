package com.storage.storageservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity
class Insurance () : Document() {

    @Column(nullable = false)
    var vehicleType:String = ""
}