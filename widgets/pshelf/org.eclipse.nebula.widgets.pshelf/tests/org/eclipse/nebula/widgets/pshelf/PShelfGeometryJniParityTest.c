/* SPDX-License-Identifier: EPL-2.0
 * Test-only JNI oracle: the original PShelf Java geometry remains runtime authority.
 */
#include <jni.h>
#include <stdint.h>
#include <limits.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>

_Static_assert(sizeof(jint) == sizeof(uint32_t), "JNI int must be 32-bit");

static uint32_t jbits(jint value) {
    uint32_t result;
    memcpy(&result, &value, sizeof(result));
    return result;
}
static jint jint_bits(uint32_t value) {
    jint result;
    memcpy(&result, &value, sizeof(result));
    return result;
}
static jint jdouble_to_int(double value) {
    if (isnan(value)) return 0;
    if (value >= (double)INT32_MAX) return INT32_MAX;
    if (value <= (double)INT32_MIN) return INT32_MIN;
    return (jint)value;
}
static void invalid(JNIEnv *env, const char *reason) {
    jclass exception = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (exception != NULL) {
        (*env)->ThrowNew(env, exception, reason);
        (*env)->DeleteLocalRef(env, exception);
    }
}
static jintArray allocate(JNIEnv *env, jint count, jint **work) {
    *work = NULL;
    if (count < 0 || count > 1048576) {
        invalid(env, "test-only PShelf JNI row count outside bounded proof range");
        return NULL;
    }
    jintArray result = (*env)->NewIntArray(env, count);
    if (result == NULL || count == 0) return result;
    *work = malloc((size_t)count * sizeof(**work));
    if (*work == NULL) {
        jclass exception = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (exception != NULL) {
            (*env)->ThrowNew(env, exception, "PShelf JNI scratch allocation");
            (*env)->DeleteLocalRef(env, exception);
        }
        return NULL;
    }
    return result;
}
static void finish(JNIEnv *env, jintArray result, jint *work, jint count) {
    if (result != NULL && work != NULL && count != 0) {
        (*env)->SetIntArrayRegion(env, result, 0, count, work);
    }
    free(work);
}

JNIEXPORT jintArray JNICALL
Java_org_eclipse_nebula_widgets_pshelf_PShelfGeometryJniParityTest_steady(
        JNIEnv *env, jclass cls, jint count, jint openIndex,
        jint clientY, jint clientHeight, jint itemHeight) {
    (void)cls;
    if (openIndex < -1 || openIndex >= count) {
        invalid(env, "steady header index outside original PShelf model");
        return NULL;
    }
    jint *work;
    jintArray result = allocate(env, count, &work);
    if (result == NULL || count == 0) return result;
    uint32_t y = jbits(clientY);
    const uint32_t header = jbits(itemHeight);
    const uint32_t bottom = jbits(clientY) + jbits(clientHeight);
    for (jint i = 0; i < count; ++i) {
        work[i] = jint_bits(y);
        y += header;
        if (i == openIndex) {
            y = bottom - header * (uint32_t)(count - (i + 1));
        }
    }
    finish(env, result, work, count);
    return result;
}

JNIEXPORT jintArray JNICALL
Java_org_eclipse_nebula_widgets_pshelf_PShelfGeometryJniParityTest_animated(
        JNIEnv *env, jclass cls, jint count, jint previous, jint next,
        jint clientY, jint clientHeight, jint itemHeight,
        jdouble progress, jboolean fromBottom) {
    (void)cls;
    if (previous < -1 || previous >= count || next < -1 || next >= count) {
        invalid(env, "animated header indices outside original PShelf model");
        return NULL;
    }
    jint *work;
    jintArray result = allocate(env, count, &work);
    if (result == NULL || count == 0) return result;
    const uint32_t header = jbits(itemHeight);
    const uint32_t client = jbits(clientY);
    const uint32_t height = jbits(clientHeight);
    uint32_t top = client;
    uint32_t bottom = client + height - header * (uint32_t)(count - (next + 1));
    uint32_t areaBits = height - header * (uint32_t)count;
    const double portion = fromBottom == JNI_TRUE ? 1.0 - progress : progress;
    const uint32_t gap = jbits(jdouble_to_int((double)jint_bits(areaBits) * portion));
    int inserted = 0;
    for (jint i = 0; i < count; ++i) {
        if (i <= previous) {
            work[i] = jint_bits(top);
            top += header;
        } else if (i > next) {
            work[i] = jint_bits(bottom);
            bottom += header;
        } else {
            if (!inserted) {
                top += gap;
                inserted = 1;
            }
            work[i] = jint_bits(top);
            top += header;
        }
    }
    finish(env, result, work, count);
    return result;
}
