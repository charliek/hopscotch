package charliek.hopscotch.docproxy.utils;

import io.netty.handler.codec.http.HttpResponseStatus;

public class RenderObject {
	private String contentType;
	private byte[] bytes;
	private HttpResponseStatus status;

	public RenderObject(String contentType, byte[] bytes) {
		this(contentType, bytes, HttpResponseStatus.OK);
	}

	public RenderObject(String contentType, byte[] bytes, HttpResponseStatus status) {
		this.contentType = contentType;
		this.bytes = bytes;
		this.status = status;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public HttpResponseStatus getStatus() {
		return status;
	}

	public void setStatus(HttpResponseStatus status) {
		this.status = status;
	}
}
