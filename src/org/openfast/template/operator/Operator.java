/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is OpenFAST.

The Initial Developer of the Original Code is The LaSalle Technology
Group, LLC.  Portions created by The LaSalle Technology Group, LLC
are Copyright (C) The LaSalle Technology Group, LLC. All Rights Reserved.

Contributor(s): Jacob Northey <jacob@lasalletech.com>
                Craig Otis <cotis@lasalletech.com>
*/


package org.openfast.template.operator;

import org.openfast.FieldValue;
import org.openfast.NumericValue;
import org.openfast.ScalarValue;
import org.openfast.StringValue;

import org.openfast.error.FastConstants;

import org.openfast.template.Scalar;
import org.openfast.template.type.Type;

import org.openfast.util.Key;

import java.util.HashMap;
import java.util.Map;


public abstract class Operator {
    protected static final String ERR_D9 = "ERR D9: PrivateEmptyMethod field with a delta operator must never have an empty prior value.";
    private static final Map OPERATOR_MAP = new HashMap();
    public static final String NONE = "none";
    public static final String CONSTANT = "constant";
    public static final String DEFAULT = "default";
    public static final String COPY = "copy";
    public static final String INCREMENT = "increment";
    public static final String DELTA = "delta";
    public static final String TAIL = "tail";
    protected static final Operator NONE_ALL = new AlwaysPresentOperator(NONE,
            Type.ALL_TYPES) {
            public ScalarValue getValueToEncode(ScalarValue value,
                ScalarValue priorValue, Scalar field) {
                if (value == null) {
                    return ScalarValue.NULL;
                }

                return value;
            }

            public ScalarValue decodeValue(ScalarValue newValue,
                ScalarValue previousValue, Scalar field) {
                return newValue;
            }

            public ScalarValue decodeEmptyValue(ScalarValue previousValue,
                Scalar field) {
                throw new IllegalStateException(
                    "This method should never be called.");
            }
        };

    protected static final Operator CONSTANT_ALL = new Operator(CONSTANT,
            Type.ALL_TYPES) {
            public ScalarValue getValueToEncode(ScalarValue value,
                ScalarValue priorValue, Scalar field) {
                return null; // Never encode constant value.
            }

            public ScalarValue decodeValue(ScalarValue newValue,
                ScalarValue previousValue, Scalar field) {
                return field.getDefaultValue();
            }

            public boolean isPresenceMapBitSet(byte[] encoding,
                FieldValue fieldValue) {
                return fieldValue != null;
            }

            public ScalarValue decodeEmptyValue(ScalarValue previousValue,
                Scalar field) {
                if (!field.isOptional()) {
                    return field.getDefaultValue();
                }

                return null;
            }

            public boolean usesPresenceMapBit(boolean optional) {
                return optional;
            }
        };

    protected static final Operator DEFAULT_ALL = new Operator(DEFAULT,
            Type.ALL_TYPES) {
            public ScalarValue getValueToEncode(ScalarValue value,
                ScalarValue priorValue, Scalar field) {
                if (value == null) {
                    return ScalarValue.NULL;
                }

                return value.equals(field.getDefaultValue()) ? null : value;
            }

            public ScalarValue decodeValue(ScalarValue newValue,
                ScalarValue previousValue, Scalar field) {
                return newValue;
            }

            public ScalarValue decodeEmptyValue(ScalarValue previousValue,
                Scalar field) {
                return field.getDefaultValue();
            }
        };

    protected static final Operator COPY_ALL = new CopyOperator(); // {
                                                                   //		public ScalarValue getValueToEncode(ScalarValue value, ScalarValue priorValue, Scalar field) {
                                                                   //
                                                                   //			if (value == null)
                                                                   //				if (field.isOptional())
                                                                   //					if (priorValue == ScalarValue.UNDEFINED)
                                                                   //						return null;
                                                                   //					else
                                                                   //						return null;
                                                                   //				else
                                                                   //					throw new IllegalStateException();
                                                                   //			if (priorValue != null && priorValue.isUndefined() && value.equals(field.getDefaultValue())) return null;
                                                                   //			return (value.equals(priorValue)) ? null : value;
                                                                   //		}
                                                                   //
                                                                   //		public ScalarValue decodeValue(ScalarValue newValue, ScalarValue previousValue, Scalar field) {
                                                                   //			return newValue;
                                                                   //		}
                                                                   //
                                                                   //		public ScalarValue decodeEmptyValue(ScalarValue previousValue, Scalar field) {
                                                                   //			if (previousValue.isUndefined())
                                                                   //				if (field.getDefaultValue().isUndefined())
                                                                   //					if (field.isOptional())
                                                                   //						return ScalarValue.UNDEFINED;
                                                                   //					else
                                                                   //						FastConstants.handleError(FastConstants.NO_DEFAULT_VALUE, "");
                                                                   //				else
                                                                   //					return field.getDefaultValue();
                                                                   //			return previousValue;
                                                                   //		}};
    protected static final Operator INCREMENT_INTEGER = new Operator(INCREMENT,
            Type.INTEGER_TYPES) {
            // TODO - refactor for efficiency
            public ScalarValue getValueToEncode(ScalarValue value,
                ScalarValue priorValue, Scalar field) {
                if (priorValue == null) {
                    return value;
                }

                if (value == null) {
                    if (field.isOptional()) {
                        if (priorValue == ScalarValue.UNDEFINED) {
                            return null;
                        }

                        return ScalarValue.NULL;
                    } else {
                        throw new IllegalArgumentException();
                    }
                }

                if (priorValue.isUndefined()) {
                    if (value.equals(field.getDefaultValue())) {
                        return null;
                    } else {
                        return value;
                    }
                }

                if (!value.equals(((NumericValue) priorValue).increment())) {
                    return value;
                }

                return null;
            }

            public ScalarValue decodeValue(ScalarValue newValue,
                ScalarValue previousValue, Scalar field) {
                return newValue;
            }

            public ScalarValue decodeEmptyValue(ScalarValue previousValue,
                Scalar field) {
                if ((previousValue == null) || previousValue.isUndefined()) {
                    if (field.getDefaultValue().isUndefined()) {
                        if (field.isOptional()) {
                            return null;
                        } else {
                            throw new IllegalStateException(
                                "Field with operator increment must send a value if no previous value existed.");
                        }
                    } else {
                        return field.getDefaultValue();
                    }
                }

                return ((NumericValue) previousValue).increment();
            }
        };

