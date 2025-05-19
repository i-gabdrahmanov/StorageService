package com.storage.storageservice.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(nullable = false)
    lateinit var name: String

    @Column(nullable = false)
    lateinit var surname: String

    @Column(nullable = false)
    var createDateTime: LocalDateTime = LocalDateTime.now()
}