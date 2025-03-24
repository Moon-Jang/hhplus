package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
public class PointControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PointService pointService;

    @DisplayName("포인트 조회 테스트")
    @Test
    public void point() throws Exception {
        // given
        UserPoint userPoint = new UserPointFixture().create();
        given(pointService.getPoint(userPoint.id())).willReturn(userPoint);

        // when & then
        mockMvc.perform(
                get("/point/{id}", userPoint.id())
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userPoint.id()))
            .andExpect(jsonPath("$.point").value(userPoint.point()))
            .andExpect(jsonPath("$.updateMillis").value(userPoint.updateMillis()));
    }

    @DisplayName("포인트 이력 조회 테스트")
    @Test
    public void getHistories() throws Exception {
        // given
        UserPoint userPoint = new UserPointFixture().create();
        PointHistory pointHistory = new PointHistoryFixture().create();
        given(pointService.getHistories(userPoint.id())).willReturn(List.of(pointHistory));

        // when & then
        mockMvc.perform(
                get("/point/{id}/histories", userPoint.id())
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(pointHistory.id()))
            .andExpect(jsonPath("$[0].userId").value(pointHistory.userId()))
            .andExpect(jsonPath("$[0].amount").value(pointHistory.amount()))
            .andExpect(jsonPath("$[0].type").value(pointHistory.type().name()))
            .andExpect(jsonPath("$[0].updateMillis").value(pointHistory.updateMillis()));
    }

    @DisplayName("포인트 충전 테스트")
    @Test
    public void charge() throws Exception {
        // given
        UserPoint userPoint = new UserPointFixture().create();
        long amount = 100L;
        given(pointService.charge(userPoint.id(), amount)).willReturn(userPoint);

        // when & then
        mockMvc.perform(
                patch("/point/{id}/charge", userPoint.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.valueOf(amount))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userPoint.id()))
            .andExpect(jsonPath("$.point").value(userPoint.point()))
            .andExpect(jsonPath("$.updateMillis").value(userPoint.updateMillis()));
    }

    @DisplayName("포인트 사용 테스트")
    @Test
    public void use() throws Exception {
        // given
        UserPoint userPoint = new UserPointFixture().create();
        long amount = 100L;
        given(pointService.use(userPoint.id(), amount)).willReturn(userPoint);

        // when & then
        mockMvc.perform(
                patch("/point/{id}/use", userPoint.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.valueOf(amount))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userPoint.id()))
            .andExpect(jsonPath("$.point").value(userPoint.point()))
            .andExpect(jsonPath("$.updateMillis").value(userPoint.updateMillis()));
    }
}
