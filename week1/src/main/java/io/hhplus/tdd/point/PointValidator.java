package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

import static io.hhplus.tdd.point.UserPoint.MIN_CHARGE_AMOUNT;

@Component
public class PointValidator {

    public void validateCharge(UserPoint userPoint, long amount) {
        if (amount > UserPoint.MAX_CHARGE_AMOUNT) {
            String errorMessage = "1회 충전 가능한 최대 포인트는 %d 입니다. 충전하시려는 포인트: %d"
                .formatted(UserPoint.MAX_CHARGE_AMOUNT, amount);
            throw new IllegalArgumentException(errorMessage);
        }

        if (amount < MIN_CHARGE_AMOUNT) {
            String errorMessage = "1회 충전 가능한 최소 포인트는 %d 입니다. 충전하시려는 포인트: %d"
                .formatted(MIN_CHARGE_AMOUNT, amount);
            throw new IllegalArgumentException(errorMessage);
        }

        var willChargePoint = userPoint.point() + amount;

        if (willChargePoint < UserPoint.MIN_BALANCE) {
            String errorMessage = "포인트 잔액은 %d 보다 작을 수 없습니다. 현재 포인트: %d, 충전 후 포인트: %d"
                .formatted(UserPoint.MIN_BALANCE, userPoint.point(), willChargePoint);
            throw new IllegalArgumentException(errorMessage);
        }

        if (willChargePoint > UserPoint.MAX_BALANCE) {
            String errorMessage = "포인트 잔액은 %d 보다 클 수 없습니다. 현재 포인트: %d, 충전 후 포인트: %d"
                .formatted(UserPoint.MAX_BALANCE, userPoint.point(), willChargePoint);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public void validateUse(UserPoint userPoint, long amount) {
        if (amount < UserPoint.MIN_USE_AMOUNT) {
            String errorMessage = "사용 포인트는 %d 보다 작을 수 없습니다. 사용하시려는 포인트: %d"
                .formatted(UserPoint.MIN_USE_AMOUNT, amount);
            throw new IllegalArgumentException(errorMessage);
        }

        if (userPoint.point() < amount) {
            String errorMessage = "포인트가 부족합니다. 현재 포인트: %d, 사용하려는 포인트: %d"
                .formatted(userPoint.point(), amount);
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
