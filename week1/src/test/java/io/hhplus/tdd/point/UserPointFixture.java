package io.hhplus.tdd.point;

import io.hhplus.tdd.TestFixture;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class UserPointFixture implements TestFixture<UserPoint> {
    private long id = 1L;
    private long point = 200;
    private long updateMillis = System.currentTimeMillis();

    public UserPoint create() {
        return new UserPoint(
            id,
            point,
            updateMillis
        );
    }
}