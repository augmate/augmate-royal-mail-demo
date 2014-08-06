#include <jni.h>
#include <stdio.h>

#include "Helpers.h"
#include "BinarizerSimple.h"

// this header is automatically generated by the javah task
#include "NativeUtils.h"

JNIEXPORT void JNICALL Java_com_augmate_sdk_scanner_NativeUtils_binarize(
    JNIEnv *env, jclass unused, jbyteArray srcArray, jbyteArray dstArray, jint width, jint height
) {
    jbyte* src = (*env)->GetByteArrayElements(env, srcArray, NULL);
    jbyte* dst = (*env)->GetByteArrayElements(env, dstArray, NULL);

    binarizerSimpleByteArray(src, dst, width, height);

    (*env)->ReleaseByteArrayElements(env, srcArray, src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, dstArray, dst, 0);
}

// front-facing cameras are mirrored, so we must manually flip horizontally here
JNIEXPORT void JNICALL Java_com_augmate_sdk_scanner_NativeUtils_binarizeToIntBuffer(
    JNIEnv *env, jclass unused, jbyteArray srcArray, jintArray dstArray, jint width, jint height
) {
    jbyte* src = (*env)->GetByteArrayElements(env, srcArray, NULL);
    jint* dst = (*env)->GetIntArrayElements(env, dstArray, NULL);

    binarizerSimpleByteToIntArray(src, dst, width, height);

    (*env)->ReleaseByteArrayElements(env, srcArray, src, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}