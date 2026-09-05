package com.uxplima.uxmessentials.worlds.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BuiltInGeneratorsTest {

    @Test
    void recognizesNamespacedBuiltInIdsCaseInsensitively() {
        assertThat(BuiltInGenerators.idOf("ApolloCraftEssentials:void")).contains("void");
        assertThat(BuiltInGenerators.idOf("UXMESSENTIALS:FLAT")).contains("flat");
    }

    @Test
    void rejectsForeignNamespaces() {
        assertThat(BuiltInGenerators.idOf("Multiverse:flat")).isEmpty();
    }

    @Test
    void rejectsBareIdsWithoutOurNamespace() {
        assertThat(BuiltInGenerators.idOf("void")).isEmpty();
        assertThat(BuiltInGenerators.idOf("flat")).isEmpty();
    }

    @Test
    void rejectsUnknownNamespacedIds() {
        assertThat(BuiltInGenerators.idOf("uxmessentials:amplified")).isEmpty();
        assertThat(BuiltInGenerators.idOf("uxmessentials:")).isEmpty();
    }

    @Test
    void refBuildsCanonicalNamespacedRef() {
        assertThat(BuiltInGenerators.ref("void").value()).isEqualTo("ApolloCraftEssentials:void");
        assertThat(BuiltInGenerators.ref("flat").value()).isEqualTo("ApolloCraftEssentials:flat");
    }
    @Test
    void legacyNamespaceRemainsAccepted() {
        assertThat(BuiltInGenerators.idOf("uxmEssentials:void")).contains("void");
        assertThat(BuiltInGenerators.idOf("uxmessentials:flat")).contains("flat");
    }
}
