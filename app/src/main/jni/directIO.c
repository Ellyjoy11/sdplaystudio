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
#include <pthread.h>
#include <sys/mman.h>

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

typedef struct {
    int id;
    int threads;
    int fd;
    off_t fsize;
    int bsize;
    int mode;
    int ops;
    void *dbuf;
} tdata;

tdata tdr[32];
tdata tdw[32];

void *worker_bee(void *data)
{
    int bytes, j, r;
    tdata *td = (tdata *)data;
    off_t pos;

    // pthread_cond_wait();

    // chunk size must be aligned to bsize
    bytes = ((td->fsize/td->threads)/td->bsize) * td->bsize;

    if (td->mode)
        printf("Worker bee #%d, writing %d bytes at position %ld...\n", td->id, bytes, (long)td->id * bytes);
    else
        printf("Worker bee #%d, reading %d bytes at position %ld...\n", td->id, bytes, (long)td->id * bytes);

    pos = lseek(td->fd, td->id * bytes, SEEK_SET);
    if (pos < 0) {
        printf("worker bee #%d, error %d seeking fd #%d", td->id, errno, td->fd);
    } else {
        printf("worker bee #%d, offset %ld for fd #%d", td->id, pos, td->fd);
    }

    for (j=0; j<bytes/td->bsize; j++) {
        if (td->mode) {
            if ((r = write(td->fd, td->dbuf, td->bsize)) < 0) {
                td->ops = r;
                printf("Error on write(fd=%d, dbuf=%p, len=%d) , errno: %d", td->fd, td->dbuf, td->bsize, errno);
                break;
            }
        } else {
            if ((r = read(td->fd, td->dbuf, td->bsize)) < 0) {
                td->ops = r;
                printf("Error on read(fd=%d, dbuf=%p, len=%d) , errno: %d", td->fd, td->dbuf, td->bsize, errno);
                break;
            }
        }
        td->ops++;
    }
    return &td->ops;
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directIOPSr(JNIEnv* env, jobject obj, jstring path, int mode, int bsize, int threads)
{
    int ops = 0;
    int i, r;
	const char * filepath = (*env)->GetStringUTFChars(env, path, NULL);
	pthread_t tid[32];
	struct stat st;
    jbyte *dbuf = NULL;
    int fd;

    if (!mode)
        mode = O_RDONLY | O_DIRECT;
	fd = open(filepath, mode);
	if (fd >= 0) {
	    fstat(fd, &st);
	    printf("Opened directIO file (%s, %lu bytes), fd: %d", filepath, st.st_size, fd);
	} else {
	    printf("Failed to open directIO file (%s), errno: %d", filepath, errno);
	    return fd;
    }

/*
	if ((dbuf = memalign(bsize, bsize)) != NULL) {
	    printf("Allocated aligned buffer [%p:%d)", dbuf, bsize);
	    memset(dbuf, 0, bsize);
	} else {
	    printf("Failed to allocate aligned buffer [%d bytes], errno: %d", bsize, errno);
	    return errno;
	}
*/

    for(i=0; i<threads; i++) {
        tdr[i].threads = threads;
        tdr[i].bsize = bsize;
        tdr[i].fsize = st.st_size;
        tdr[i].dbuf = memalign(bsize, bsize);
        tdr[i].fd = open(filepath, mode);
        tdr[i].id = i;
        tdr[i].ops = 0;
        tdr[i].mode = 0;
        printf("Creating worker thread %d ...\n", i);
        pthread_create(&tid[i], NULL, worker_bee, &tdr[i]);
    }

    // pthread_cond_broadcast(BLAH);

    for(i=0; i<threads; i++) {
        printf("Joining worker thread %d ...\n", i);
        pthread_join(tid[i], NULL);
        close(tdr[i].fd);
        free(tdr[i].dbuf);
        ops += tdr[i].ops;
    }

//    free(dbuf);
    close(fd);
    return ops;
}

JNIEXPORT int JNICALL
Java_com_elena_sdplay_BenchStart_directIOPSw(JNIEnv* env, jobject obj, jstring path, int mode, int bsize, int threads)
{
    int ops = 0;
    int i, r;
	const char * filepath = (*env)->GetStringUTFChars(env, path, NULL);
	pthread_t tid[32];
	struct stat st;
    jbyte *dbuf = NULL;
    int fd;

    if (!mode)
        mode = O_WRONLY | O_DIRECT | O_SYNC;
	fd = open(filepath, mode);
	if (fd >= 0) {
	    fstat(fd, &st);
	    printf("Opened directIO file (%s, %lu bytes), fd: %d", filepath, st.st_size, fd);
	} else {
	    printf("Failed to open directIO file (%s), errno: %d", filepath, errno);
	    return fd;
    }

/*
	if ((dbuf = memalign(bsize, bsize)) != NULL) {
	    printf("Allocated aligned buffer [%p:%d)", dbuf, bsize);
	    memset(dbuf, 0, bsize);
	} else {
	    printf("Failed to allocate aligned buffer [%d bytes], errno: %d", bsize, errno);
	    return errno;
	}
*/

    for(i=0; i<threads; i++) {
        tdw[i].threads = threads;
        tdw[i].bsize = bsize;
        tdw[i].fsize = st.st_size;
        tdw[i].dbuf = memalign(bsize, bsize);
        tdw[i].fd = open(filepath, mode);
        tdw[i].id = i;
        tdw[i].ops = 0;
        tdw[i].mode = 1;
        printf("Creating worker thread %d ...\n", i);
        pthread_create(&tid[i], NULL, worker_bee, &tdw[i]);
    }

    // pthread_cond_broadcast(BLAH);

    for(i=0; i<threads; i++) {
        printf("Joining worker thread %d ...\n", i);
        pthread_join(tid[i], NULL);
        close(tdw[i].fd);
        free(tdw[i].dbuf);
        ops += tdw[i].ops;
    }

//    free(dbuf);
    close(fd);
    return ops;
}
