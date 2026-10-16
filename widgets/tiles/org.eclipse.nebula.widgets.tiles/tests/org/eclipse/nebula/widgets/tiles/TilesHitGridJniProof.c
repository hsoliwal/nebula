/* SPDX-License-Identifier: EPL-2.0
 * Test-only JNI coordinate oracle, not loaded by production Nebula Tiles.
 */
#include <jni.h>
#include <stdint.h>
#include <limits.h>

static void invalid(JNIEnv *env) {
    jclass klass = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (klass != NULL) {
        (*env)->ThrowNew(env, klass, "unqualified tile hit geometry");
        (*env)->DeleteLocalRef(env, klass);
    }
}

JNIEXPORT jint JNICALL
Java_org_eclipse_nebula_widgets_tiles_TilesHitGridJniProof_indexed(
        JNIEnv *env, jclass klass, jint firstX, jint firstY, jint columns,
        jint stepX, jint stepY, jint count, jint tileWidth, jint tileHeight,
        jint x, jint y) {
    (void)klass;
    if (columns <= 0 || stepX <= 0 || stepY <= 0 || count < 0
            || tileWidth <= 0 || tileHeight <= 0 || firstX <= 0 || firstY <= 0) {
        invalid(env);
        return -1;
    }
    if (x < firstX || y < firstY) return -1;
    int64_t col = ((int64_t)x - firstX) / stepX;
    int64_t row = ((int64_t)y - firstY) / stepY;
    if (col >= columns || row > INT64_MAX / columns) return -1;
    int64_t slot = row * columns + col;
    if (slot >= count) return -1;
    int64_t left = firstX + col * stepX;
    int64_t top = firstY + row * stepY;
    if ((int64_t)x <= left + tileWidth && (int64_t)y <= top + tileHeight) {
        return (jint)slot;
    }
    return -1;
}
