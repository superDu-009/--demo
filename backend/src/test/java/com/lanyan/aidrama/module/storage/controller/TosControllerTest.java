package com.lanyan.aidrama.module.storage.controller;

import com.lanyan.aidrama.module.storage.dto.TosCompleteRequest;
import com.lanyan.aidrama.module.storage.dto.TosPresignRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TosControllerTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void presignRequestShouldRequireCoreFields() {
        TosPresignRequest req = new TosPresignRequest();
        assertEquals(3, validator.validate(req).size());
    }

    @Test
    void completeRequestShouldRequireFileKey() {
        TosCompleteRequest req = new TosCompleteRequest();
        assertEquals(1, validator.validate(req).size());
    }
}
