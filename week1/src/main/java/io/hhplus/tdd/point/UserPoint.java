package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static final int MIN_BALANCE = 0;
    public static final int MAX_BALANCE = 1_000_000;
    public static final int MIN_CHARGE_AMOUNT = 1;
    public static final int MAX_CHARGE_AMOUNT = 100_000;
    public static final int MIN_USE_AMOUNT = 1;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
}
