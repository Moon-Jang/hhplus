package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
public class PointControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PointService pointService;

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
}
