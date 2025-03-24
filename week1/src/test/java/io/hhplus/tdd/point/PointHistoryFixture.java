package io.hhplus.tdd.point;

import io.hhplus.tdd.TestFixture;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class PointHistoryFixture implements TestFixture<PointHistory> {
    private long id = 1L;
    private long userId = 1L;
    private long amount = 1000L;
    private TransactionType type = TransactionType.CHARGE;
    private long updateMillis = System.currentTimeMillis();

    @Override
    public PointHistory create() {
        return new PointHistory(
            id,
            userId,
            amount,
            type,
            updateMillis
        );
    }
}