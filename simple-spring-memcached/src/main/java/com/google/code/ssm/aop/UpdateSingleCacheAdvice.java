package net.nelz.simplesm.aop;

import java.lang.reflect.Method;
import java.util.Collection;

import net.nelz.simplesm.api.UpdateSingleCache;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright (c) 2008, 2009 Nelson Carpentier
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * @author Nelson Carpentier
 * 
 */
@Aspect
public class UpdateSingleCacheAdvice extends CacheBase {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateSingleCacheAdvice.class);

    @Pointcut("@annotation(net.nelz.simplesm.api.UpdateSingleCache)")
    public void updateSingle() {
    }

    @AfterReturning(pointcut = "updateSingle()", returning = "retVal")
    public void cacheUpdateSingle(final JoinPoint jp, final Object retVal) throws Throwable {
        // For Update*Cache, an AfterReturning aspect is fine. We will only
        // apply our caching after the underlying method completes successfully, and we will have
        // the same access to the method params.
        try {
            final Method methodToCache = getMethodToCache(jp);
            final UpdateSingleCache annotation = methodToCache.getAnnotation(UpdateSingleCache.class);
            final AnnotationData annotationData = AnnotationDataBuilder.buildAnnotationData(annotation, UpdateSingleCache.class,
                    methodToCache);

            final String[] objectsIds = getObjectIds(annotationData.getKeysIndex(), jp, methodToCache);
            final String cacheKey = buildCacheKey(objectsIds, annotationData);

            final Object dataObject = annotationData.isReturnDataIndex() ? retVal : getIndexObject(annotationData.getDataIndex(), jp,
                    methodToCache);
            Class<?> jsonClass = getJsonClass(methodToCache, annotationData.getDataIndex());
            final Object submission = (dataObject == null) ? PertinentNegativeNull.NULL : dataObject;
            set(cacheKey, annotationData.getExpiration(), submission, jsonClass);
        } catch (Exception ex) {
            warn("Updating caching via " + jp.toShortString() + " aborted due to an error.", ex);
        }
    }

    protected String getObjectId(final int keyIndex, final Object returnValue, final JoinPoint jp, final Method methodToCache)
            throws Exception {
        final Object keyObject = keyIndex == -1 ? validateReturnValueAsKeyObject(returnValue, methodToCache) : getIndexObject(keyIndex, jp,
                methodToCache);
        final Method keyMethod = getKeyMethod(keyObject);
        return generateObjectId(keyMethod, keyObject);
    }

    protected String[] getObjectIds(final Collection<Integer> keysIndexes, final JoinPoint jp, final Method methodToCache) throws Exception {
        final Object[] keysObjects = getIndexObjects(keysIndexes, jp, methodToCache);
        final Method[] keysMethods = getKeysMethods(keysObjects);
        return generateObjectIds(keysMethods, keysObjects);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}