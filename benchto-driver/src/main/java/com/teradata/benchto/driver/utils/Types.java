/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Types
{
    public static <A, B extends A> B checkType(A value, Class<B> target, String name)
    {
        checkNotNull(value, "%s is null", name);
        checkArgument(
                target.isInstance(value),
                "%s must be of type %s, not %s",
                name,
                target.getName(),
                value.getClass().getName()
        );
        return target.cast(value);
    }

    private Types()
    {
    }
}
