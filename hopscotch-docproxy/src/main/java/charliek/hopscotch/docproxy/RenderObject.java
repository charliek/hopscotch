package charliek.hopscotch.docproxy;

public class RenderObject {
	private String contentType;
	private byte[] bytes;

	public RenderObject(String contentType, byte[] bytes) {
		this.contentType = contentType;
		this.bytes = bytes;
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
}
