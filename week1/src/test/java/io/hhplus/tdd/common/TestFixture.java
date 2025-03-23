package io.hhplus.tdd.common;

public interface TestFixture<T> {
    T create();
}