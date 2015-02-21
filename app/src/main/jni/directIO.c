#include <jni.h>
#include <string.h>

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <malloc.h>

#include <errno.h>

#include <android/log.h>

#define  LOG_TAG    "SDPlay"
#define  LOGI(...)  ((void)__android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__))
#define  LOGE(...)  ((void)__android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__))
#define printf(fmt,args...)  __android_log_print(4, LOG_TAG, fmt, ##args)

JNIEXPORT jobject JNICALL
Java_com_elena_sdplay_BenchStart_directAllocate( JNIEnv* env, jobject obj, int size )
{
	jobject nio_buf = NULL;
	jbyte *dbuf = memalign(size, size);
	if (dbuf != NULL) {
	    nio_buf = (*env)->NewDirectByteBuffer(env, dbuf, size);
        printf("Allocated direct NIO buffer @ %p [%p]", nio_buf, dbuf);
	} else
        printf("Failed to allocate direct NIO buffer of size %d", size);
	return nio_buf;
}

JNIEXPORT void JNICALL
Java_com_elena_sdplay_BenchStart_directFree( JNIEnv* env, jobject obj, jobject nio_buf )
{
	free((*env)->GetDirectBufferAddress(env, nio_buf));
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directOpen( JNIEnv* env, jobject obj, jstring path, int mode )
{
	const char * filepath = (*env)->GetStringUTFChars(env, path, NULL);
	int fd = open(filepath, mode ? mode : O_RDWR | O_DIRECT /*| O_SYNC*/);
	printf("Opened directIO file (%s), fd: %d", filepath, fd);
    return fd;
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directClose( JNIEnv* env, jobject obj, int fd )
{
    return close(fd);
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directSeek( JNIEnv* env, jobject obj, int fd, long offset )
{
    return lseek(fd, offset, SEEK_SET);
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directRead( JNIEnv* env, jobject obj, int fd, jobject buffer, int size )
{
	jbyte *buf = (*env)->GetDirectBufferAddress(env, buffer);
	jsize len = (*env)->GetDirectBufferCapacity(env, buffer);
    int bytes = -EINVAL;

    if (len < size) {
        printf("Buffer size (%d) is smaller than requested read size (%d)", len, size);
    } else if ((bytes = read(fd, buf, len)) < 0) {
    	printf("Error on read(fd=%d, buf=%p, len=%d) , errno: %d", fd, buf, len, errno);
    }
    return bytes;
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directWrite( JNIEnv* env, jobject obj, int fd, jobject buffer, int size )
{
	jbyte *buf = (*env)->GetDirectBufferAddress(env, buffer);
	jsize len = (*env)->GetDirectBufferCapacity(env, buffer);
    int bytes = -EINVAL;

    if (len < size) {
        printf("Buffer size (%d) is smaller than requested read size (%d)", len, size);
    } else if ((bytes = write(fd, buf, len)) < 0) {
      	printf("Error on write(fd=%d, buf=%p, len=%d) , errno: %d", fd, buf, len, errno);
    }
    return bytes;
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directIOPSr( JNIEnv* env, jobject obj, jstring path, int mode, int bsize)
{
    int bytes = 0, ops = 0;
    int i, r;
	const char * filepath = (*env)->GetStringUTFChars(env, path, NULL);
	struct stat st;
	jbyte *dbuf = NULL;

	int fd = open(filepath, mode ? mode : O_RDWR | O_DIRECT /*| O_SYNC*/);
	if (fd >= 0) {
	    fstat(fd, &st);
	    printf("Opened directIO file (%s, %lu bytes), fd: %d", filepath, st.st_size, fd);
	} else {
	    printf("Failed to open directIO file (%s), errno: %d", filepath, errno);
	    return fd;
    }

	if ((dbuf = memalign(bsize, bsize)) != NULL) {
	    printf("Allocated aligned buffer [%p:%d)", dbuf, bsize);
	} else {
	    printf("Failed to allocate aligned buffer [%d bytes], errno: %d", bsize, errno);
	    return errno;
	}

    for(i=0; i<st.st_size/2; i+=bsize) {
        lseek(fd, i, SEEK_SET);
        if ((r = read(fd, dbuf, bsize)) < 0) {
          	ops = r;
          	printf("Error on read(fd=%d, dbuf=%p, len=%d) , errno: %d", fd, dbuf, bsize, errno);
          	break;
        }
        ops++;
        bytes += r;
        lseek(fd, i+st.st_size/2, SEEK_SET);
        if ((r = read(fd, dbuf, bsize)) < 0) {
          	ops = r;
          	printf("Error on read(fd=%d, dbuf=%p, len=%d) , errno: %d", fd, dbuf, bsize, errno);
          	break;
        }
        ops++;
        bytes += r;
    }
    free(dbuf);
    close(fd);
    return ops;
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directIOPSw( JNIEnv* env, jobject obj, jstring path, int mode, int bsize)
{
    int bytes = 0, ops = 0;
    int i, r;
	const char * filepath = (*env)->GetStringUTFChars(env, path, NULL);
	struct stat st;
	jbyte *dbuf = NULL;

	int fd = open(filepath, mode ? mode : O_RDWR | O_DIRECT /*| O_SYNC*/);
	if (fd >= 0) {
	    fstat(fd, &st);
	    printf("Opened directIO file (%s, %lu bytes), fd: %d", filepath, st.st_size, fd);
	} else {
	    printf("Failed to open directIO file (%s), errno: %d", filepath, errno);
	    return fd;
    }

	if ((dbuf = memalign(bsize, bsize)) != NULL) {
	    printf("Allocated aligned buffer [%p:%d)", dbuf, bsize);
	    memset(dbuf, 0, bsize);
	} else {
	    printf("Failed to allocate aligned buffer [%d bytes], errno: %d", bsize, errno);
	    return errno;
	}

    for(i=0; i<st.st_size/2; i+=bsize) {
        lseek(fd, i, SEEK_SET);
        if ((r = write(fd, dbuf, bsize)) < 0) {
          	ops = r;
          	printf("Error on write(fd=%d, dbuf=%p, len=%d) , errno: %d", fd, dbuf, bsize, errno);
          	break;
        }
        ops++;
        bytes += r;
        lseek(fd, i+st.st_size/2, SEEK_SET);
        if ((r = write(fd, dbuf, bsize)) < 0) {
          	ops = r;
          	printf("Error on write(fd=%d, dbuf=%p, len=%d) , errno: %d", fd, dbuf, bsize, errno);
          	break;
        }
        ops++;
        bytes += r;
    }

    free(dbuf);
    close(fd);
    return ops;
}
