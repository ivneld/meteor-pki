package com.meteor.pki.api;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CertProfileApiIT extends ApiIT {

    @Test
    void defaultEkusAreRegisteredAndNewOnesCanBeAdded() throws Exception {
        mockMvc.perform(get("/api/extended-key-usages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].oid", hasItem("1.3.6.1.5.5.7.3.9")));

        mockMvc
            .perform(post("/api/extended-key-usages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oid\":\"1.3.6.1.4.1.99999.1\",\"purpose\":\"V2X\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        mockMvc
            .perform(post("/api/extended-key-usages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oid\":\"1.3.6.1.4.1.99999.1\",\"purpose\":\"again\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("PROFILE_INVALID_INPUT"))
            .andExpect(jsonPath("$.detail").value("Extended Key Usage with the same OID already exists."));
    }

    @Test
    void createdProfileIsReadBackByName() throws Exception {
        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content("""
                {"policyName":"api-sub","certType":"SUB","validityPeriod":"P5Y","pathLenConstraint":1,
                 "keyUsages":["DIGITAL_SIGNATURE"],"extendKeyUsage":["1.3.6.1.5.5.7.3.9"],
                 "policyOids":["2.23.140.1.2.1"],"useDefaultAiaCaIssuer":true,
                 "ocspUris":["http://ocsp.example.com"],"keyAlgorithmSpec":"EC-P-384"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("api-sub"));

        mockMvc.perform(get("/api/profiles/detail").param("profileName", "api-sub"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.certType").value("SUB"))
            .andExpect(jsonPath("$.validityPeriod").value("P5Y"))
            .andExpect(jsonPath("$.isCa").value(true))
            .andExpect(jsonPath("$.pathLenConstraint").value(1))
            .andExpect(jsonPath("$.keyUsageCritical").value(true))
            .andExpect(jsonPath("$.keyUsages", hasSize(3)))
            .andExpect(jsonPath("$.extendedKeyUsages[0].purpose").value("OCSP Signing"))
            .andExpect(jsonPath("$.certificateOidsCsv").value("2.23.140.1.2.1"))
            .andExpect(jsonPath("$.keyAlgorithm").value("EC-P-384"))
            .andExpect(jsonPath("$.ocspUris[0]").value("http://127.0.0.1/api/ocsp"))
            .andExpect(jsonPath("$.ocspUris[1]").value("http://ocsp.example.com"))
            .andExpect(jsonPath("$.profileCustomExtensions", hasSize(0)))
            .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get("/api/profiles").param("page", "0").param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.certProfileDetailResponses[*].name", hasItem("api-sub")))
            .andExpect(jsonPath("$.blockStart").value(0));
    }

    @Test
    void profileErrorsUseX509RaCodes() throws Exception {
        mockMvc.perform(get("/api/profiles/detail").param("profileName", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("PROFILE_RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content("""
                {"policyName":"api-bad-key","certType":"LEAF","validityPeriod":"P1Y","keyUsages":[],
                 "keyAlgorithmSpec":"EC_P256"}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("CERT_INVALID_INPUT"))
            .andExpect(jsonPath("$.detail").value("Unsupported key algorithm spec."));

        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content("""
                {"policyName":"api-unknown-eku","certType":"LEAF","validityPeriod":"P1Y","keyUsages":[],
                 "extendKeyUsage":["1.2.3.4.5.6"],"keyAlgorithmSpec":"EC-P-256"}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("PROFILE_INVALID_INPUT"))
            .andExpect(jsonPath("$.detail").value("Unknown EKU OID."));

        mockMvc.perform(get("/api/profiles").param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("COMMON_INVALID_INPUT"));
    }

}
