package charliek.hopscotch.docproxy;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.ByteStreams;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

import java.io.IOException;
import java.io.InputStream;

public class S3Service {
	private static Logger LOG = LoggerFactory.getLogger(S3Service.class);
	private final AmazonS3 s3client;

	public S3Service() {
		this.s3client = new AmazonS3Client();
	}

	public Observable<RenderObject> getRenderObject(String bucket, String path) {
		return Async.fromCallable(() -> blockingRenderObject(bucket, path), Schedulers.io());
	}

	private RenderObject blockingRenderObject(String bucket, String path) {
		LOG.debug("Looking up object at s3://{}/{}", bucket, path);
		try {
			S3Object object = s3client.getObject(new GetObjectRequest(bucket, path));
			String contentType = object.getObjectMetadata().getContentType();
			InputStream objectData = object.getObjectContent();
			try {
				byte[] bytes = ByteStreams.toByteArray(objectData);
				objectData.close();
				return new RenderObject(contentType, bytes);
			} catch (IOException e) {
				throw new AmazonClientException(
					String.format("Error downloading bytes for object s3://%s/%s", bucket, path), e);
			}
		} catch (AmazonServiceException e) {
			if (e.getErrorCode().equals("NoSuchKey")) {
				return new RenderObject("text/text", "NotFound".getBytes(), HttpResponseStatus.NOT_FOUND);
			}
			throw e;
		}
	}

}
