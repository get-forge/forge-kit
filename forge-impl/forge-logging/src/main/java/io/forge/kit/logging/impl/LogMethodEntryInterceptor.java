package io.forge.kit.logging.impl;

import io.forge.kit.common.impl.interceptor.InvocationContextParameterExtractor;
import io.forge.kit.common.impl.reflect.AnnotationResolver;
import io.forge.kit.logging.api.LogMethodEntry;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Method;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

/**
 * CDI interceptor that automatically logs method entry for methods annotated with {@link LogMethodEntry}. The interceptor logs the class name and
 * method name in the format: {@code ClassName#methodName}.
 *
 * <p>The logger is created for the actual class where the method is declared, not the interceptor class,
 * ensuring log messages show the correct class name (e.g., {@code ActorService#getProfile} instead of
 * {@code LogMethodEntryInterceptor#getProfile}).
 *
 * <p>If a message format is provided in the annotation, it will be appended with extracted parameter values.
 */
@LogMethodEntry
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_AFTER + 100) // 3100
public final class LogMethodEntryInterceptor
{
    @AroundInvoke
    public Object logMethodEntry(final InvocationContext context) throws Exception
    {
        final LogMethodEntry annotation = AnnotationResolver.resolve(context, LogMethodEntry.class);
        if (annotation != null)
        {
            final Method method = context.getMethod();
            final Class<?> declaringClass = method.getDeclaringClass();
            final Logger logger = Logger.getLogger(declaringClass);

            final String className = declaringClass.getSimpleName();
            final String methodName = method.getName();

            if (StringUtils.isEmpty(annotation.message()))
            {
                logger.infof("%s#%s", className, methodName);
            }
            else
            {
                final Object[] args = InvocationContextParameterExtractor.extractParameterValues(context, annotation.argPaths());
                logger.infof("%s#%s %s", className, methodName, String.format(annotation.message(), args));
            }
        }

        return context.proceed();
    }
}
