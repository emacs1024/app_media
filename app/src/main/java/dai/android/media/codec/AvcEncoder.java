package dai.android.media.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public final class AvcEncoder {
    private final static String TAG = "AvcEncoder";

    private final static int TIMEOUT_U_SECOND = 12000;

    private MediaCodec mMediaCodec;

    private int mWidth;
    private int mHeight;
    private int mFrameRate;

    private byte[] mInfo = null;
    private byte[] configByte;


    private final ArrayBlockingQueue<byte[]> mYUVQueue = new ArrayBlockingQueue<>(10);

    private BufferedOutputStream bufferedOutputStream;
    private FileOutputStream fileOutputStream;

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;

    private boolean mIsRunning = false;


    public AvcEncoder(int with, int height, int frame_rate, String file) {
        mWidth = with;
        mHeight = height;
        mFrameRate = frame_rate;

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", with, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, with * height * 5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            Log.e(TAG, "MediaCodec create by 'video/avc' failed.", e);
        }

        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        createFile(file);
    }

    public void put_YUV_data(byte[] buffer) {
        if (mYUVQueue.size() >= 10) {
            mYUVQueue.poll();
        }
        mYUVQueue.add(buffer);
    }

    public void stopEncoderThread() {
        mIsRunning = false;

        stopEncoder();

        if (null != bufferedOutputStream) {
            try {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "flush stream failed", e);
            }
        }
    }

    public void startEncoderThread() {
        Thread encoderThread = new Thread(() -> {
            mIsRunning = true;

            byte[] input = null;
            long pts = 0;
            long generateIndex = 0;

            if (null == mMediaCodec) {
                return;
            }

            while (mIsRunning) {
                if (mYUVQueue.size() > 0) {
                    input = mYUVQueue.poll();
                    byte[] yuv420sp = new byte[mWidth * mHeight * 3 / 2];
                    nv21TOnv12(input, yuv420sp, mWidth, mHeight);

                    input = yuv420sp;
                }

                if (null != input) {
                    long startMillis = System.currentTimeMillis();
                    ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                    ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();

                    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        pts = computePresentationTime(generateIndex);
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        inputBuffer.clear();
                        inputBuffer.put(input);
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                        generateIndex += 1;
                    }

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_U_SECOND);
                    while (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        byte[] outData = new byte[bufferInfo.size];
                        outputBuffer.get(outData);

                        if (bufferInfo.flags == 2) {
                            configByte = new byte[bufferInfo.size];
                            configByte = outData;
                        } else if (bufferInfo.flags == 1) {
                            byte[] keyFrame = new byte[bufferInfo.size + configByte.length];
                            System.arraycopy(configByte, 0, keyFrame, 0, configByte.length);
                            System.arraycopy(outData, 0, keyFrame, configByte.length, outData.length);

                            try {
                                bufferedOutputStream.write(keyFrame, 0, keyFrame.length);
                            } catch (IOException e) {
                                Log.e(TAG, "write file failed in flags equal 1.", e);
                            }
                        } else {
                            try {
                                bufferedOutputStream.write(outData, 0, outData.length);
                            } catch (IOException e) {
                                Log.e(TAG, "write file failed in flags equal other.", e);
                            }
                        }

                        mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_U_SECOND);
                    }

                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        });

        encoderThread.start();
    }

    private void createFile(String file) {
        File newFile = new File(file);
        if (newFile.exists()) {
            newFile.delete();
        }

        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(newFile));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "create failed failed.", e);
        }
    }

    private void stopEncoder() {
        if (null == mMediaCodec) {
            return;
        }

        mMediaCodec.stop();
        mMediaCodec.release();
    }

    private void nv21TOnv12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null)
            return;

        int frame_size = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, frame_size);

        for (i = 0; i < frame_size; ++i) {
            nv12[i] = nv21[i];
        }

        for (j = 0; j < frame_size / 2; j += 2) {
            nv12[frame_size + j - 1] = nv21[j + frame_size];
        }

        for (j = 0; j < frame_size / 2; j += 2) {
            nv12[frame_size + j] = nv21[j + frame_size - 1];
        }
    }

    private long computePresentationTime(long index) {
        return 132 + index * 1000000 / mFrameRate;
    }


}
