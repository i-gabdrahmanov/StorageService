package com.storage.storageservice.model;

interface IEntity<T> {

    void setId(T id);

    T getId();
}