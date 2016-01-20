package uk.co.drnaylor.minecraft.quickstart.internal.annotations;

import java.lang.annotation.*;

/**
 * Any {@link uk.co.drnaylor.minecraft.quickstart.internal.CommandBase} that is decorated with this annotation will be
 * run on an async thread. This should only be used for thread-safe operations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RunAsync {
}