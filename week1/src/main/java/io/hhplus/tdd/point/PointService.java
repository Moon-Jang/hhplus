package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final PointValidator pointValidator;

    public UserPoint get(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> getHistories(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint charge(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);

        pointValidator.validateCharge(userPoint,  amount);
        UserPoint savedPoint = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point() + amount);
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return savedPoint;
    }

    public UserPoint use(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);

        pointValidator.validateUse(userPoint, amount);
        UserPoint savedPoint = userPointTable.insertOrUpdate(userPoint.id(), userPoint.point() - amount);
        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        return savedPoint;
    }
}