package com.amcglynn.myzappi.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdTest {

    @Test
    void test() {
        var userId = UserId.from("testUserId");
        assertThat(userId).hasToString("testUserId");
        assertThat(userId).isEqualTo(UserId.from("testUserId"));
    }

    @Test
    void testAlexaUserId() {
        var userId = AlexaUserId.from("alexaUserId");
        assertThat(userId).hasToString("alexaUserId");
        assertThat(userId).isEqualTo(AlexaUserId.from("alexaUserId"));
    }
}
