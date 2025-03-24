package io.hhplus.tdd;

public interface TestFixture<T> {
    T create();
}