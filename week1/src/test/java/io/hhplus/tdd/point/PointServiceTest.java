package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @InjectMocks
    private PointService pointService;
    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;
    @Mock
    private PointValidator pointValidator;

    @DisplayName("충전 기능 테스트")
    @Nested
    class chargeTest {
        @Test
        void 성공() {
            // given
            long amount = 100L;
            UserPoint userPoint = new UserPointFixture().create();
            UserPoint savedUserPoint = new UserPointFixture()
                .setPoint(userPoint.point() + amount)
                .create();

            given(userPointTable.selectById(userPoint.id()))
                .willReturn(userPoint);
            given(userPointTable.insertOrUpdate(userPoint.id(), userPoint.point() + amount))
                .willReturn(savedUserPoint);

            // when
            Throwable throwable = catchThrowable(() -> pointService.charge(userPoint.id(), amount));

            // then
            assertThat(throwable).isNull();
            verify(pointValidator).validateCharge(userPoint, amount);
            verify(userPointTable).insertOrUpdate(userPoint.id(), userPoint.point() + amount);
            verify(pointHistoryTable).insert(savedUserPoint.id(), amount, TransactionType.CHARGE, savedUserPoint.updateMillis());
        }
    }
}