/* SPDX-License-Identifier: EPL-2.0
 * Test-only JNI twin of the existing Chips.drawClose int calculations.
 */
#include <jni.h>
#include <stdint.h>
#include <string.h>

_Static_assert(sizeof(jint)==sizeof(uint32_t), "JNI jint must have 32 bits");
static uint32_t bits(jint x) {
    uint32_t u;
    memcpy(&u, &x, sizeof(u));
    return u;
}
static jint jint_bits(uint32_t x) {
    jint n;
    memcpy(&n, &x, sizeof(n));
    return n;
}
JNIEXPORT jintArray JNICALL
Java_org_eclipse_nebula_widgets_chips_ChipsCloseCenterJniProof_center(
        JNIEnv *env, jclass klass, jint x, jint height) {
    (void)klass;
    /* Java signed-int additions wrap mod 2^32. C signed overflow is undefined. */
    uint32_t centerX=bits(x)+UINT32_C(4)+UINT32_C(7);
    uint32_t shiftedHeight=bits(height)-UINT32_C(14);
    /* JNI int division truncates toward zero exactly as Java for /2. */
    jint half=jint_bits(shiftedHeight)/2;
    uint32_t centerY=bits(half)+UINT32_C(7);
    jint result[2]={jint_bits(centerX),jint_bits(centerY)};
    jintArray output=(*env)->NewIntArray(env,2);
    if(output==NULL) return NULL;
    (*env)->SetIntArrayRegion(env,output,0,2,result);
    return output;
}
