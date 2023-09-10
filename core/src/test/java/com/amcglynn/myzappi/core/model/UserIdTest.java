package com.amcglynn.myzappi.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdTest {

    @Test
    void test() {
        var userId = UserId.from("testUserId");
        assertThat(userId.toString()).isEqualTo("testUserId");
        assertThat(userId).isEqualTo(UserId.from("testUserId"));
    }
}
