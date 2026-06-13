package com.amcglynn.myzappi.core.dal;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.ByteBuffer;

public final class DynamoDbAttributeValues {

    private DynamoDbAttributeValues() {
    }

    public static AttributeValue stringValue(String value) {
        return AttributeValue.builder().s(value).build();
    }

    public static AttributeValue numberValue(long value) {
        return numberValue(String.valueOf(value));
    }

    public static AttributeValue numberValue(String value) {
        return AttributeValue.builder().n(value).build();
    }

    public static AttributeValue binaryValue(ByteBuffer value) {
        return AttributeValue.builder().b(SdkBytes.fromByteBuffer(value)).build();
    }

    public static ByteBuffer byteBufferValue(AttributeValue value) {
        return value.b().asByteBuffer();
    }
}
