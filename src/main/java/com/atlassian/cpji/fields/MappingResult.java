package com.atlassian.cpji.fields;

import java.util.List;

/**
 * @since v1.4
 */
public class MappingResult
    {
        private final List<String> unmappedValues;
        private final boolean hasOneValidValue;
        private final boolean isEmpty;

        public MappingResult(final List<String> unmappedValues, final boolean hasOneValidValue, final boolean empty)
        {
            this.unmappedValues = unmappedValues;
            this.hasOneValidValue = hasOneValidValue;
            isEmpty = empty;
        }

        public List<String> getUnmappedValues()
        {
            return unmappedValues;
        }

        public boolean hasOneValidValue()
        {
            return hasOneValidValue;
        }

        public boolean isEmpty()
        {
            return isEmpty;
        }
    }
