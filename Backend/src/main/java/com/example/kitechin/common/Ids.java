package com.example.kitechin.common;

import com.example.kitechin.exception.ApiException;
import org.bson.types.ObjectId;

public final class Ids {

    private Ids() {
    }

    public static ObjectId parse(String value, String label) {
        if (value == null || !ObjectId.isValid(value)) {
            throw ApiException.badRequest("Invalid " + label);
        }
        return new ObjectId(value);
    }
}
