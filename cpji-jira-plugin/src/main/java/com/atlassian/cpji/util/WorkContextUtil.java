package com.atlassian.cpji.util;

import com.atlassian.util.concurrent.LazyReference;
import com.atlassian.workcontext.api.WorkContextDoorway;

import java.util.function.Supplier;

public class WorkContextUtil {

    private static final LazyReference<Boolean> hasWorkContextInClassPath = new LazyReference<Boolean>() {
        @Override
        protected Boolean create() throws Exception {
            try {
                Class.forName("com.atlassian.workcontext.api.WorkContextDoorway");
                return true;
            } catch (Throwable e) {
                return false;
            }
        }
    };

    public static <T> T runWithNewWorkContextIfAvailable(final Supplier<T> supplier) {
        if (hasWorkContextInClassPath.get()) {
            return runWithNewWorkContext(supplier);
        } else {
            return supplier.get();
        }
    }

    private static <T> T runWithNewWorkContext(Supplier<T> supplier) {
        try (WorkContextDoorway doorway = new WorkContextDoorway().open()) {
            return supplier.get();
        }
    }

}