    public static final Operator DELTA_INTEGER = new AlwaysPresentOperator(DELTA,
            Type.INTEGER_TYPES) {
            public ScalarValue getValueToEncode(ScalarValue value,
                ScalarValue priorValue, Scalar field) {
                if (priorValue == null) {
                    throw new IllegalStateException(ERR_D9);
                }

                if (value == null) {
                    if (field.isOptional()) {
                        return ScalarValue.NULL;
                    } else {
                        throw new IllegalArgumentException(
                            "Mandatory fields can't be null.");
                    }
                }

                if (priorValue.isUndefined()) {
                    priorValue = field.getInitialValue();
                }

                return ((NumericValue) value).subtract((NumericValue) priorValue);
            }

            public ScalarValue decodeValue(ScalarValue newValue,
                ScalarValue previousValue, Scalar field) {
                if (previousValue == null) {
                    throw new IllegalStateException(ERR_D9);
                }

                if ((newValue == null) || newValue.isNull()) {
                    return null;
                }

                if (previousValue.isUndefined()) {
                    if (field.getDefaultValue().isUndefined()) {
                        previousValue = field.getInitialValue();
                    } else {
                        previousValue = field.getDefaultValue();
                    }
                }

                return ((NumericValue) newValue).add((NumericValue) previousValue);
            }

            public ScalarValue decodeEmptyValue(ScalarValue previousValue,
                Scalar field) {
                if (previousValue.isUndefined()) {
                    if (field.getDefaultValue().isUndefined()) {
                        if (field.isOptional()) {
                            return ScalarValue.UNDEFINED;
                        } else {
                            FastConstants.handleError(FastConstants.NO_DEFAULT_VALUE,
                                "");
                        }
                    } else {
                        return field.getDefaultValue();
                    }
                }

                return previousValue;
            }
        };

    public static final Operator DELTA_STRING = new DeltaStringOperator();
    protected static final Operator DELTA_DECIMAL = new DeltaDecimalOperator();
    protected static final Operator TAIL_STRING = new Operator("tail",
            new String[] { Type.STRING }) {
            public ScalarValue getValueToEncode(ScalarValue value,
                ScalarValue priorValue, Scalar field) {
                if (value == null) {
                    return ScalarValue.NULL;
                }

                if (priorValue == null) {
                    return value;
                }

                if (priorValue.isUndefined()) {
                    if (value.equals(field.getDefaultValue())) {
                        return null;
                    } else {
                        return value;
                    }
                }

                int index = 0;

                for (;
                        ((StringValue) value).value.charAt(index) == ((StringValue) priorValue).value.charAt(
                            index); index++)
                    ;

                return new StringValue(((StringValue) value).value.substring(
                        index));
            }

            public ScalarValue decodeValue(ScalarValue newValue,
                ScalarValue previousValue, Scalar field) {
                StringValue base;

                if ((previousValue == null) && !field.isOptional()) {
                    FastConstants.handleError(FastConstants.MNDTRY_FIELD_NOT_PRESENT,
                        "");

                    return null;
                } else if ((previousValue == null) ||
                        previousValue.isUndefined()) {
                    base = (StringValue) field.getInitialValue();
                } else {
                    base = (StringValue) previousValue;
                }

                if ((newValue == null) || newValue.isNull()) {
                    if (field.isOptional()) {
                        return null;
                    } else {
                        throw new IllegalArgumentException("");
                    }
                }

                String delta = ((StringValue) newValue).value;
                int length = Math.max(base.value.length() - delta.length(), 0);
                String root = base.value.substring(0, length);

                return new StringValue(root + delta);
            }

            public ScalarValue decodeEmptyValue(ScalarValue previousValue,
                Scalar field) {
                if (previousValue.isUndefined()) {
                    return field.getInitialValue();
                }

                return previousValue;
            }
        };

    private final String name;

    protected Operator(String name, String[] types) {
        this.name = name;

        for (int i = 0; i < types.length; i++) {
            Key key = new Key(name, types[i]);

            if (!OPERATOR_MAP.containsKey(key)) {
                OPERATOR_MAP.put(key, this);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "operator:" + name;
    }

    public static Operator getOperator(String name, String type) {
        Key key = new Key(name, type);

        if (!OPERATOR_MAP.containsKey(key)) {
        	FastConstants.handleError(FastConstants.OPERATOR_TYPE_INCOMP, "The operator \"" + name +
                    "\" does not exist for the type " + type);
            throw new IllegalArgumentException();
        }

        return (Operator) OPERATOR_MAP.get(key);
    }

    public abstract ScalarValue getValueToEncode(ScalarValue value,
        ScalarValue priorValue, Scalar field);

    public abstract ScalarValue decodeValue(ScalarValue newValue,
        ScalarValue priorValue, Scalar field);

    public boolean isPresenceMapBitSet(byte[] encoding, FieldValue fieldValue) {
        return encoding.length != 0;
    }

    public abstract ScalarValue decodeEmptyValue(ScalarValue previousValue,
        Scalar field);

    public boolean usesPresenceMapBit(boolean optional) {
        return true;
    }
}
