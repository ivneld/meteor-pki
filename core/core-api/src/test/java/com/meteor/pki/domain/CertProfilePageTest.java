package com.meteor.pki.domain;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CertProfilePageTest {

    @Test
    void pageBlocksGroupFivePages() {
        assertThat(new CertProfilePage(List.of(), 7, 20, 200, 10).blockStart()).isEqualTo(5);
        assertThat(new CertProfilePage(List.of(), 7, 20, 200, 10).blockEnd()).isEqualTo(9);
        assertThat(new CertProfilePage(List.of(), 0, 20, 0, 0).blockEnd()).isEqualTo(-1);
    }

}
