package dai.android.media.codec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder {
    private static final String TAG = "VideoDecoder";

    private static final String VIDEO = "video/";
    private MediaExtractor mMediaExtractor;
    private MediaCodec mMediaCodec;


    private boolean mEosReceived = false;

    public boolean setDataSource(Surface surface, String filePath) {
        Log.d(TAG, "setDataSource: file = " + filePath);

        mEosReceived = false;

        try {
            mMediaExtractor = new MediaExtractor();

            mMediaExtractor.setDataSource(filePath);

            for (int i = 0; i < mMediaExtractor.getTrackCount(); ++i) {
                MediaFormat format = mMediaExtractor.getTrackFormat(i);

                String mime = format.getString(MediaFormat.KEY_MIME);
                if (null == mime) {
                    continue;
                }

                if (mime.startsWith(VIDEO)) {
                    mMediaExtractor.selectTrack(i);
                    mMediaCodec = MediaCodec.createByCodecName(mime);
                    try {
                        mMediaCodec.configure(format, surface, null, 0);
                    } catch (IllegalStateException e) {
                        return false;
                    }
                    mMediaCodec.start();
                    break;
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "setDataSource failed.", e);
        }

        return true;
    }

    public void start() {
        Thread thread = new Thread(mRunnable);
        thread.start();
    }


    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (null == mMediaCodec) {
                Log.e(TAG, "MediaCodec not created");
                return;
            }

            if (null == mMediaExtractor) {
                Log.e(TAG, "MediaExtractor not create");
                return;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();

            mMediaCodec.getOutputBuffers();

            boolean isInput = true;
            boolean isFirst = false;
            long startTime = 0L;

            while (!mEosReceived) {
                if (isInput) {
                    int inputIndex = mMediaCodec.dequeueInputBuffer(10000);
                    if (inputIndex < 0) {
                        Log.w(TAG, "can not dequeueInputBuffer");
                        continue;
                    }

                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
                    if (null == inputBuffer) {
                        Log.w(TAG, "can getInputBuffer by index " + inputIndex);
                        continue;
                    }

                    int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                    if (sampleSize > 0 && mMediaExtractor.advance()) {
                        mMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mMediaExtractor.getSampleTime(), 0);
                    } else {
                        Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isInput = false;
                    }
                }

                int outIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                switch (outIndex) {
                    //case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                        Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format = " + mMediaCodec.getOutputFormat());
                        break;
                    }
                    case MediaCodec.INFO_TRY_AGAIN_LATER: {
                        break;
                    }

                    default: {
                        if (!isFirst) {
                            startTime = System.currentTimeMillis();
                            isFirst = true;
                        }

                        long sleepTime = (bufferInfo.presentationTimeUs / 1000) - (System.currentTimeMillis() - startTime);
                        Log.d(TAG, "presentationTimeUs : " + (bufferInfo.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startTime) + " sleepTime : " + sleepTime);
                        if (sleepTime > 0) {
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException ignore) {
                            }
                        }

                        mMediaCodec.releaseOutputBuffer(outIndex, true);
                        break;
                    }
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }

            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaExtractor.release();
        }
    };


    public void close() {
        mEosReceived = true;
    }

}
