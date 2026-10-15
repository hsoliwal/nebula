/*******************************************************************************
 * Copyright (C) 2011 Angelo Zerr <angelo.zerr@gmail.com>, Pascal Leclercq <pascal.leclercq@gmail.com>
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo ZERR - initial API and implementation
 *     Pascal Leclercq - initial API and implementation
 *******************************************************************************/
/* Optional C17 JNI batch projection of the pinned Nebula page-window method.
 * No native dispatch is enabled in existing widgets. No pointers escape the call.
 */
#include "org_eclipse_nebula_widgets_pagination_internal_PageWindowBatch.h"
#include <stdint.h>
#include <stdlib.h>
_Static_assert(sizeof(jint)==sizeof(int32_t), "JNI int width");
static int page_window(int currentPageIndex,int totalPages,int nbMax,int32_t *destination) {

		nbMax = nbMax > totalPages ? totalPages : nbMax;
		int32_t *indexes = destination;
        for (int j=0;j<nbMax;j++) indexes[j]=0;
		if (totalPages <= nbMax) {
			// ALl indexes must be filled
			for (int i = 0; i < nbMax; i++) {
				indexes[i] = i;
			}
		} else {
			if (currentPageIndex > (totalPages - nbMax) + 3) {
				int index = totalPages - 1;
				for (int i = nbMax - 1; i >= 0; i--) {
					if (i == 0) {
						indexes[i] = i;
					} else if (i == 1) {
						indexes[i] = -1;
					} else {
						indexes[i] = index--;
					}
				}
			} else {

				if (nbMax - currentPageIndex > 2) {
					for (int i = 0; i < nbMax; i++) {
						if (i == nbMax - 1) {
							indexes[i] = totalPages - 1;
						} else if (i == nbMax - 2) {
							indexes[i] = -1;
						} else {
							indexes[i] = i;
						}
					}
				} else {
					// Total page is > to nb max of pages to display
					// Compute list to have for instance 1 ... 10, 11, 12, 13,
					// 14,
					// ...
					// 20
					int middle = nbMax / 2;
					int index = currentPageIndex;
					int nbItems = 0;
					for (int i = middle; i > 0 && index > 0; i--) {
						if (i == 1) {
							// before last item, check if it's 2 item
							indexes[i] = index == 2 ? index : -1;
						} else if (i == 0) {
							indexes[i] = 0;
						} else if (index > 0) {
							indexes[i] = index--;
						}
						nbItems++;
					}

					index = currentPageIndex;
					for (int i = nbItems; i < nbMax; i++) {
						if (i == nbMax - 2) {
							indexes[i] = index == totalPages ? index : -1;
						} else if (i == nbMax - 1) {
							indexes[i] = totalPages - 1;
						} else {
							indexes[i] = index++;
						}
					}
				}
			}

		}
		return nbMax;
	
}
static jintArray fail(JNIEnv *env,const char *type,const char *message) {
    jclass cls=(*env)->FindClass(env,type);
    if(cls!=NULL){(void)(*env)->ThrowNew(env,cls,message);(*env)->DeleteLocalRef(env,cls);}
    return NULL;
}
JNIEXPORT jintArray JNICALL Java_org_eclipse_nebula_widgets_pagination_internal_PageWindowBatch_nativeWindows
(JNIEnv *env,jclass cls,jintArray triples) {
    (void)cls;
    if(triples==NULL)return fail(env,"java/lang/IllegalArgumentException","invalid batch shape");
    jsize n=(*env)->GetArrayLength(env,triples);
    if(n%3!=0 || n>4096*3)return fail(env,"java/lang/IllegalArgumentException","invalid batch shape");
    if(n==0)return (*env)->NewIntArray(env,0);
    jint *owned=malloc((size_t)n*sizeof(jint));
    if(owned==NULL)return fail(env,"java/lang/OutOfMemoryError","native batch copy");
    (*env)->GetIntArrayRegion(env,triples,0,n,owned);
    if((*env)->ExceptionCheck(env)){free(owned);return NULL;}
    for(jsize i=0;i<n;i+=3){
        jint current=owned[i],total=owned[i+1],slots=owned[i+2];
        if(total<0 || current<0 || (total==0?current!=0:current>=total) || slots<5 || slots>31){
            free(owned);return fail(env,"java/lang/IllegalArgumentException","invalid page window");
        }
    }
    jintArray result=(*env)->NewIntArray(env,n/3*32);
    if(result==NULL){free(owned);return NULL;}
    for(jsize i=0;i<n;i+=3){
        jint row[32];
        for(int j=0;j<32;j++)row[j]=-2;
        row[0]=page_window(owned[i],owned[i+1],owned[i+2],row+1);
        (*env)->SetIntArrayRegion(env,result,i/3*32,32,row);
        if((*env)->ExceptionCheck(env)){free(owned);return NULL;}
    }
    free(owned);return result;
}
