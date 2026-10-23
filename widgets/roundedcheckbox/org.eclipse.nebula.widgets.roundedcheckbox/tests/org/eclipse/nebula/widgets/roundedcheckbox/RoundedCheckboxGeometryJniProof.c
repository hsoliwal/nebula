/* SPDX-License-Identifier: EPL-2.0
 * JNI oracle only; no RoundedCheckbox product native method is introduced.
 */
#include <jni.h>
#include <stdint.h>
#include <string.h>

_Static_assert(sizeof(jint) == sizeof(uint32_t), "JNI jint must be 32-bit");

static uint32_t bits(jint value) {
    uint32_t result;
    memcpy(&result, &value, sizeof(result));
    return result;
}

static jint from_bits(uint32_t value) {
    jint result;
    memcpy(&result, &value, sizeof(result));
    return result;
}

JNIEXPORT jintArray JNICALL
Java_org_eclipse_nebula_widgets_roundedcheckbox_RoundedCheckboxGeometryJniProof_coordinates(
        JNIEnv *env, jclass klass, jint width, jint height) {
    (void) klass;
    const jint x = width / 2;
    const jint y = height / 2;
    const jint values[9] = {
        from_bits(bits(width) - UINT32_C(4)),
        from_bits(bits(height) - UINT32_C(4)),
        from_bits(bits(width) - UINT32_C(9)),
        from_bits(bits(height) - UINT32_C(9)),
        x, y, (jint)(y + 4), (jint)(x + 5), (jint)(y - 3)
    };
    jintArray result = (*env)->NewIntArray(env, 9);
    if (result == NULL) return NULL;
    (*env)->SetIntArrayRegion(env, result, 0, 9, values);
    return result;
}
