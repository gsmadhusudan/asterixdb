/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.runtime.evaluators.functions;

import java.io.DataOutput;

import org.apache.asterix.dataflow.data.nontagged.serde.ADoubleSerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.AFloatSerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.AInt16SerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.AInt32SerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.AInt64SerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.AInt8SerializerDeserializer;
import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.om.base.AMutableDouble;
import org.apache.asterix.om.base.AMutableFloat;
import org.apache.asterix.om.base.AMutableInt16;
import org.apache.asterix.om.base.AMutableInt32;
import org.apache.asterix.om.base.AMutableInt64;
import org.apache.asterix.om.base.AMutableInt8;
import org.apache.asterix.om.functions.AsterixBuiltinFunctions;
import org.apache.asterix.om.functions.IFunctionDescriptor;
import org.apache.asterix.om.functions.IFunctionDescriptorFactory;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.om.types.EnumDeserializer;
import org.apache.asterix.runtime.evaluators.base.AbstractScalarFunctionDynamicDescriptor;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.common.exceptions.NotImplementedException;
import org.apache.hyracks.algebricks.core.algebra.functions.FunctionIdentifier;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluator;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluatorFactory;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.VoidPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class NumericUnaryMinusDescriptor extends AbstractScalarFunctionDynamicDescriptor {

    private static final long serialVersionUID = 1L;
    public static final IFunctionDescriptorFactory FACTORY = new IFunctionDescriptorFactory() {
        @Override
        public IFunctionDescriptor createFunctionDescriptor() {
            return new NumericUnaryMinusDescriptor();
        }
    };

    @Override
    public IScalarEvaluatorFactory createEvaluatorFactory(final IScalarEvaluatorFactory[] args) {
        return new IScalarEvaluatorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public IScalarEvaluator createScalarEvaluator(final IHyracksTaskContext ctx) throws AlgebricksException {

                return new IScalarEvaluator() {

                    private ArrayBackedValueStorage resultStorage = new ArrayBackedValueStorage();
                    private DataOutput out = resultStorage.getDataOutput();
                    private IPointable argPtr = new VoidPointable();
                    private IScalarEvaluator eval = args[0].createScalarEvaluator(ctx);
                    private AMutableDouble aDouble = new AMutableDouble(0);
                    private AMutableFloat aFloat = new AMutableFloat(0);
                    private AMutableInt64 aInt64 = new AMutableInt64(0);
                    private AMutableInt32 aInt32 = new AMutableInt32(0);
                    private AMutableInt16 aInt16 = new AMutableInt16((short) 0);
                    private AMutableInt8 aInt8 = new AMutableInt8((byte) 0);
                    @SuppressWarnings("rawtypes")
                    private ISerializerDeserializer serde;

                    @SuppressWarnings("unchecked")
                    @Override
                    public void evaluate(IFrameTupleReference tuple, IPointable result) throws AlgebricksException {
                        resultStorage.reset();
                        eval.evaluate(tuple, argPtr);
                        byte[] data = argPtr.getByteArray();
                        int offset = argPtr.getStartOffset();

                        try {
                            if (data[offset] == ATypeTag.SERIALIZED_INT8_TYPE_TAG) {
                                serde = AqlSerializerDeserializerProvider.INSTANCE
                                        .getSerializerDeserializer(BuiltinType.AINT8);
                                aInt8.setValue((byte) -AInt8SerializerDeserializer.getByte(data, offset + 1));
                                serde.serialize(aInt8, out);
                            } else if (data[offset] == ATypeTag.SERIALIZED_INT16_TYPE_TAG) {
                                serde = AqlSerializerDeserializerProvider.INSTANCE
                                        .getSerializerDeserializer(BuiltinType.AINT16);
                                aInt16.setValue((short) -AInt16SerializerDeserializer.getShort(data, offset + 1));
                                serde.serialize(aInt16, out);
                            } else if (data[offset] == ATypeTag.SERIALIZED_INT32_TYPE_TAG) {
                                serde = AqlSerializerDeserializerProvider.INSTANCE
                                        .getSerializerDeserializer(BuiltinType.AINT32);
                                aInt32.setValue(-AInt32SerializerDeserializer.getInt(data, offset + 1));
                                serde.serialize(aInt32, out);
                            } else if (data[offset] == ATypeTag.SERIALIZED_INT64_TYPE_TAG) {
                                serde = AqlSerializerDeserializerProvider.INSTANCE
                                        .getSerializerDeserializer(BuiltinType.AINT64);
                                aInt64.setValue(-AInt64SerializerDeserializer.getLong(data, offset + 1));
                                serde.serialize(aInt64, out);
                            } else if (data[offset] == ATypeTag.SERIALIZED_FLOAT_TYPE_TAG) {
                                serde = AqlSerializerDeserializerProvider.INSTANCE
                                        .getSerializerDeserializer(BuiltinType.AFLOAT);
                                aFloat.setValue(-AFloatSerializerDeserializer.getFloat(data, offset + 1));
                                serde.serialize(aFloat, out);
                            } else if (data[offset] == ATypeTag.SERIALIZED_DOUBLE_TYPE_TAG) {
                                serde = AqlSerializerDeserializerProvider.INSTANCE
                                        .getSerializerDeserializer(BuiltinType.ADOUBLE);
                                aDouble.setValue(-ADoubleSerializerDeserializer.getDouble(data, offset + 1));
                                serde.serialize(aDouble, out);
                            } else {
                                throw new NotImplementedException(
                                        AsterixBuiltinFunctions.NUMERIC_UNARY_MINUS.getName() + ": not implemented for "
                                                + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(data[offset]));
                            }
                        } catch (HyracksDataException e) {
                            throw new AlgebricksException(e);
                        }
                        result.set(resultStorage);
                    }
                };
            }
        };
    }

    @Override
    public FunctionIdentifier getIdentifier() {
        return AsterixBuiltinFunctions.NUMERIC_UNARY_MINUS;
    }

}
